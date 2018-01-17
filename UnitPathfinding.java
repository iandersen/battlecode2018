import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayList;

import bc.Direction;
import bc.GameController;
import bc.MapLocation;
import bc.Planet;
import bc.Unit;
import bc.UnitType;
import bc.VecUnit;

public class UnitPathfinding {
	private static Direction[] directions = { Direction.North, Direction.Northeast, Direction.East, Direction.Southeast,
			Direction.South, Direction.Southwest, Direction.West, Direction.Northwest };
	private static int[] tryToTurn = { 0, 1, -1, 2, -2 };
	private static Map<Integer, LinkedList<MapLocation>> stinkySquares = new HashMap<Integer, LinkedList<MapLocation>>();
	private static GameController gc = Player.gc;
	private static final int MAX_STINKY_SQUARES = 6;
	private static int[][] earthCollisionMap;
	private static int[][] marsCollisionMap;
	private static boolean earthInitialized = false;
	private static boolean marsInitialized = false;

	public static void updateMap() {
		if (gc.planet().equals(Planet.Earth) && !earthInitialized) {
			long height = gc.startingMap(gc.planet()).getHeight();
			long width = gc.startingMap(gc.planet()).getWidth();
			earthCollisionMap = new int[(int) width][(int) height];
			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					earthCollisionMap[x][y] = gc.startingMap(gc.planet())
							.isPassableTerrainAt(new MapLocation(gc.planet(), x, y));
			earthInitialized = true;
		}
		if (gc.planet().equals(Planet.Mars) && !marsInitialized) {
			long height = gc.startingMap(gc.planet()).getHeight();
			long width = gc.startingMap(gc.planet()).getWidth();
			marsCollisionMap = new int[(int) width][(int) height];
			for (int x = 0; x < width; x++)
				for (int y = 0; y < height; y++)
					marsCollisionMap[x][y] = gc.startingMap(gc.planet())
							.isPassableTerrainAt(new MapLocation(gc.planet(), x, y));
			marsInitialized = true;
		}
		refreshUnitPositionsOnMap();
	}

	private static void refreshUnitPositionsOnMap() {
		long height = gc.startingMap(gc.planet()).getHeight();
		long width = gc.startingMap(gc.planet()).getWidth();
		int[][] map = collisionMap(gc.planet());
		for (int x = 0; x < width; x++)
			for (int y = 0; y < height; y++) {
				if (map[x][y] == 2)
					map[x][y] = 0;
				if (gc.canSenseLocation(new MapLocation(gc.planet(), x, y))
						&& gc.isOccupiable(new MapLocation(gc.planet(), x, y)) == 0
						|| gc.hasUnitAtLocation(new MapLocation(gc.planet(), x, y)))
					map[x][y] = 2;
			}
	}

	private static MapLocation startPathToLocation(Unit unit, MapLocation location) {
		HashMap<MapLocation, MapLocation> path = new HashMap<MapLocation, MapLocation>();
		int height = (int) gc.startingMap(gc.planet()).getHeight();
		int width = (int) gc.startingMap(gc.planet()).getWidth();
		boolean[][] isVisited = new boolean[width][height];
		boolean[][] isOpen = new boolean[width][height];
		int destX = location.getX();
		int destY = location.getY();
		MapLocation origin = unit.location().mapLocation();
		MapLocation currentNode = origin;
		MapLocation destNode = origin;
		addAdjacentNodes(currentNode, isOpen, isVisited);
		isVisited[origin.getX()][origin.getY()] = true;
		int steps = 0;
		while (currentNode.getX() != destX || currentNode.getY() != destY) {
			isVisited[currentNode.getX()][currentNode.getY()] = true;
			isOpen[currentNode.getX()][currentNode.getY()] = false;
			addAdjacentNodes(currentNode, isOpen, isVisited);
			MapLocation bestNode = getClosestSquare(isOpen, location, origin, isVisited);
//			System.out.println("moving from " + origin + " to: " + bestNode + " to get to " + location);
			isOpen[bestNode.getX()][bestNode.getY()] = false;
			if(currentNode.getX() == origin.getX() && currentNode.getY() == origin.getY()){
				destNode = bestNode;
			}
			currentNode = bestNode;
			steps++;
		}
		System.out.println("Steps: " + steps);
		System.out.println("moving from " + origin + " to: " + destNode + " to get to " + location);
		return destNode;
	}

