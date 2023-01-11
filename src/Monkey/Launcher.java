package Monkey;

import battlecode.common.*;

public class Launcher extends Robot {

    boolean defender = false;
    MapLocation[] exploreLocations;
    MapLocation exploreTarget;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 50;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        defender = Math.random()>0.9;
        exploreLocations = new MapLocation[] {
                new MapLocation(width - home.x,home.y),
                new MapLocation(home.x,height - home.y),
                new MapLocation(width - home.x,height - home.y)
        };
        exploreTarget = randomLocation(exploreLocations);
    }

    public void turn() throws Exception {
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        RobotInfo bestFriend = null;
        for (RobotInfo r : friends) {
            if (r.getTotalAnchors() > 0 && r.type.equals(RobotType.CARRIER)) {
                bestFriend = r;
                break;
            }
            if (r.type.equals(RobotType.CARRIER)) {
                bestFriend = r;
            }
        }

//        if(defender){
//            int[] islands = rc.senseNearbyIslands();
//            if(islands.length>0){
//                for(int i : islands){
//                    if(rc.senseTeamOccupyingIsland(i) == rc.getTeam().opponent()){
//                        MapLocation[] islandLocs = rc.senseNearbyIslandLocations(i);
//                        if(islandLocs.length >0){
//                            fuzzyMove(islandLocs[0]);
//                            break;
//                        }
//                    }
//                }
//            }
//        }


        if(!attackNearby()) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            if(visibleEnemies.length>0){
                fuzzyMove(visibleEnemies[0].location);
                attackNearby();
            }else{
                // follow best friend if you defend
                if(defender){
                    if(bestFriend!=null){
                        if(!rc.getLocation().add(dirTo(bestFriend.getLocation())).isAdjacentTo(bestFriend.getLocation())){
                            fuzzyMove(bestFriend.getLocation());
                        }else{
                            fuzzyMove(dirTo(bestFriend.getLocation()).opposite());
                        }
                    }else{
                        fuzzyMove(RobotPlayer.randomDirection());
                    }
                }else{
                    MapLocation closestHelp = closestCryForHelp();
                    if(closestHelp!=null){
                        bugNav(closestHelp);
                    }else{
                        // Attacker logic
                        bugNav(exploreTarget);
                        exploreTurns++;
                        if(rc.canSenseLocation(exploreTarget) || exploreTurns>MAX_EXPLORE_TURNS){
                            exploreTarget = randomLocation(exploreLocations);
                            exploreTurns = 0;
                        }
                    }
                }
            }
        }


    }

    public boolean attackNearby() throws GameActionException{
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getType().actionRadiusSquared, rc.getTeam().opponent());
        RobotInfo target = null;
        boolean targetIsLauncher = false;
        int minHealth = Integer.MAX_VALUE;
        for(RobotInfo r : enemies){
            if((!targetIsLauncher && r.type != RobotType.HEADQUARTERS && r.health < minHealth) ||
                    (!targetIsLauncher && r.type == RobotType.LAUNCHER) ||
                    (targetIsLauncher && r.type == RobotType.LAUNCHER && r.health < minHealth)){
                if(r.type == RobotType.LAUNCHER){
                    targetIsLauncher = true;
                    minHealth = r.health;
                }
                minHealth = r.health;
                target = r;
            }
        }
        if(target != null && rc.canAttack(target.location)){
            rc.attack(target.location);
            if(target.type == RobotType.LAUNCHER){
                tryMove(dirTo(target.location).opposite());
            }
            return true;
        }
        return false;
    }


}
