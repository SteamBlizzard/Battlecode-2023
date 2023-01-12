package Defendabot;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Iterator;

public class Amplifier extends Robot {

    MapLocation lastLocation;
    ArrayList<MapLocation> possibleHQlocations;
    MapLocation exploreTarget;

    MapLocation averageEnemy;

    public Amplifier(RobotController rc) throws GameActionException {
        super(rc);
        lastLocation = rc.getLocation();
        possibleHQlocations = allPossibleHQs();
        exploreTarget = randomLocation(possibleHQlocations);
    }
    public void turn() throws Exception {
        // broadcast location
        clearLocationFromArray(lastLocation, AMPLIFIER_MIN_INDEX,AMPLIFIER_MAX_INDEX);
        writeLocationToArray(rc.getLocation(),AMPLIFIER_MIN_INDEX,AMPLIFIER_MAX_INDEX);
        lastLocation = rc.getLocation();

        if(rc.getRoundNum()%2==0){
            incrementSharedArray(AMPLIFIER_COUNT_INDEX);
        }

        possibleHQlocations = allPossibleHQs();
        if(!possibleHQlocations.contains(exploreTarget)){
            exploreTarget = randomLocation(possibleHQlocations);
        }

        rc.setIndicatorDot(exploreTarget,255,0,0);
        rc.setIndicatorString(possibleHQlocations.toString());

        Iterator<MapLocation> itr = possibleHQlocations.iterator();
        while(itr.hasNext()){
            MapLocation loc = itr.next();
            if(rc.canSenseLocation(loc)){
                RobotInfo r = rc.senseRobotAtLocation(loc);
                if(r!=null && r.getType()!=RobotType.HEADQUARTERS){
                    itr.remove();
                    removeHQlocation(loc);
                }
                if(loc.equals(exploreTarget)){
                    exploreTarget = randomLocation(possibleHQlocations);
                }
            }
        }


        // sense enemies and such
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam().opponent());
        int launcherCount = 0;
        boolean tooClose = false;
        int x = 0;
        int y = 0;
        for(RobotInfo e : enemies){
            if(e.type.equals(RobotType.LAUNCHER)){
                launcherCount++;
                x+=e.location.x;
                y+=e.location.y;
                if(distTo(e.location)<=RobotType.LAUNCHER.actionRadiusSquared){
                    fuzzyMove(dirTo(e.location).opposite());
                    tooClose = true;
                }
            }
        }
        if(launcherCount>0){
            if(launcherCount>packSize && averageEnemy!=null){
                fuzzyMove(dirTo(averageEnemy).opposite());
            }else{
                if(averageEnemy!=null){
                    clearLocationFromArray(averageEnemy,ENEMY_LOCATIONS_MIN,ENEMY_LOCATION_MAX);
                }
                averageEnemy = new MapLocation(x/launcherCount,y/launcherCount);
                writeLocationToArray(averageEnemy,ENEMY_LOCATIONS_MIN,ENEMY_LOCATION_MAX);
                if(!tooClose && rc.getLocation().add(dirTo(averageEnemy)).distanceSquaredTo(averageEnemy)>RobotType.LAUNCHER.visionRadiusSquared){
                    fuzzyMove(averageEnemy);
                }
            }

        }else{
            moveWithPack();
        }



    }

    int packSize;
    MapLocation packCenter;
    public void moveWithPack() throws GameActionException{
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam());
        packSize = 0;
        int x = 0;
        int y = 0;
        for (RobotInfo r : friends) {
            if(r.type.equals(RobotType.LAUNCHER)){
                packSize++;
                x+=r.location.x;
                y+=r.location.y;
            }
        }
        if(packSize>0){
            packCenter = new MapLocation(x/packSize,y/packSize);
        }
        MapLocation closestHelp = closestCryForHelp();
        if(closestHelp!=null){
            fuzzyMove(closestHelp);
            // otherwise group together and attack
        }else{
            fuzzyMove(exploreTarget);
        }

        rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
    }



}
