package newVnewME;

import battlecode.common.*;

import java.util.HashMap;

public class Destabilizer extends Robot {
    MapLocation[] exploreLocations;
    MapLocation exploreTarget;
    int exploreTurns = 0;
    int MAX_EXPLORE_TURNS = 75;
    int leaderID = rc.getID();
    MapLocation enemyHQLoc = null;

    public Destabilizer(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void turn() throws Exception {
        findSquad();

        if(!attackNearby()) {
            RobotInfo[] visibleEnemies = rc.senseNearbyRobots(rc.getType().visionRadiusSquared, rc.getTeam().opponent());
            boolean foundEnemy = false;
            for(RobotInfo r : visibleEnemies){
                if(r.type != RobotType.HEADQUARTERS){
                    fuzzyMove(r.location);
                    attackNearby();
                    tryAttack(r.location);
                    foundEnemy = true;
                } else {
                    enemyHQLoc = r.getLocation();
                }
            }
            // if nobody to attack
            if(!foundEnemy){

                // OPTIMIZE THIS PLEASE DANIEL
                // Daniel: no need, it is simply chilling
                for(int x = 4; x >= -4; x--){
                    for(int y = -4; y <= 4; y++){
                        if(x*x + y*y <= RobotType.LAUNCHER.actionRadiusSquared){
                            MapLocation myLoc = rc.getLocation();
                            MapLocation test = new MapLocation(myLoc.x+x,myLoc.y+y);
                            rc.setIndicatorDot(test,255,0,255);
                            if(tryAttack(test)){
                                break;
                            }
                        }

                    }
                }

                if(lastAttack!=null){
                    tryAttack(lastAttack);
                }
                // Attacker logic
                squadGobble();
                attackNearby();
            }

        }
    }

    int packSize = 0;
    int adjacentLaunchers = 0;
    HashMap<Integer,Integer> mySquad = new HashMap<Integer,Integer>();
    MapLocation squadAverageLocation;
    RobotInfo inDanger = null;
    int dangerCounter = 0;
    boolean packless = false;
    public void findSquad() throws GameActionException{
        if(dangerCounter<=0){
            inDanger = null;
        }
        dangerCounter--;
        HashMap<Integer,Integer> newSquad = new HashMap<Integer,Integer>();
        int x = 0;
        int y = 0;
        packSize = 0;
        adjacentLaunchers = 0;

        leaderID = rc.getID();
        RobotInfo[] nearbyFriends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam());
        for (int i = 0; i < Math.min(nearbyFriends.length,8); i++) {
            RobotInfo r = nearbyFriends[i];
            boolean isAttacker = r.type.equals(RobotType.LAUNCHER) || r.type.equals(RobotType.DESTABILIZER);
            if(isAttacker){
                packSize++;
                x += r.location.x;
                y += r.location.y;
                Integer lastHealth = mySquad.get(r.ID);
                if(lastHealth != null && r.health < lastHealth && (inDanger == null || distTo(r.location) < distTo(inDanger.location))){
                    inDanger = r;
                    dangerCounter = 2;
                }
                newSquad.put(r.ID,r.health);
            }
            if(isAttacker && touching(r.location)){
                adjacentLaunchers++;
            }
            if (isAttacker && r.getID() < leaderID) {
                leaderID = r.getID();
            }
        }

        if(inDanger!=null){
            rc.setIndicatorDot(inDanger.location,255,0,0);
            rc.setIndicatorString(Integer.toString(inDanger.ID));
        }
        if(packSize>0){
            packless = false;
            squadAverageLocation = new MapLocation((int)Math.round(x/(double)packSize), (int)Math.round(y/(double)packSize));
            rc.setIndicatorDot(squadAverageLocation,0,255,0);
        }else if(packless){
            squadAverageLocation = null;
        }else{
            packless = true;
        }
        mySquad = newSquad;
    }

    public void squadGobble() throws GameActionException {
        if(inDanger!=null){
            fuzzyMove(inDanger.location);
        }else{
            if (leaderID == rc.getID()) {
                if(adjacentLaunchers >= Math.min(2,packSize)){
                    bugNav(exploreTarget);
                    exploreTurns++;
                    if ((rc.canSenseLocation(exploreTarget) && (rc.senseRobotAtLocation(exploreTarget) == null || touching(exploreTarget))) || exploreTurns > MAX_EXPLORE_TURNS) {
                        exploreTarget = randomLocation(allPossibleHQs());
                        exploreTurns = 0;
                    }
                }else{
//                    fuzzyMove(squadAverageLocation);
                }
            } else {
                RobotInfo leader = rc.senseRobot(leaderID);
                fuzzyMove(leader.getLocation());
                rc.setIndicatorLine(rc.getLocation(), leader.getLocation(), 165, 42, 42);
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
            lastAttack = target.location;
//            if(target.type == RobotType.LAUNCHER){
//                tryMove(dirTo(target.location).opposite());
//            }
            if(target.type == RobotType.LAUNCHER || (target.type == RobotType.CARRIER && distTo(target.location) <= RobotType.CARRIER.actionRadiusSquared)){
                if(squadAverageLocation!=null){
                    if(!touching(squadAverageLocation)){
                        fuzzyMove(squadAverageLocation);
                    }
                }else{
                    fuzzyMove(dirTo(target.location).opposite());
                }
            }
            return true;
        }
        return false;
    }
}
