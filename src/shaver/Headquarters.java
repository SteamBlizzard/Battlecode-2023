package shaver;

import battlecode.common.*;
import battlecode.world.Well;

public class Headquarters extends Robot {

    int carrierCount = 0;
    int MAX_CARRIER_COUNT = 6;

    WellInfo[] nearbyWells = new WellInfo[0];
    int lastSpawn = 0;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        nearbyWells = rc.senseNearbyWells();
    }

    public void turn() throws Exception {
        if(carrierCount < MAX_CARRIER_COUNT){

//            rc.setIndicatorString(Integer.toString(nearbyWells.length));
            if(nearbyWells.length>0){
                WellInfo chosenWell = nearbyWells[lastSpawn];
                rc.setIndicatorString(chosenWell.toString());
                if(buildClosestTo(chosenWell.getMapLocation(),RobotType.CARRIER)){
                    carrierCount++;
                    lastSpawn = (lastSpawn+1)%nearbyWells.length;
                }
            }else{
                if(tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection())){
                    carrierCount++;
                }
            }

        } else if (rc.getAnchor() == null) {
            tryBuildAnchor();
        }else {
            if(Math.random()>0.5){
                if(tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection())){
                    carrierCount++;
                }
            }else{
                tryBuild(RobotType.LAUNCHER, RobotPlayer.randomDirection());
            }
        }
        if(rc.getResourceAmount(ResourceType.MANA)>100){
            buildClosestTo(new MapLocation(width-home.x, height-home.y), RobotType.LAUNCHER);
        }
        if(rc.getResourceAmount(ResourceType.ADAMANTIUM)>100){
            if(nearbyWells.length>0){
                WellInfo chosenWell = nearbyWells[lastSpawn];
                rc.setIndicatorString(chosenWell.toString());
                if(buildClosestTo(chosenWell.getMapLocation(),RobotType.CARRIER)){
                    carrierCount++;
                    lastSpawn = (lastSpawn+1)%nearbyWells.length;
                }
            }else{
                if(tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection())){
                    carrierCount++;
                }
            }
        }
    }

    // tries to build a robot in the given direction
    public boolean tryBuild(RobotType type, Direction dir) throws GameActionException{
        MapLocation buildLoc = rc.getLocation().add(dir);
        if(rc.canBuildRobot(type,buildLoc)){
            rc.buildRobot(type,buildLoc);
            return true;
        }
        return false;
    }
    public boolean tryBuild(RobotType type, MapLocation loc) throws GameActionException{
        if(rc.canBuildRobot(type,loc)){
            rc.buildRobot(type,loc);
            return true;
        }
        return false;
    }

    public boolean tryBuildAnchor() throws GameActionException {
        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            rc.buildAnchor(Anchor.STANDARD);
            return true;
        }
        return false;
    }

    public boolean buildClosestTo(MapLocation to, RobotType rt) throws GameActionException{
        MapLocation closestLoc = null;
        int closest = Integer.MAX_VALUE;

        for(MapLocation loc : rc.getAllLocationsWithinRadiusSquared(rc.getLocation(),GameConstants.DISTANCE_SQUARED_FROM_HEADQUARTER)){
            if(loc.distanceSquaredTo(to)<closest && rc.canBuildRobot(rt,loc)){
                closest = loc.distanceSquaredTo(to);
                closestLoc = loc;
            }
        }
        if(closestLoc != null){
            rc.buildRobot(rt,closestLoc);
            return true;
        }
        return false;
    }
}
