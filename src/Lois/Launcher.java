package Lois;

import battlecode.common.*;

public class Launcher extends Robot {

    boolean defender = false;
    MapLocation[] exploreLocations;
    MapLocation exploreTarget;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 75;
    int leaderID = rc.getID();
    MapLocation enemyHQLoc = null;

    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
        defender = Math.random()>0.9;
        exploreLocations = new MapLocation[] {
                new MapLocation(width - home.x,home.y),
                new MapLocation(home.x,height - home.y),
                new MapLocation(width - home.x,height - home.y)
        };
        exploreTarget = randomLocation(exploreLocations);
        leaderID = rc.getID();
    }



    public void turn() throws Exception {
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam());
        RobotInfo bestFriend = null;
        if (enemyHQLoc != null) {
            navAdjacent(enemyHQLoc);
        } else {
            for (RobotInfo r : friends) {
                if (r.getTotalAnchors() > 0 && r.type.equals(RobotType.CARRIER)) {
                    bestFriend = r;
                    break;
                }
                if (r.type.equals(RobotType.CARRIER)) {
                    bestFriend = r;
                }
            }
        }
        if(!attackNearby()) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            boolean foundEnemy = false;
            for(RobotInfo r : visibleEnemies){
                if(r.type != RobotType.HEADQUARTERS){
                    fuzzyMove(visibleEnemies[0].location);
                    attackNearby();
                    foundEnemy = true;
                } else {
                    enemyHQLoc = r.getLocation();
                }
            }
            // if nobody to attack
            if(!foundEnemy){
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
                    // go to closest help if someone needs help
                    MapLocation closestHelp = closestCryForHelp();
                    if(closestHelp!=null){
                        bugNav(closestHelp);

                    // otherwise group together and attack
                    }else{
                        // Attacker logic
                        squadGobble(friends);
                        attackNearby();
                    }
                }
            }
        }
    }

    public void squadGobble(RobotInfo[] friends) throws GameActionException {
        if (!rc.canSenseRobot(leaderID) || leaderID == rc.getID()) {
            leaderID = rc.getID();
            for (RobotInfo r : friends) {
                if (r.getType().equals(RobotType.LAUNCHER) && r.getID() < leaderID) {
                    leaderID = r.getID();
                }
            }
        }
        if (leaderID == rc.getID()) {
            bugNav(exploreTarget);
            exploreTurns++;
            if (rc.canSenseLocation(exploreTarget) || exploreTurns > MAX_EXPLORE_TURNS) {
                exploreTarget = randomLocation(exploreLocations);
                exploreTurns = 0;
            }
            rc.setIndicatorDot(rc.getLocation(), 0, 255, 0);
        } else {
            RobotInfo leader = rc.senseRobot(leaderID);
            fuzzyMove(leader.getLocation());
            rc.setIndicatorLine(rc.getLocation(), leader.getLocation(), 165, 42, 42);
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
