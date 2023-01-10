package Kiting;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.*;

public class Headquarters extends Robot {

    int carrierCount = 0;
    int MAX_CARRIER_COUNT = 6;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void turn() throws Exception {
        if(carrierCount < MAX_CARRIER_COUNT){
            if(tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection())){
                carrierCount++;
            };
        } else if (rc.getAnchor() == null) {
            tryBuildAnchor();
        }else {
            if(Math.random()>0.5){
                if(tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection())){
                    carrierCount++;
                }
            }else{
                tryBuild(RobotType.LAUNCHER,RobotPlayer.randomDirection());
            }
        }
        if(rc.getResourceAmount(ResourceType.MANA)>100){
            tryBuild(RobotType.LAUNCHER,RobotPlayer.randomDirection());
        }
        if(rc.getResourceAmount(ResourceType.ADAMANTIUM)>100){
            tryBuild(RobotType.CARRIER,RobotPlayer.randomDirection());
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

    public boolean tryBuildAnchor() throws GameActionException {
        if (rc.canBuildAnchor(Anchor.STANDARD)) {
            rc.buildAnchor(Anchor.STANDARD);
            return true;
        }
        return false;
    }
}
