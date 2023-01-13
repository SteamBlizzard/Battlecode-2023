package GlovesOff;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
            RobotType.CARRIER,
            RobotType.LAUNCHER,
            RobotType.AMPLIFIER,
            RobotType.BOOSTER,
            RobotType.DESTABILIZER
    };

    static public final ResourceType[] resourceTypes = {
            ResourceType.ADAMANTIUM,
            ResourceType.MANA,
            ResourceType.ELIXIR
    };

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static int turnCount;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/


    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        Robot r = null;

        switch (rc.getType()) {
            case HEADQUARTERS:      r = new Headquarters(rc);  break;
            case CARRIER:           r = new Carrier(rc);   break;
            case LAUNCHER:          r = new Launcher(rc); break;
            case BOOSTER:           r = new Booster(rc); break;
            case DESTABILIZER:      r = new Destabilizer(rc); break;
            case AMPLIFIER:         r = new Amplifier(rc); break;
        }
        r.run();
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
}
