package Lezduit;

import battlecode.common.*;

public class Launcher extends Robot {

    MapLocation targetIsland;
    MapLocation[] exploreLocations;
    MapLocation exploreTarget;
    boolean holding = false;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 90;
    boolean surrounding = false;
    MapLocation surroundedLocation = null;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        exploreLocations = new MapLocation[] {
                new MapLocation(2,2),
                new MapLocation(width-2,2),
                new MapLocation(2,height - 2),
                new MapLocation(width - 2,height - 2),
                new MapLocation(width/2,height/2)
        };
        exploreTarget = randomLocation(exploreLocations);
    }

    public void holdIsland() throws GameActionException{
        int rcLocIdx = rc.senseIsland(rc.getLocation());
        if (holding && rcLocIdx != -1 && rc.senseTeamOccupyingIsland(rcLocIdx) == null) {
            holding = false;
        }
        if (!holding) {
            bugNav(exploreTarget);
            exploreTurns++;
            if (rc.canSenseLocation(exploreTarget) || exploreTurns > MAX_EXPLORE_TURNS) {
                exploreTarget = randomLocation(exploreLocations);
                exploreTurns = 0;
            }

            int[] islandIndexes = rc.senseNearbyIslands();
            for (int i : islandIndexes) {
                if (rc.senseAnchor(i) != null) {
                    MapLocation[] islands = rc.senseNearbyIslandLocations(islandIndexes[0]);
                    if (islands.length > 0) {
                        targetIsland = islands[0];
                    }
                }
            }
        }

    }

    public void turn() throws Exception {
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam());
        RobotInfo bestFriend = null;
        for(RobotInfo r : friends){
            if(r.getTotalAnchors()>0 && r.type.equals(RobotType.CARRIER)) {
                bestFriend = r;

                break;
            }
        }

        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        RobotInfo target = null;
        int minHealth = Integer.MAX_VALUE;
        for(RobotInfo r : enemies){
            if(r.type != RobotType.HEADQUARTERS && r.health < minHealth){
                minHealth = r.health;
                target = r;
            }
        }

        if(target != null && rc.canAttack(target.location)){
            rc.attack(target.location);
            if (!surrounding) {
                fuzzyMove(dirTo(target.location).opposite());
            }
        }else if (!surrounding){
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if(visibleEnemies.length>0){
                fuzzyMove(visibleEnemies[0].location);
            }else{
                if(bestFriend!=null){
                    if(!rc.getLocation().add(dirTo(bestFriend.getLocation())).isAdjacentTo(bestFriend.getLocation())){
                        fuzzyMove(bestFriend.getLocation());
                    }else{
                        fuzzyMove(dirTo(bestFriend.getLocation()).opposite());
                    }
                }else{
                    holdIsland();
                }
            }
            for (RobotInfo r : visibleEnemies) {
                if (rc.getLocation().isAdjacentTo(r.getLocation()) && r.getType() == RobotType.HEADQUARTERS) {
                    surrounding = true;
                    surroundedLocation = rc.getLocation();
                }
            }
        } else {
            if (rc.getLocation() != surroundedLocation) {
                surrounding = false;
            }
        }


    }


}
