package SuperiorCowPowers;

import battlecode.common.*;

import java.util.ArrayList;

public class Robot {
    RobotController rc;
    MapLocation home = null;

    boolean RIGHT = false;

    int homeID;

    int width;
    int height;

    /*

        Communication Table:
        0 - 19: wells
        20 - 29: help us
        30 - 39: friendly HQ locations

     */

    final int WELL_MIN_INDEX = 0;
    final int WELL_MAX_INDEX = 19;
    final int HELP_US_MIN_INDEX = 20;
    final int HELP_US_MAX_INDEX = 29;

    int FRIENDLY_HQ_MIN_INDEX = 30;
    int FRIENDLY_HQ_MAX_INDEX = 33;
    int POSSIBLE_HQ_MIN_INDEX = 34;
    int POSSIBLE_HQ_MAX_INDEX = 45;

    public Robot(RobotController robot) throws GameActionException {
        rc = robot;
        width = rc.getMapWidth();
        height = rc.getMapHeight();
        RIGHT = Math.random() > 0.5;
        if (rc.getType() == RobotType.HEADQUARTERS) {
            home = rc.getLocation();
            homeID = rc.getID();
            return;
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

    public void writeLocationToArray(MapLocation loc, int MIN_INDEX, int MAX_INDEX) throws GameActionException{
        int locCode = locToInt(loc);
        int firstHoleIndex = 0;
        for(int i = MIN_INDEX; i <= MAX_INDEX; i++){
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

    public void clearLocationFromArray(MapLocation loc, int MIN_INDEX, int MAX_INDEX) throws GameActionException{
        int locCode = locToInt(loc);
        for(int i = MIN_INDEX; i <= MAX_INDEX; i++){
            if(rc.readSharedArray(i) == locCode && rc.canWriteSharedArray(i,0)){
                rc.writeSharedArray(i,0);
                return;
            }
        }
    }

    public MapLocation closestLocationFromArray(int MIN_INDEX, int MAX_INDEX) throws GameActionException{
        MapLocation closestLoc = null;
        int closestDist = Integer.MAX_VALUE;
        for(int i = MIN_INDEX; i <= MAX_INDEX; i++){
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

    public void addWell(MapLocation loc, ResourceType rt) throws GameActionException{
        int resourceNum = 0;
        if(rt.equals(ResourceType.MANA)){
            resourceNum = 1;
        }else if(rt.equals(ResourceType.ELIXIR)){
            resourceNum = 2;
        }

        int locCode = resourceNum*10000 + locToInt(loc);
        int firstHoleIndex = 0;
        for(int i = WELL_MIN_INDEX; i <= WELL_MAX_INDEX; i++){
            int code = rc.readSharedArray(i);
            if(code == locCode){
                return;
            }
            if(code == 0){
                firstHoleIndex = i;
                break;
            }
        }
        if(rc.canWriteSharedArray(firstHoleIndex,locCode)){
            rc.writeSharedArray(firstHoleIndex,locCode);
        }
    }

    public void navAdjacent(MapLocation loc) throws GameActionException {
        if (!rc.getLocation().isAdjacentTo(loc)) {
            bugNav(loc);
        }
    }

    public MapLocation findClosestWell(ResourceType rt) throws GameActionException{
        MapLocation closestLoc = null;
        int closestDist = Integer.MAX_VALUE;
        for(int i = WELL_MIN_INDEX; i <= WELL_MAX_INDEX; i++){
            int code = rc.readSharedArray(i);
            if(code==0) {
                break;
            }
            ResourceType resourceType = RobotPlayer.resourceTypes[code/10000];

            if(resourceType.equals(rt)){
                MapLocation loc = intToLoc(code%10000);
                if(distTo(loc)<closestDist){
                    closestDist = distTo(loc);
                    closestLoc = loc;
                }
            }

        }

        return closestLoc;
    }

    public MapLocation findClosestWell() throws GameActionException{
        MapLocation closestLoc = null;
        int closestDist = Integer.MAX_VALUE;
        for(int i = WELL_MIN_INDEX; i <= WELL_MAX_INDEX; i++){
            int code = rc.readSharedArray(i);
            if(code==0) {
                break;
            }
            MapLocation loc = intToLoc(code%10000);
            if(distTo(loc)<closestDist){
                closestDist = distTo(loc);
                closestLoc = loc;
            }
        }

        return closestLoc;
    }

    public void cryForHelp(MapLocation loc) throws GameActionException{
        writeLocationToArray(loc,HELP_US_MIN_INDEX,HELP_US_MAX_INDEX);
    }

    public MapLocation closestCryForHelp() throws GameActionException{
        return closestLocationFromArray(HELP_US_MIN_INDEX,HELP_US_MAX_INDEX);
    }

    public void clearCry(MapLocation loc) throws GameActionException{
        clearLocationFromArray(loc,HELP_US_MIN_INDEX,HELP_US_MAX_INDEX);
    }

    public void writeHQlocation(MapLocation loc) throws GameActionException{
        writeLocationToArray(loc,POSSIBLE_HQ_MIN_INDEX,POSSIBLE_HQ_MAX_INDEX);
    }

    public ArrayList<MapLocation> allPossibleHQs() throws GameActionException{
        ArrayList<MapLocation> result = new ArrayList<>();
        for(int i = POSSIBLE_HQ_MIN_INDEX; i <= POSSIBLE_HQ_MAX_INDEX; i++){
            int code = rc.readSharedArray(i);
            if(code!=0){
                MapLocation loc = intToLoc(code);
                result.add(loc);
            }
        }
        return result;
    }

    public void removeHQlocation(MapLocation loc) throws GameActionException{
        clearLocationFromArray(loc,POSSIBLE_HQ_MIN_INDEX,POSSIBLE_HQ_MAX_INDEX);
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
        rc.setIndicatorString(Integer.toString(bugNavTurns));
        rc.setIndicatorDot(loc,0,255,255);
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

    public MapLocation randomLocation(ArrayList<MapLocation> locs){
        return locs.get((int)(Math.random()*locs.size()));
    }





}