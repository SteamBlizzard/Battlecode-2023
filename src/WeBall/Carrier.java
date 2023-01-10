package WeBall;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.*;

public class Carrier extends Robot {

    WellInfo mainWell = null;
    public Carrier(RobotController rc) throws GameActionException {
        super(rc);
    }

    public void turn() throws Exception{
        WellInfo[] nearbyWells = rc.senseNearbyWells(rc.getType().visionRadiusSquared);

        if (mainWell == null) {
            mainWell = closestWell(nearbyWells, rc.getLocation());
        }

        if(mainWell != null){
            if(getWeight() == GameConstants.CARRIER_CAPACITY){
                fuzzyMove(home);
            }else{
                if(rc.getLocation().isAdjacentTo(mainWell.getMapLocation()) || rc.getLocation().equals(mainWell.getMapLocation())){
                    tryGetResource(mainWell);
                }
                fuzzyMove(dirTo(mainWell.getMapLocation()));
            }
        }else{
            explore();
        }
    }

    public void explore() throws GameActionException{
        fuzzyMove(RobotPlayer.randomDirection());
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
            amount = GameConstants.CARRIER_CAPACITY - weight
        }
        if(rc.canCollectResource(well.getMapLocation(),amount)){
            rc.collectResource(well.getMapLocation(),amount);
            return true;
        }

        return false;
    }

    public void deliverAnchor() {
        if (rc.ancho)
    }
}
