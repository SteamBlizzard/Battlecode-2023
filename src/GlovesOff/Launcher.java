package GlovesOff;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Launcher extends Robot {

    boolean defender = false;
    MapLocation[] exploreLocations;
    MapLocation exploreTarget;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 75;
    int leaderID = rc.getID();
    MapLocation enemyHQLoc = null;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        defender = Math.random()>0.9;
        exploreLocations = new MapLocation[] {
                new MapLocation(width - home.x,home.y),
                new MapLocation(home.x,height - home.y),
                new MapLocation(width - home.x,height - home.y)
        };
        exploreTarget = randomLocation(allPossibleHQs());
        leaderID = rc.getID();
    }



    public void turn() throws Exception {
        findSquad();

        if(!attackNearby()) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            boolean foundEnemy = false;
            for(RobotInfo r : visibleEnemies){
                if(r.type != RobotType.HEADQUARTERS){
                    fuzzyMove(visibleEnemies[0].location);
                    attackNearby();
                    foundEnemy = true;
                } else {
                    enemyHQLoc = r.getLocation();
                }
            }
            // if nobody to attack
            if(!foundEnemy){
                // Attacker logic
                squadGobble();
                attackNearby();
            }

        }
    }

    int packSize = 0;
    HashMap<Integer,Integer> mySquad = new HashMap<Integer,Integer>();
    MapLocation squadAverageLocation;
    RobotInfo inDanger = null;
    public void findSquad() throws GameActionException{
        inDanger = null;
        HashMap<Integer,Integer> newSquad = new HashMap<Integer,Integer>();
        int x = 0;
        int y = 0;
        packSize = 0;
        if (!rc.canSenseRobot(leaderID) || leaderID == rc.getID()) {
            leaderID = rc.getID();
            for (RobotInfo r : rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam())) {
                if(r.type.equals(RobotType.LAUNCHER)){
                    packSize++;
                    x += r.location.x;
                    y += r.location.y;
                    Integer lastHealth = mySquad.get(r.ID);
                    if(lastHealth != null && r.health < lastHealth){
                        inDanger = r;
                    }
                    newSquad.put(r.ID,r.health);
                }
                if (r.getType().equals(RobotType.LAUNCHER) && r.getID() < leaderID) {
                    leaderID = r.getID();
                }
            }
        }
        if(packSize>0){
            squadAverageLocation = new MapLocation(x/packSize, y/packSize);
        }
        mySquad = newSquad;
    }
    public void squadGobble() throws GameActionException {

        if (leaderID == rc.getID()) {
            if(inDanger!=null){
                fuzzyMove(inDanger.location);
            }else{
                bugNav(exploreTarget);
                exploreTurns++;
                if (rc.canSenseLocation(exploreTarget) || exploreTurns > MAX_EXPLORE_TURNS) {
                    exploreTarget = randomLocation(allPossibleHQs());
                    exploreTurns = 0;
                }
//            bugNav(closestLocationFromArray(POSSIBLE_HQ_MIN_INDEX,POSSIBLE_HQ_MAX_INDEX));
                rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
            }
        } else {
            RobotInfo leader = rc.senseRobot(leaderID);
            fuzzyMove(leader.getLocation());
            rc.setIndicatorLine(rc.getLocation(), leader.getLocation(), 165, 42, 42);
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
            if(target.type == RobotType.LAUNCHER){
                if(squadAverageLocation!=null){
                    fuzzyMove(squadAverageLocation);
                }else{
                    tryMove(dirTo(target.location).opposite());
                }

            }
            return true;
        }
        return false;
    }



}
