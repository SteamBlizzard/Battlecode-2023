package ballsxl;

import battlecode.common.*;

public class Carrier extends Robot {

    WellInfo mainWell = null;
    MapLocation targetIsland;
    boolean enemyIsland;
    boolean mining = true;
    boolean adamantiumMining = true;
    boolean anchorer = false;

    MapLocation[] exploreLocations;
    MapLocation exploreTarget;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 50;
    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
        RobotInfo hq = rc.senseRobotAtLocation(home);
        if(hq.getNumAnchors(Anchor.STANDARD) > 0){
            anchorer = true;
        }else{
            WellInfo[] nearbyWells = rc.senseNearbyWells(rc.getType().visionRadiusSquared);

            if (mainWell == null) {
                mainWell = closestWell(nearbyWells, rc.getLocation());
                if(mainWell!=null){
                    adamantiumMining = mainWell.getResourceType().equals(ResourceType.ADAMANTIUM);
                }
            }
        }


        exploreLocations = new MapLocation[] {
                new MapLocation(2,2),
                new MapLocation(width-2,2),
                new MapLocation(2,height - 2),
                new MapLocation(width - 2,height - 2),
                new MapLocation(width/2,height/2)
        };
        exploreTarget = randomLocation(exploreLocations);
    }

    RobotInfo danger;
    int dangerTurns = 0;

    public void turn() throws Exception{
        dangerTurns--;
        // how many friendly launchers
        int nearbyLaunchers = 0;
        for(RobotInfo f : rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam())){
            if(f.type == RobotType.LAUNCHER){
                nearbyLaunchers++;
            }
        }
        // run away from enemies
        RobotInfo[] enemies = rc.senseNearbyRobots(RobotType.CARRIER.visionRadiusSquared,rc.getTeam().opponent());
        for(RobotInfo r : enemies){
            if(r.type == RobotType.LAUNCHER){
                danger = r;
                dangerTurns = 4;
                if(getWeight()>=10){
                    if(!tryAttack(r.location)){
                        if(rc.getHealth()<=5 || nearbyLaunchers == 0){
                            fuzzyMove(r.location);
                            tryAttack(r.location);
                            adamantiumMining = !adamantiumMining;
                        }
                    }
                    if(getWeight()>15){
                        mining = false;
                    }
                } else{
                    fuzzyMove(dirTo(r.location).opposite());
                    fuzzyMove(dirTo(r.location).opposite());
                    if(getWeight()>15){
                        mining = false;
                    }else{
                        adamantiumMining = !adamantiumMining;
                    }
                    break;
                }
            }
        }
        if(dangerTurns>0){
//            fuzzyMove(dirTo(danger.location).opposite());
        }else{
            if(anchorer){
                // special chosen anchorer functionality
                deliverAnchor();
            }else{
                // normal carrier functionality
                normalCarrierTurn();
            }
        }

    }

    public void normalCarrierTurn() throws GameActionException{
        if(mainWell != null && ((adamantiumMining && mainWell.getResourceType().equals(ResourceType.ADAMANTIUM)) || (!adamantiumMining && mainWell.getResourceType().equals(ResourceType.MANA)))){
            if(!mining){
                if(touching(home)){
                    addWell(mainWell.getMapLocation(), mainWell.getResourceType());
                    transferAllResources(home);
                    rc.setIndicatorString(home.toString());
                    if(getWeight()==0){
                        mining = true;
//                        adamantiumMining = !adamantiumMining;
                        adamantiumMining = Math.random()>0.7;
                    }
                }else {
                    rc.setIndicatorDot(home,255,0,0);
                    bugNav(home);
                }
            }else{
                rc.setIndicatorDot(mainWell.getMapLocation(),0,0,255);
                rc.setIndicatorString("mining");
                if(!touching(mainWell.getMapLocation())) {
                    bugNav(mainWell.getMapLocation());
                    if(!touching(mainWell.getMapLocation())) {
                        bugNav(mainWell.getMapLocation());
                    }
                }
                searchNearbyWells();
                // mining away
                tryGetResource(mainWell);
                Direction bestDir = null;
                int closest = distTo(home);
                for(Direction dir : Direction.allDirections()){
                    MapLocation newLoc = rc.getLocation().add(dir);
                    int dist = newLoc.distanceSquaredTo(home);
                    if((newLoc.isAdjacentTo(mainWell.getMapLocation()) || newLoc.equals(mainWell.getMapLocation())) && rc.canMove(dir) && dist<closest){
                        bestDir = dir;
                        closest = dist;
                    }
                }
                if(bestDir != null){
                    rc.move(bestDir);
                }
                if(getWeight() == GameConstants.CARRIER_CAPACITY){
                    mining = false;
                }
            }
        }else{
            rc.setIndicatorString("exploring");
            explore();
            explore();
        }
    }

    public void explore() throws GameActionException{
        MapLocation closestWellComms = null;
        if(adamantiumMining){
            closestWellComms = findClosestWell(ResourceType.ADAMANTIUM);
        }else{
            closestWellComms = findClosestWell(ResourceType.MANA);
        }
        if(closestWellComms != null && !anchorer){
            bugNav(closestWellComms);
            rc.setIndicatorDot(closestWellComms,0,255,0);
        }else{
            bugNav(exploreTarget);
            exploreTurns++;
            if(rc.canSenseLocation(exploreTarget) || exploreTurns>MAX_EXPLORE_TURNS){
                exploreTarget = randomLocation(exploreLocations);
                exploreTurns = 0;
            }
        }


        searchNearbyWells();


        int[] islandIndexes = rc.senseNearbyIslands();
        for(int i : islandIndexes){
            Team occupying = rc.senseTeamOccupyingIsland(i);
            if(occupying == Team.NEUTRAL || occupying == rc.getTeam().opponent()){
                enemyIsland = occupying == rc.getTeam().opponent();
                MapLocation[] islands = rc.senseNearbyIslandLocations(i);
                if(islands.length>0){
                    targetIsland = islands[0];
                }
            }
        }

    }

    public void searchNearbyWells() throws GameActionException{
        WellInfo[] nearbyWells = rc.senseNearbyWells(rc.getType().visionRadiusSquared);
        WellInfo newWell = null;
        int closestDistToWell = Integer.MAX_VALUE;
        for(WellInfo well : nearbyWells){
            int distToWell = distTo(well.getMapLocation());
            if (mainWell == null) {
                mainWell = well;
                adamantiumMining = mainWell.getResourceType().equals(ResourceType.ADAMANTIUM);
            }else if(((adamantiumMining && well.getResourceType().equals(ResourceType.ADAMANTIUM)) || (!adamantiumMining && well.getResourceType().equals(ResourceType.MANA))) && distToWell < closestDistToWell){
                closestDistToWell = distToWell;
                newWell = well;
            }

        }
        if(newWell!=null) {
            mainWell = newWell;
        }
    }

    public void deliverAnchor() throws GameActionException {
        if (rc.getAnchor() == null) {
            if (rc.getLocation().isAdjacentTo(home)) {
                if (rc.canTakeAnchor(home, Anchor.STANDARD)) {
                    rc.takeAnchor(home, Anchor.STANDARD);
                } else if (getWeight() > 0) {
                    transferAllResources(home);
                }
            } else {
                bugNav(home);
            }
        } else {
            if(targetIsland==null){
                explore();
                rc.setIndicatorString(exploreTarget.toString());
            }else{
                if(rc.canSenseLocation(targetIsland) &&
                        rc.senseTeamOccupyingIsland(rc.senseIsland(targetIsland)) == rc.getTeam()){
                    targetIsland = null;
                    return;
                }

                int rcLocIdx = rc.senseIsland(rc.getLocation());
                if (rcLocIdx != -1 && rc.senseAnchor(rcLocIdx) == null) {
                    if (tryPlaceAnchor()) {
                        targetIsland = null;
                    }
                } else {
                    bugNav(targetIsland);
                }
            }

        }
    }

    public int getWeight() throws GameActionException{
        return rc.getResourceAmount(ResourceType.ADAMANTIUM) +
                rc.getResourceAmount(ResourceType.MANA) +
                rc.getResourceAmount(ResourceType.ELIXIR);
    }

    public boolean tryGetResource(WellInfo well) throws GameActionException{
        int amount = 0;
        if(well.isUpgraded()){
            amount = GameConstants.WELL_ACCELERATED_RATE;
        }else{
            amount = GameConstants.WELL_STANDARD_RATE;
        }
        int weight = getWeight();
        if(GameConstants.CARRIER_CAPACITY - weight < amount){
            amount = GameConstants.CARRIER_CAPACITY - weight;
        }
        if(rc.canCollectResource(well.getMapLocation(),amount)){
            rc.collectResource(well.getMapLocation(),amount);
            return true;
        }

        return false;
    }

    public boolean tryTransferResources(MapLocation loc, ResourceType rt, int amount) throws GameActionException{
        if(amount==0){
            return true;
        }
        if(rc.canTransferResource(loc,rt,amount)){
            rc.transferResource(loc,rt,amount);
            return true;
        }
        return false;
    }

    public void transferAllResources(MapLocation loc) throws GameActionException{
        for(ResourceType rt : RobotPlayer.resourceTypes){
            tryTransferResources(loc,rt,rc.getResourceAmount(rt));
        }
    }

    public boolean tryPlaceAnchor() throws GameActionException {
        if (rc.canPlaceAnchor()) {
            rc.placeAnchor();
            return true;
        }
        return false;
    }


}
