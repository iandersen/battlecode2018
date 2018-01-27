import java.util.ArrayList;
import java.util.HashMap;

import bc.Direction;
import bc.GameController;
import bc.MapLocation;
import bc.Planet;
import bc.PlanetMap;
import bc.Team;
import bc.Unit;
import bc.UnitType;
import bc.VecUnit;
import bc.Direction;

public class MarsUnitController {
	public static HashMap<Integer, Unit> allEnemies = new HashMap<Integer, Unit>();
	private static GameController gc = Player.gc;
	static long[][] karboniteCount;
	static boolean initialized = false;
	private static HashMap<Integer, MapLocation> pressON = new HashMap<>();
	
	public static void init() {
		updateEnemyList();
		updateUnitAges();
		if (!initialized && gc.planet().equals(Planet.Mars)) {
			PlanetMap map = gc.startingMap(gc.planet());
			if (map != null) {
				karboniteCount = new long[(int) map.getWidth()][(int) map.getHeight()];
				for (int x = 0; x < map.getWidth(); x++)
					for (int y = 0; y < map.getHeight(); y++) {
						MapLocation loc = new MapLocation(gc.planet(), x, y);
						karboniteCount[x][y] = map.initialKarboniteAt(loc);
					}
				initialized = true;
			}
		}
	}
	public static void updateUnitAges(){
		VecUnit myUnits = gc.myUnits();
		for (int i = 0; i < myUnits.size(); i++) {
			Unit u = myUnits.get(i);
			UnitProps props = UnitProps.get(u.id());
			props.age = props.age + 1;
		}
	}

	
	public static void factoryStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.factoryStep(unit);
	}

	public static void healerStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.healerStep(unit);
	}

	public static void knightStep(Unit unit) {
		
		
		if(!unit.location().isOnMap())
			return;
		boolean pressOn = false;
		if (pressON.get(unit.id()) != null) {
			pressOn = true;
		}
		
		VecUnit enemies = gc.senseNearbyUnits(unit.location().mapLocation(),(long) Math.floor(Math.sqrt(unit.attackRange())));
		boolean attacked = false;
		if(unit.attackHeat() < 10)
			for(int i = 0; i < enemies.size(); i++){
				Unit enemy = enemies.get(i);
				if(!enemy.team().equals(gc.team()))
					if(gc.canAttack(unit.id(), enemy.id())){
						gc.attack(unit.id(), enemy.id());
						pressON.put(unit.id(), enemy.location().mapLocation());
						attacked = true;
						break;
					}
			}
		if(Math.random() * 8 < .5 || pressOn){
			if(!attacked){
				if (unit.movementHeat() < 10 && pressOn && gc.canMove(unit.id(), directionToOtherSquare(unit, pressON.get(unit.id())))) {
					gc.moveRobot(unit.id(), directionToOtherSquare(unit, pressON.get(unit.id())));
					System.out.println("I did something useful");
					
					if(enemies.size() == 0) {
						pressOn = false;
					}
				}
				else {
				Direction direction = UnitPathfinding.firstAvailableDirection(unit);
				//System.out.println("knight wants to move in direction: " + direction);
				if(unit.movementHeat() == 0 && gc.canMove(unit.id(), direction))
					gc.moveRobot(unit.id(), direction);
				}
			}
		}
	}
	
	public static Direction directionToOtherSquare(Unit me, MapLocation spot) {
		MapLocation loc = me.location().mapLocation();
		if(loc.getX() == spot.getX()) {
			if (loc.getY() == spot.getY() + 1)
				return Direction.North;
			if (loc.getY() == spot.getY() - 1)
				return Direction.South;
			
		}
		else if (loc.getX() == spot.getX() + 1) {
			if (loc.getY() == spot.getY())
				return Direction.East;
			if (loc.getY() == spot.getY() + 1)
				return Direction.Northeast;
			if (loc.getY() == spot.getY() - 1)
				return Direction.Southeast;
			
		}
		else if (loc.getX() == spot.getX() + -1) {
			if (loc.getY() == spot.getY())
				return Direction.West;
			if (loc.getY() == spot.getY() + 1)
				return Direction.Northwest;
			if (loc.getY() == spot.getY() - 1)
				return Direction.Southwest;
			
		}
		return Direction.Center;
	}
	
	

	public static void mageStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		
		ArrayList<MapLocation> list = new ArrayList<>();
		
		Object[] keys = allEnemies.keySet().toArray();
		if (unit.attackHeat() < 10) {
			for (int i = 0; i < allEnemies.size(); i++) {
				int id = (int) keys[i];
				Unit enemy = allEnemies.get(id);
				if (!enemy.team().equals(gc.team()))
					if (gc.canAttack(unit.id(), enemy.id())) {
						// keep track
						list.add(enemy.location().mapLocation());
					}
			}
			if (list.size() > 0)
				gc.attack(unit.id(), gc.senseUnitAtLocation(nearby(list)).id());
		}
		
		meshStep(unit);
	}

	public static void rangerStep(Unit unit) {
		// TODO Auto-generated method stub
		if (!unit.location().isOnMap())
			return;
		Object[] keys = allEnemies.keySet().toArray();
		if (unit.attackHeat() < 10) {
			long minHealth = Long.MAX_VALUE;
			Unit bestUnit = null;
			for (int i = 0; i < allEnemies.size(); i++) {
				int id = (int) keys[i];
				Unit enemy = allEnemies.get(id);
				long dist = unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());
				if (dist <= unit.attackRange())
					if (dist > unit.rangerCannotAttackRange()) {
						if (gc.canAttack(unit.id(), enemy.id())) {
							if(enemy.health() < minHealth){
								minHealth = enemy.health();
								bestUnit = enemy;
							}
						}
					}
			}
			if(bestUnit != null){
				gc.attack(unit.id(), bestUnit.id());
				System.out.println("Somebody has been attacked");
			}
		}
		meshStep(unit);
	}
	
	public static MapLocation nearby(ArrayList<MapLocation> list) {
		// return the best square
		int[] cation = new int[list.size()];
		for (int i = 0; i < cation.length; i++) {
			cation[i] = 0;
		}
		int i = 0;
		PlanetMap map = gc.startingMap(gc.planet());
		Team enemyTeam = gc.team().equals(Team.Blue) ? Team.Red : Team.Blue;
		for (MapLocation place : list) {
			Direction[] directions = { Direction.North, Direction.Northeast, Direction.East, Direction.Southeast,
					Direction.South, Direction.Southwest, Direction.West, Direction.Northwest };
			for (Direction d : directions) {
				MapLocation square = place.add(d);
				if (map.onMap(square)) {
					if (gc.hasUnitAtLocation(square)) {
						if (gc.senseUnitAtLocation(square).team().equals(enemyTeam))
							cation[i]++;
					}
				}
			}
			i++;
		
		}
		// search for max
		int max = cation[0];
		int tar = 0;
		for (int k = 1; k < list.size(); k++) {
			if (cation[k] > max) {
				max = cation[k];
				tar = k;
			}
		}
		return list.get(tar);
	}

	public static void rocketStep(Unit unit) {
		updateEnemyList();
		updateUnitAges();
		// TODO Auto-generated method stub
		if(Player.numberOfUnitType(UnitType.Knight) < 20)
			if (unit.structureGarrison().size()!=0 && !UnitPathfinding.firstAvailableUnloadDirection(unit).equals(Direction.Center)) {
				Player.gc.unload(unit.id(), UnitPathfinding.firstAvailableUnloadDirection(unit));
			}
	}
	
	public static void meshStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
			int id = unit.id();
			UnitProps props = UnitProps.get(id);
			if (props.movesInStartDirection == 0) {
				Direction d = UnitPathfinding.firstAvailableDiagMoveDirection(unit);
				MapLocation loc = unit.location().mapLocation();
				if (d.equals(Direction.Center)) {
					Direction[] ds = { Direction.Northwest, Direction.Northeast, Direction.Southeast,
							Direction.Southwest };
					for (Direction x : ds) {
						MapLocation newLoc = loc.add(x);
						if (gc.hasUnitAtLocation(newLoc)) {
							Unit u = gc.senseUnitAtLocation(newLoc);
							UnitProps uProps = UnitProps.get(u.id());
							uProps.movesInStartDirection = 0;
						}
					}
				} else {
					if (unit.movementHeat() < 10)
						if (gc.canMove(id, d)) {
							gc.moveRobot(id, d);
							props.movesInStartDirection++;
						}
				}
			}
		
	}
	
	public static void updateEnemyList() {
		allEnemies.clear();
		VecUnit myUnits = gc.myUnits();
		Team enemyTeam = gc.team().equals(Team.Blue) ? Team.Red : Team.Blue;
		for (int i = 0; i < myUnits.size(); i++) {
			Unit u = myUnits.get(i);
			if (u.location().isOnMap()) {
				VecUnit enemies = gc.senseNearbyUnitsByTeam(u.location().mapLocation(), u.visionRange(), enemyTeam);
				for (int n = 0; n < enemies.size(); n++)
					allEnemies.put(enemies.get(n).id(), enemies.get(n));
			}
		}
	}

	public static void workerStep(Unit unit) {
		
				meshStep(unit);
			
		
	}

}