	private static MapLocation getClosestSquare(boolean[][] openNodes, MapLocation destination, MapLocation origin, boolean[][] isVisited) {
		long smallestDistance = Integer.MAX_VALUE;
		MapLocation ret = origin;
		int startX = origin.getX();
		int startY = origin.getY();
		int destX = origin.getX();
		int destY = origin.getY();
		for (int x = 0; x < openNodes.length; x++) {
			for (int y = 0; y < openNodes[x].length; y++) {
				if (openNodes[x][y] && !isVisited[x][y]) {
					int dist = (int) (Math.pow(x - destX, 2) + Math.pow(y - destY, 2) + 2*(Math.pow(x - startX, 2)
							+ Math.pow(y - startY, 2)));
					if (dist < smallestDistance) {
						smallestDistance = dist;
						ret = new MapLocation(gc.planet(), x, y);
					}
				}
			}
		}
		return ret;
	}

	private static void addAdjacentNodes(MapLocation loc, boolean[][] isOpen, boolean[][] isVisited) {
		for (Direction d : directions) {
			MapLocation m = loc.add(d);
			int x = m.getX();
			int y = m.getY();
			if(x >= 0 && y >= 0 && x < isOpen.length && y < isOpen[0].length){
				if (!isVisited[x][y]) {
					if (gc.canSenseLocation(m) && gc.isOccupiable(m) == 1 && !gc.hasUnitAtLocation(m))
						isOpen[x][y] = true;
					else if (!gc.canSenseLocation(m))
						isOpen[x][y] = true;
				}
			}
		}
	}

	private static int[][] collisionMap(Planet p) {
		if (p.equals(Planet.Mars))
			return marsCollisionMap;
		else
			return earthCollisionMap;
	}

	public static Direction moveUnitTowardLocation(Unit unit, MapLocation location) {
		MapLocation unitMapLocation = unit.location().mapLocation();
//		Direction dir = unitMapLocation.directionTo(startPathToLocation(unit, location));
//		if(canMoveUnitInDirection(unit, dir))
//			moveUnitInDirection(unit, dir);
//		return dir;
		 Direction idealDirection = unitMapLocation.directionTo(location);
		 Direction returnDirection = unitMapLocation.directionTo(location);
		 for (int i = 0; !canMoveUnitInDirection(unit, returnDirection) && i <
		 tryToTurn.length; i++) {
		 returnDirection = rotate(idealDirection, tryToTurn[i]);
		 }
		 if(canMoveUnitInDirection(unit, returnDirection))
		 moveUnitInDirection(unit, returnDirection);
		 return returnDirection;
	}

	private static void moveUnitInDirection(Unit unit, Direction direction) {
		gc.moveRobot(unit.id(), direction);
		Queue<MapLocation> badSquares = stinkySquares.get(unit.id());
		if (badSquares == null)
			badSquares = new LinkedList<MapLocation>();
		if (badSquares.size() >= MAX_STINKY_SQUARES)
			badSquares.poll();
		badSquares.add(unit.location().mapLocation().add(direction));
	}

	private static boolean canMoveUnitInDirection(Unit unit, Direction direction) {
		Queue<MapLocation> badSquares = stinkySquares.get(unit.id());
		MapLocation newLocation = unit.location().mapLocation().add(direction);
		if (badSquares != null && badSquares.contains(newLocation))
			return false;
		return gc.canMove(unit.id(), direction);
	}

	private static Direction rotate(Direction start, int rotation) {
		int startIndex = 0;
		for (int i = 0; i < directions.length; i++)
			if (directions[i].equals(start))
				startIndex = i;
		return directions[(startIndex + rotation + directions.length) % (directions.length - 1)];
	}

	public static Direction firstAvailableDirection(Unit unit) {
		for (int i = 0; i < directions.length; i++) {
			MapLocation newLoc = unit.location().mapLocation().add(directions[i]);
			long height = gc.startingMap(gc.planet()).getHeight();
			long width = gc.startingMap(gc.planet()).getWidth();
			if (newLoc.getX() >= 0 && newLoc.getY() >= 0)
				if (newLoc.getX() < width && newLoc.getY() < height)
					if (gc.canSenseLocation(newLoc) && gc.isOccupiable(newLoc) < 1)
						return directions[i];
		}
		return Direction.Center;
	}

	public static Direction firstAvailableBuildDirection(Unit unit, UnitType structureType) {
		for (int i = 0; i < directions.length; i++) {
			if (gc.canBlueprint(unit.id(), structureType, directions[i]))
				return directions[i];
		}
		return Direction.Center;
	}
	
	public static Direction firstAvailableUnloadDirection(Unit unit) {
		for (int i = 0; i < directions.length; i++) {
			if (gc.canUnload(unit.id(), directions[i]))
				return directions[i];
		}
		return Direction.Center;
	}
}
