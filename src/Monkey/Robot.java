package Monkey;

import battlecode.common.*;

public class Robot {
    RobotController rc;
    MapLocation home = null;

    boolean RIGHT = false;

    int homeID;

    int width;
    int height;

    /*

        Communication Table:
        0 - 9: wells
        10-19: islands
        20-29: help us


     */

    int HELP_US_MIN_INDEX = 20;
    int HELP_US_MAX_INDEX = 29;


    public Robot(RobotController robot) throws GameActionException {
        rc = robot;
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        RIGHT = Math.random() > 0.5;
        if (rc.getType() == RobotType.HEADQUARTERS) {
            home = rc.getLocation();
            homeID = rc.getID();
            return;
        } else {
            // Setup Navigation
        }
        if(rc.getType() == RobotType.CARRIER){
            RIGHT = true;
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

    public void cryForHelp(MapLocation loc) throws GameActionException{
        int locCode = locToInt(loc);
        int firstHoleIndex = 0;
        for(int i = HELP_US_MIN_INDEX; i <= HELP_US_MAX_INDEX; i++){
            int code = rc.readSharedArray(i);
            if(code == locCode){
                return;
            }
            if(firstHoleIndex == 0 && code == 0){
                firstHoleIndex = i;
            }
        }
        if(rc.canWriteSharedArray(firstHoleIndex,locCode)){
            rc.writeSharedArray(firstHoleIndex,locCode);
        }
    }

    public MapLocation closestCryForHelp() throws GameActionException{
        MapLocation closestLoc = null;
        int closestDist = Integer.MAX_VALUE;
        for(int i = HELP_US_MIN_INDEX; i <= HELP_US_MAX_INDEX; i++){
            int code = rc.readSharedArray(i);
            if(code!=0){
                MapLocation loc = intToLoc(code);
                if(distTo(loc)<closestDist){
                    closestDist = distTo(loc);
                    closestLoc = loc;
                }
            }
        }

        return closestLoc;
    }

    public void clearCry(MapLocation loc) throws GameActionException{
        int locCode = locToInt(loc);
        for(int i = HELP_US_MIN_INDEX; i <= HELP_US_MAX_INDEX; i++){
            if(rc.readSharedArray(i) == locCode && rc.canWriteSharedArray(i,0)){
                rc.writeSharedArray(i,0);
                return;
            }
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

    public boolean tryAttack(MapLocation loc) throws GameActionException {
        if(rc.canAttack(loc)){
            rc.attack(loc);
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

    MapLocation lastLocation = null;
    int minDist = Integer.MAX_VALUE;
    int bugNavTurns = 0;
    int MAX_BUG_NAV_TURNS = 60;
    public void bugNav(MapLocation loc) throws GameActionException{
        Direction dir = dirTo(loc);
        Direction[] fuzzyDirs = new Direction[] {
                dir,
                dir.rotateRight(),
                dir.rotateLeft(),
                dir.rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft(),
                dir.rotateRight().rotateRight().rotateRight(),
                dir.rotateLeft().rotateLeft().rotateLeft(),
        };
        if(loc != lastLocation || bugNavTurns >= MAX_BUG_NAV_TURNS){
            minDist = Integer.MAX_VALUE;
            lastLocation = loc;
            bugNavTurns = 0;
        }
        bugNavTurns++;
        for(Direction d : fuzzyDirs){
            if(loc.distanceSquaredTo(rc.getLocation().add(d))<minDist && rc.canMove(d)){
                rc.move(d);
                minDist = distTo(loc);
                return;
            }
        }
        // Wall following time
        Direction d = dirTo(loc);
        boolean foundWall = false;
        for (int i = 0; i < 9; i++){
            // follow wall
            if(foundWall && rc.canMove(d)){
                tryMove(d);
                return;
            }
            // find wall
            if(rc.onTheMap(rc.getLocation().add(d)) && !rc.sensePassability(rc.getLocation().add(d))){
                foundWall = true;
            }
            // rotate direction
            if(RIGHT){
                d = d.rotateRight();
            }else{
                d = d.rotateLeft();
            }
        }
        if(!foundWall){
            fuzzyMove(dirTo(loc));
        }
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

    public int distTo(MapLocation loc) throws GameActionException{
        return rc.getLocation().distanceSquaredTo(loc);
    }

    public boolean touching(MapLocation loc) throws GameActionException{
        return rc.getLocation().isAdjacentTo(loc) || rc.getLocation().equals(loc);
    }
    
    public int locToInt(MapLocation loc) throws GameActionException{
        return loc.x + loc.y*rc.getMapWidth();
    }

    public MapLocation intToLoc(int i) throws GameActionException{
        return new MapLocation((i%rc.getMapWidth()), (i/rc.getMapWidth()));
    }



    public MapLocation randomLocation(MapLocation[] locs){
        return locs[(int)(Math.random()*locs.length)];
    }





}