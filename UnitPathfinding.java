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
//		refreshUnitPositionsOnMap();
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
		int[][] costMap = new int[width][height];
		for(int x = 0; x < costMap.length; x++)
			for(int y = 0; y < costMap[0].length; y++)
				costMap[x][y] = 0;
		updateHMap(costMap, new Coord(origin), new Coord(location));
		updateHMap(costMap, new Coord(location), new Coord(origin));
		//print2dArray(costMap);
		int minCost = Integer.MAX_VALUE;
		for(Direction d : directions){
			MapLocation l = origin.add(d);
			if(l.getX() < width && l.getX() >= 0 && l.getY() < height && l.getY() >= 0){
				int cost = costMap[l.getX()][l.getY()];
				if(cost < minCost){
					minCost = cost;
					destNode = l;
				}
			}
		}
		return destNode;
	}
	
	private static void updateHMap(int[][] costMap, Coord start, Coord finish){
		Stack<Coord> openNodes = new Stack<Coord>();
		Stack<Coord> visitedNodes = new Stack<Coord>();
		Stack<Coord> temp = new Stack<Coord>();
		openNodes.push(start);
		int depth = 0;
		outer: while(true){
			while(openNodes.size() > 0){
				Coord n = openNodes.pop();
				visitedNodes.push(n);
				Stack<Coord> adjNodes = getAdjacentNodes(n, visitedNodes, openNodes);
				for(Coord m : adjNodes){
					//if(m.equals(finish))
					//	break outer;
					if(!temp.contains(m)){
						costMap[m.getX()][m.getY()] += depth + 1;
						temp.push(m);
					}
				}
			}
			depth++;
			openNodes = temp;
			temp = new Stack<Coord>();
			if(openNodes.size() == 0)
				break;
		}
	}
	
	public static class Coord{
		Planet p;
		int x;
		int y;
		public Coord(MapLocation m){
			this.p = m.getPlanet();
			this.x = m.getX();
			this.y = m.getY();
		}
		
		public Coord(Planet p, int x, int y){
			this.p = p;
			this.x = x;
			this.y = y;
		}
		
		public int getX(){
			return this.x;
		}
		
		public Planet getPlanet(){
			return p;
		}
		
		public int getY(){
			return this.y;
		}
		
		public MapLocation toLocation(){
			return new MapLocation(this.p, this.x, this.y);
		}
		
		public boolean equals(Object o){
			if(o instanceof Coord){
				Coord c = (Coord)o;
				if(c.p.toString().equals(this.p.toString()))
					if(c.x == this.x && c.y == this.y)
						return true;
			}
			return false;
		}
	}
	
	private static void print2dArray(boolean[][] array) {
        int length = 2;
        for (boolean[] row : array) {
            for (boolean col : row) {
            	int n = col ? 1 : 0;
                String s = "" + n;
                while (s.length() <= length)
                    s = " " + s;
                System.out.print(s);
            }
            System.out.println();
        }
    }
	
	private static void print2dArray(int[][] array) {
		int length = 4;
		for (int[] row : array) {
			for (int col : row) {
				String s = "" + col;
				while (s.length() <= length)
					s = " " + s;
				System.out.print(s);
			}
			System.out.println();
		}
	}

	private static MapLocation getClosestSquare(boolean[][] openNodes, MapLocation destination, MapLocation origin, boolean[][] isVisited) {
		long smallestDistance = Integer.MAX_VALUE;
		MapLocation ret = origin;
		int destX = origin.getX();
		int destY = origin.getY();
		for (int x = 0; x < openNodes.length; x++) {
			for (int y = 0; y < openNodes[x].length; y++) {
				if (openNodes[x][y] && !isVisited[x][y]) {
					int dist = Math.max(Math.abs(x - destX), Math.abs(y - destY)); 
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
			int[][] colMap = collisionMap(gc.planet());
			int x = m.getX();
			int y = m.getY();
			if(x >= 0 && y >= 0 && x < isOpen.length && y < isOpen[0].length){
				if (!isVisited[x][y]) {
					if(colMap[x][y] == 1)
						isOpen[x][y] = true;
				}
			}
		}
	}
	
	private static Stack<Coord> getAdjacentNodes(Coord loc, Stack<Coord> visited, Stack<Coord> open) {
		Stack<Coord> ret = new Stack<Coord>();
		for (Direction d : directions) {
			Coord m = new Coord(loc.toLocation().add(d));
			int[][] colMap = collisionMap(gc.planet());
			int x = m.getX();
			int y = m.getY();
			if(x >= 0 && y >= 0 && x < colMap.length && y < colMap[0].length){
				if (!visited.contains(m) && !open.contains(m)) {
					if(colMap[x][y] == 1)
						ret.push(m);
				}
			}
		}
		return ret;
	}

	private static int[][] collisionMap(Planet p) {
		if (p.equals(Planet.Mars))
			return marsCollisionMap;
		else
			return earthCollisionMap;
	}

	public static Direction moveUnitTowardLocation(Unit unit, MapLocation location) {
		MapLocation unitMapLocation = unit.location().mapLocation();
		Direction dir = unitMapLocation.directionTo(startPathToLocation(unit, location));
		if(canMoveUnitInDirection(unit, dir))
			moveUnitInDirection(unit, dir);
		return dir;
//		 Direction idealDirection = unitMapLocation.directionTo(location);
//		 Direction returnDirection = unitMapLocation.directionTo(location);
//		 for (int i = 0; !canMoveUnitInDirection(unit, returnDirection) && i <
//		 tryToTurn.length; i++) {
//		 returnDirection = rotate(idealDirection, tryToTurn[i]);
//		 }
//		 if(canMoveUnitInDirection(unit, returnDirection))
//		 moveUnitInDirection(unit, returnDirection);
//		 return returnDirection;
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
		if(unit.location().isOnMap())
			for (int i = 0; i < directions.length; i++) {
				MapLocation newLoc = unit.location().mapLocation().add(directions[i]);
				long height = gc.startingMap(gc.planet()).getHeight();
				long width = gc.startingMap(gc.planet()).getWidth();
				if (newLoc.getX() >= 0 && newLoc.getY() >= 0)
					if (newLoc.getX() < width && newLoc.getY() < height)
						if (gc.canMove(unit.id(), directions[i]))
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
		if(unit.location().isOnMap())
			for (int i = 0; i < directions.length; i++) {
				if (gc.canUnload(unit.id(), directions[i]))
					return directions[i];
			}
		return Direction.Center;
	}
}
