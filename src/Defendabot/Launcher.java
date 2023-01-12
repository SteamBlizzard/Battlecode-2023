package Defendabot;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Iterator;

public class Launcher extends Robot {

    boolean defender = false;
    ArrayList<MapLocation> possibleHQlocs;
    MapLocation exploreTarget;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 50;
    int leaderID = rc.getID();
    MapLocation packCenter = null;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        defender = Math.random()>0.9;
        possibleHQlocs = allPossibleHQs();
        exploreTarget = randomLocation(possibleHQlocs);
        leaderID = rc.getID();
    }



    public void turn() throws Exception {
        //update possible hq locations
        ArrayList<MapLocation> newPossibleHQs = new ArrayList<>();
        for(MapLocation loc : allPossibleHQs()){
            if(possibleHQlocs.contains(loc)){
                newPossibleHQs.add(loc);
            }
        }
        possibleHQlocs = newPossibleHQs;
        rc.setIndicatorString(possibleHQlocs.toString());

        // if you stumble upon a potential hq location and see nothing remove it from possible hq locations

        Iterator<MapLocation> itr = possibleHQlocs.iterator();

        while(itr.hasNext()){
            MapLocation loc = itr.next();
            if(rc.canSenseLocation(loc)){
                RobotInfo r = rc.senseRobotAtLocation(loc);
                if(r!=null && r.getType()!=RobotType.HEADQUARTERS){
                    itr.remove();
                    if(loc.equals(exploreTarget)){
                        exploreTarget = randomLocation(possibleHQlocs);
                        exploreTurns = 0;
                    }
                }
            }
        }


//        rc.setIndicatorString("Move CD: " + rc.getMovementCooldownTurns() + ", Action CD: " + rc.getActionCooldownTurns());
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        RobotInfo bestFriend = null;
        for (RobotInfo r : friends) {
            if (r.getTotalAnchors() > 0 && r.type.equals(RobotType.CARRIER)) {
                bestFriend = r;
                break;
            }
            if (r.type.equals(RobotType.CARRIER)) {
                bestFriend = r;
            }
        }

        if(!attackNearby()) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            boolean foundEnemy = false;

            int enemyLaunchers = 0;
            int lowestHealth = Integer.MAX_VALUE;
            RobotInfo target = null;
            boolean targetIsLauncher = false;
            for(RobotInfo r : visibleEnemies){
                if(r.type != RobotType.HEADQUARTERS){
                    if(r.type != RobotType.LAUNCHER){
                        if(!targetIsLauncher && r.health<lowestHealth){
                            target = r;
                            lowestHealth = r.health;
                        }
                    }else{
                        enemyLaunchers++;
                        if(!targetIsLauncher || r.health<lowestHealth){
                            target = r;
                            targetIsLauncher = true;
                            lowestHealth = r.health;
                        }
                    }
                }
            }
            if(target!=null){
                if(enemyLaunchers<=packSize){
//                    fuzzyMove(target.location);
                    attackNearby();
                }else{
                    fuzzyMove(dirTo(target.location).opposite());
                }
                // if nobody to attack
            }else {
                MapLocation closestEnemy = closestLocationFromArray(ENEMY_LOCATIONS_MIN,ENEMY_LOCATION_MAX);
                if(closestEnemy!=null){
                    fuzzyMove(closestEnemy);
                }else{
                    squadGobble(friends);
                }
                attackNearby();
            }
        }
    }

    int packSize = 0;
    public void squadGobble(RobotInfo[] friends) throws GameActionException {
        leaderID = rc.getID();
        packSize = 0;
        int x = 0;
        int y = 0;
        boolean followingAmp = false;
        for (RobotInfo r : friends) {
            if(r.type.equals(RobotType.LAUNCHER)){
                packSize++;
                x+=r.location.x;
                y+=r.location.y;
            }
            if ((r.getType().equals(RobotType.LAUNCHER) && r.getID() < leaderID) || r.getType().equals(RobotType.AMPLIFIER)) {
                leaderID = r.getID();
                if(r.getType().equals(RobotType.AMPLIFIER)){
                    followingAmp = true;
                    break;
                }
            }
        }
        if(packSize>0){
            packCenter = new MapLocation(x/packSize,y/packSize);
        }
        if (leaderID == rc.getID()) {
            if((packSize>0 && adjacentTurkeys()>Math.max(0,packSize-3)) || packSize == 0){
                MapLocation closestHelp = closestCryForHelp();
                if(closestHelp!=null){
                    fuzzyMove(closestHelp);

                    // otherwise group together and attack
                }else{
                    MapLocation closestAmp = closestLocationFromArray(AMPLIFIER_MIN_INDEX,AMPLIFIER_MAX_INDEX);
                    if(closestAmp!=null){
                        fuzzyMove(closestAmp);
                        rc.setIndicatorLine(rc.getLocation(), closestAmp, 165, 42, 42);
                    }else{
                        fuzzyMove(exploreTarget);
                        exploreTurns++;
                        if(exploreTurns > MAX_EXPLORE_TURNS){
                            exploreTarget = randomLocation(possibleHQlocs);
                            exploreTurns = 0;
                        }
                    }
                }
            }else{
                fuzzyMove(packCenter);
            }
            rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
        } else {
            MapLocation closestAmp = closestLocationFromArray(AMPLIFIER_MIN_INDEX,AMPLIFIER_MAX_INDEX);
            if(followingAmp && closestAmp!=null){
                fuzzyMove(closestAmp);
                rc.setIndicatorLine(rc.getLocation(), closestAmp, 165, 42, 42);
            }else{
                RobotInfo leader = rc.senseRobot(leaderID);
                fuzzyMove(leader.getLocation());
                rc.setIndicatorLine(rc.getLocation(), leader.getLocation(), 165, 42, 42);
            }

        }
    }



    public boolean attackNearby() throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        RobotInfo target = null;
        boolean targetIsLauncher = false;
        int minHealth = Integer.MAX_VALUE;
        for(RobotInfo r : enemies){
            if((!targetIsLauncher && r.type != RobotType.HEADQUARTERS && r.health < minHealth) ||
                    (!targetIsLauncher && r.type == RobotType.LAUNCHER) ||
                    (targetIsLauncher && r.type == RobotType.LAUNCHER && r.health < minHealth)){
                if(r.type == RobotType.LAUNCHER){
                    targetIsLauncher = true;
                }
                minHealth = r.health;
                target = r;
            }

        }
        if(target != null && rc.canAttack(target.location)){
            rc.attack(target.location);
            if(target.type == RobotType.LAUNCHER && target.health>RobotType.LAUNCHER.damage){
                if(packCenter != null){
                    fuzzyMove(packCenter);
                }else{
                    fuzzyMove(dirTo(target.location).opposite());
                }

            }
            return true;
        }
        return false;
    }



}
