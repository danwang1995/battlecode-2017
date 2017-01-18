package dendrophileplayer;
import java.util.ArrayList;
import java.util.List;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static boolean plantedTree = false;
    public static final float BULLETS_TO_WIN = GameConstants.BULLET_EXCHANGE_RATE * GameConstants.VICTORY_POINTS_TO_WIN;
    public static final float PLANTING_THRESHOLD = 1;
    
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     */
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
			default:
				break;
        }
	}
    
    /**
     * Donates 10,000 bullets to win the game if we have that many.
     * 
     * @throws GameActionException
     */
    static void donateToWinIfPossible() throws GameActionException {
    	if (rc.getTeamBullets() >= BULLETS_TO_WIN) {
    		rc.donate(BULLETS_TO_WIN);
    	}
    }

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	donateToWinIfPossible();

                // Generate a random direction
                Direction dir = randomDirection();
                
                // Randomly attempt to build a gardener in all directions
                for (int i = 0; i < 5; i++) {
                	Direction hiringDirection = dir.rotateLeftDegrees(60 * i);
                	
	                if (rc.canHireGardener(hiringDirection)) {
	                    rc.hireGardener(hiringDirection);
	                }
                }
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
    
    static void waterLowestTree() throws GameActionException {
    	TreeInfo[] nearbyTrees = rc.senseNearbyTrees(2, rc.getTeam());
        float lowestTreeHealth = GameConstants.BULLET_TREE_MAX_HEALTH + 1;
        int lowestTreeID = -1;
        
        for (TreeInfo nearbyTree : nearbyTrees) {
        	float nearbyTreeHealth = nearbyTree.getHealth();
        	int nearbyTreeID = nearbyTree.getID();
        	
        	if (rc.canWater(nearbyTreeID) && nearbyTreeHealth < lowestTreeHealth) {
        		lowestTreeHealth = nearbyTreeHealth;
        		lowestTreeID = nearbyTreeID;
        	}
        }
        
        if (rc.canWater(lowestTreeID)) {
        	rc.water(lowestTreeID);
        }
    }
    
    static void shakeGreatestNeutralTree() throws GameActionException {
    	TreeInfo[] neutralTrees = rc.senseNearbyTrees(2, Team.NEUTRAL);
        int greatestContainedBullets = -1;
        int greatestTreeID = -1;
        
        for (TreeInfo neutralTree : neutralTrees) {
        	int neutralTreeBullets = neutralTree.getContainedBullets();
        	int neutralTreeID = neutralTree.getID();
        	
        	if (rc.canShake(neutralTreeID) && neutralTreeBullets > greatestContainedBullets) {
        		greatestContainedBullets = neutralTreeBullets;
        		greatestTreeID = neutralTreeID;
        	}
        }
        
        if (greatestContainedBullets > 0 && rc.canShake(greatestTreeID)) {
        	rc.shake(greatestTreeID);
        }
    }
    
    static void plantNewTree() throws GameActionException {
    	List<Direction> plantableDirections = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
        	Direction plantingDirection = Direction.getEast().rotateLeftDegrees(60 * i);
        	
        	if (rc.canPlantTree(plantingDirection)) {
        		plantableDirections.add(plantingDirection);               		
        	}
        }
        
        System.out.println(plantableDirections.size());
        
        if (plantableDirections.size() >= PLANTING_THRESHOLD) {
        	for (Direction plantableDirection : plantableDirections) {
        		if (rc.canPlantTree(plantableDirection)) {
            		rc.plantTree(plantableDirection);      		
            	}
        	}
        	
        	// equivalent to rc.plantTree(plantableDirections.get(0));
        	
        	plantedTree = true;
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	donateToWinIfPossible();
            	
            	if (!plantedTree) { 
                    // Move randomly
                    tryMove(randomDirection());
            	}
            	
                shakeGreatestNeutralTree();
                plantNewTree();
                waterLowestTree();
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
