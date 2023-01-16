package unstable;

import battlecode.common.*;

public class Headquarters extends Robot {

    int carrierCount = 0;
    int MAX_CARRIER_COUNT = 10;

    WellInfo[] nearbyWells = new WellInfo[0];
    int lastSpawn = 0;

    MapLocation closestEnemyHQ;

    public Headquarters(RobotController rc) throws GameActionException {
        super(rc);
        nearbyWells = rc.senseNearbyWells();
        for(WellInfo well : nearbyWells){
            addWell(well.getMapLocation(),well.getResourceType());
        }
        // write friendly location
        writeLocationToArray(rc.getLocation(),FRIENDLY_HQ_MIN_INDEX,FRIENDLY_HQ_MAX_INDEX);
        // write possible enemy locations
        writeHQlocation(new MapLocation(width - 1 - home.x,home.y));
        writeHQlocation(new MapLocation(home.x,height - 1 - home.y));
        writeHQlocation(new MapLocation(width - 1 - home.x,height - 1 - home.y));

        closestEnemyHQ = new MapLocation(width - 1 - home.x,height - 1 - home.y);
    }

    public void turn() throws Exception {
        if(rc.getRoundNum()==2){
            // eliminate symmetry based off of hq locations
            for(int i = 0; i < 12; i++){
                int val = rc.readSharedArray(POSSIBLE_HQ_MIN_INDEX+i);
                if (val != 0) {
                    MapLocation enemyHQ = intToLoc(val);
                    rc.setIndicatorDot(enemyHQ,0,0,255);
                    if(rc.canSenseLocation(enemyHQ)){
                        RobotInfo enemy = rc.senseRobotAtLocation(enemyHQ);
                        if(enemy==null || enemy.type != RobotType.HEADQUARTERS || enemy.getTeam().equals(rc.getTeam())){
                            // reduce symmetry possibility
                            for(int j = 0; j<GameConstants.MAX_STARTING_HEADQUARTERS;j++){
                                if(rc.canWriteSharedArray(POSSIBLE_HQ_MIN_INDEX+ j*3+(i%3),0)){
                                    rc.writeSharedArray(POSSIBLE_HQ_MIN_INDEX+j*3+(i%3),0);
                                }
                            }
                        }
                    }
                }
            }
            // eliminate symmetry based off of well positions
            for(int i = WELL_MIN_INDEX; i <WELL_MAX_INDEX; i++){
                for(int j = 0; j < 3; j++){
                    int val = rc.readSharedArray(i);
                    if(val==0){
                        break;
                    }
                    MapLocation symLocation = intToLoc(val%10000);
                    ResourceType rt = RobotPlayer.resourceTypes[val/10000];
                    if(j==0){
                        symLocation = new MapLocation(width - 1 - symLocation.x,symLocation.y);
                    }else if (j==1){
                        symLocation = new MapLocation(symLocation.x,height - 1 - symLocation.y);
                    }else{
                        symLocation = new MapLocation(width - 1 - symLocation.x,height - 1 - symLocation.y);
                    }
                    if(rc.canSenseLocation(symLocation)){
                        WellInfo well = rc.senseWell(symLocation);
                        if(well == null || !well.getResourceType().equals(rt)){
                            for(int k = 0; k<GameConstants.MAX_STARTING_HEADQUARTERS;k++){
                                if(rc.canWriteSharedArray(POSSIBLE_HQ_MIN_INDEX+ k*3+(j%3),0)){
                                    rc.writeSharedArray(POSSIBLE_HQ_MIN_INDEX+k*3+(j%3),0);
                                }
                            }
                        }
                    }
                }
            }


        }
        if(rc.getRoundNum()>2){
            closestEnemyHQ = closestLocationFromArray(POSSIBLE_HQ_MIN_INDEX,POSSIBLE_HQ_MAX_INDEX);
        }



        rc.setIndicatorDot(closestEnemyHQ,255,255,0);

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam().opponent());
        int launchers = 0;
        for (RobotInfo e : enemies){
            if(e.type == RobotType.LAUNCHER){
                launchers++;
            }
        }
        if(launchers > 0){
//            cryForHelp(home);
            buildFurthestFrom(enemies[0].location, RobotType.LAUNCHER);
            return;
        }

        if(carrierCount < MAX_CARRIER_COUNT){
            buildClosestTo(closestEnemyHQ, RobotType.LAUNCHER);
            buildCarrier();
        } else if (rc.getRoundNum()>200 && rc.getNumAnchors(Anchor.STANDARD) == 0) {
            rc.setIndicatorString("trying to build a thing?");
            tryBuildAnchor();
        }else {
            if(Math.random()>0.5){
                buildCarrier();
                buildClosestTo(closestEnemyHQ, RobotType.LAUNCHER);
            }else{
                buildClosestTo(closestEnemyHQ, RobotType.LAUNCHER);
                buildCarrier();
            }
        }
        if(rc.getResourceAmount(ResourceType.MANA)>100){
            buildClosestTo(closestEnemyHQ, RobotType.LAUNCHER);
        }
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam());
        int carriers = 0;
        for(RobotInfo f : friends){
            if(f.type == RobotType.CARRIER){
                carriers++;
            }
        }

        if(rc.getResourceAmount(ResourceType.ADAMANTIUM)>100 && carriers<15){
            buildCarrier();
        }
    }

    // builds carrier as close as it can to a well
    public void buildCarrier() throws GameActionException{
        if(nearbyWells.length>0){
            WellInfo chosenWell = nearbyWells[lastSpawn];
            WellInfo closestMana = null;
            int closest = Integer.MAX_VALUE;
            for(WellInfo well : nearbyWells){
                int dist = distTo(well.getMapLocation());
                if(well.getResourceType() == ResourceType.MANA && dist < closest){
                    closestMana = well;
                    closest = dist;
                }
            }
//            rc.setIndicatorString(chosenWell.toString());
            if(closestMana!=null){
                if(buildClosestTo(closestMana.getMapLocation(),RobotType.CARRIER)){
                    carrierCount++;
                }
            }else{
                if(buildClosestTo(chosenWell.getMapLocation(),RobotType.CARRIER)){
                    carrierCount++;
                    lastSpawn = (lastSpawn+1)%(nearbyWells.length);
                }
            }

        }else{
            if(tryBuild(RobotType.CARRIER, RobotPlayer.randomDirection())){
                carrierCount++;
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
            if(loc.distanceSquaredTo(to)<closest && rc.canBuildRobot(rt,loc) && rc.senseMapInfo(loc).getCurrentDirection()==Direction.CENTER){
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
    public boolean buildFurthestFrom(MapLocation from, RobotType rt) throws GameActionException {
        MapLocation myLoc = rc.getLocation();
        return buildClosestTo(new MapLocation(myLoc.x - (from.x - myLoc.x), myLoc.y - (from.y - myLoc.y)), rt);
    }
}
