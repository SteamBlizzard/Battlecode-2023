package WeBall;

import battlecode.common.*;

public class Launcher extends Robot {
    public Launcher(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void turn() throws Exception {
        RobotInfo[] friends = rc.senseNearbyRobots(rc.getType().visionRadiusSquared,rc.getTeam());
        RobotInfo bestFriend = null;
        for(RobotInfo r : friends){
            if(r.getTotalAnchors()>0){
                bestFriend = r;
                break;
            }
            if(r.type.equals(RobotType.CARRIER)){
                bestFriend = r;
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
        }else{
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
                    fuzzyMove(RobotPlayer.randomDirection());
                }
            }
        }


    }


}
