import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.ArrayList;

import bc.Direction;
import bc.GameController;
import bc.MapLocation;
import bc.Unit;
import bc.UnitType;

public class UnitPathfinding {
	private static Direction[] directions = { Direction.North, Direction.Northeast, Direction.East, Direction.Southeast,
			Direction.South, Direction.Southwest, Direction.West, Direction.Northwest };
	private static int[] tryToTurn = { 0, 1, -1, 2, -2 };
	private static Map<Integer, LinkedList<MapLocation>> stinkySquares = new HashMap<Integer, LinkedList<MapLocation>>();
	private static GameController gc = Player.gc;
	private static final int MAX_STINKY_SQUARES = 6;

	public static Direction moveUnitTowardLocation(Unit unit, MapLocation location) {
		MapLocation unitMapLocation = unit.location().mapLocation();
		Direction idealDirection = unitMapLocation.directionTo(location);
		Direction returnDirection = unitMapLocation.directionTo(location);
		for (int i = 0; !canMoveUnitInDirection(unit, returnDirection) && i < tryToTurn.length; i++) {
			returnDirection = rotate(idealDirection, tryToTurn[i]);
		}
		if(canMoveUnitInDirection(unit, returnDirection))
			moveUnitInDirection(unit, returnDirection);
		return returnDirection;
	}
	
	private static void moveUnitInDirection(Unit unit, Direction direction){
		gc.moveRobot(unit.id(), direction);
		Queue<MapLocation> badSquares = stinkySquares.get(unit.id());
		if(badSquares == null)
			badSquares = new LinkedList<MapLocation>();
		if(badSquares.size() >= MAX_STINKY_SQUARES)
			badSquares.poll();
		badSquares.add(unit.location().mapLocation().add(direction));
	}
	
	private static boolean canMoveUnitInDirection(Unit unit, Direction direction){
		Queue<MapLocation> badSquares = stinkySquares.get(unit.id());
		MapLocation newLocation = unit.location().mapLocation().add(direction);
		if(badSquares != null && badSquares.contains(newLocation))
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
		for(int i = 0; i < directions.length; i++){
			if(gc.isOccupiable(unit.location().mapLocation().add(directions[i])) < 1)
				return directions[i];
		}
		return Direction.Center;
	}
	
	public static Direction firstAvailableBuildDirection(Unit unit, UnitType structureType) {
		for(int i = 0; i < directions.length; i++){
			if(gc.canBlueprint(unit.id(), structureType, directions[i]))
				return directions[i];
		}
		return Direction.Center;
	}
}
