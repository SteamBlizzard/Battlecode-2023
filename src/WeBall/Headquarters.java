package WeBall;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.*;

public class Headquarters extends Robot {
    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void turn() throws Exception {
        tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection());
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
