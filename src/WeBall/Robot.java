package WeBall;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class Robot {
    RobotController rc;
    MapLocation home = null;

    int homeID;
    public Robot(RobotController robot) throws GameActionException {
        rc = robot;
        if (rc.getType() == RobotType.HEADQUARTERS) {
            home = rc.getLocation();
            homeID = rc.getID();
            return;
        } else {
            // Setup Navigation
        }
        for(RobotInfo r:rc.senseNearbyRobots(rc.getType().visionRadiusSquared,  rc.getTeam())) {
            if(r.type==RobotType.HEADQUARTERS) {
                home = r.location;
                homeID = r.ID;
            }
        }
        if(home == null) {
            home = rc.getLocation();
            homeID = -1;
        }
    }
    public void turn() throws Exception {

    }
    public void run() {
        while(true) {
            try {
                turn();
            } catch(Exception e) {
                e.printStackTrace();
            }
            Clock.yield();
        }
    }
}