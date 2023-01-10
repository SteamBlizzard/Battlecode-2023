package WeBall;

import battlecode.common.*;

public class Robot {
    RobotController rc;
    MapLocation home = null;

    boolean RIGHT = false;

    int homeID;
    public Robot(RobotController robot) throws GameActionException {
        rc = robot;
        RIGHT = Math.random() > 0.5;
        if (rc.getType() == RobotType.HEADQUARTERS) {
            home = rc.getLocation();
            homeID = rc.getID();
            return;
        } else {
            // Setup Navigation
        }
        for(RobotInfo r:rc.senseNearbyRobots(3,  rc.getTeam())) {
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

    // tries moving in the given direction dir
    public boolean tryMove(Direction dir) throws GameActionException {
        if(rc.canMove(dir)){
            rc.move(dir);
            return true;
        }
        return false;
    }

    public void fuzzyMove(Direction dir) throws GameActionException{
        Direction[] fuzzyDirs;
        if(!RIGHT){
            fuzzyDirs = new Direction[] {
                    dir,
                    dir.rotateLeft(),
                    dir.rotateRight(),
                    dir.rotateLeft().rotateLeft(),
                    dir.rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft().rotateLeft(),
                    dir.rotateRight().rotateRight().rotateRight()
            };
        }else{
            fuzzyDirs = new Direction[] {
                    dir,
                    dir.rotateRight(),
                    dir.rotateLeft(),
                    dir.rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft(),
                    dir.rotateRight().rotateRight().rotateRight(),
                    dir.rotateLeft().rotateLeft().rotateLeft(),
            };
        }

        for(Direction d : fuzzyDirs){
            if(tryMove(d)){
                return;
            }
        }
    }

    public void fuzzyMove(MapLocation loc) throws GameActionException{
        fuzzyMove(dirTo(loc));
    }

    public WellInfo closestWell(WellInfo[] wells, MapLocation from) throws GameActionException{
        int closest = Integer.MAX_VALUE;
        WellInfo closestWell = null;
        for(WellInfo w : wells){
            int dist = from.distanceSquaredTo(w.getMapLocation());
            if(dist < closest){
                closestWell = w;
            }
        }
        return closestWell;
    }

    public Direction dirTo(MapLocation loc) throws GameActionException{
        return rc.getLocation().directionTo(loc);
    }
}