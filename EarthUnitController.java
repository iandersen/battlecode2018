import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import bc.GameController;
import bc.MapLocation;
import bc.Unit;
import bc.Direction;
import bc.Planet;
import bc.PlanetMap;
import bc.Team;
import bc.UnitType;
import bc.VecUnit;

public class EarthUnitController extends DefaultUnitController {
	private static UnitType[] createRobotList = { UnitType.Ranger, UnitType.Ranger, UnitType.Knight, UnitType.Knight, UnitType.Ranger, UnitType.Ranger, UnitType.Ranger, UnitType.Worker, UnitType.Healer, UnitType.Ranger, UnitType.Ranger};
	private static HashMap<Integer, Stack<MapLocation>> paths;
	private static HashMap<Integer, MapLocation> duties;
	private static int spot = 0;
	private static HashMap<Integer, Integer> factoryList = new HashMap<>();
	private static GameController gc = Player.gc;
	private static boolean timeToBuildaRocket = false;
	private static final int WORKER_REPLICATE_COST = 15;
	private static final int NUM_WORKERS = 8;
	private static int NUM_FACTORIES = 3;

	private static int NUM_FACTORIES_MAX = 10;
	private static int desiredRocketcount = 2;
	private static final int NUM_ROCKETS = 15;
	private static final int NUM_KNIGHTS = 100;
	private static final int MINE_TURNS = 50;
	private static final int KARBONITE_MIN = 0;
	public static HashMap<Integer, Unit> allEnemies = new HashMap<Integer, Unit>();
	static Map<Integer, UnitProps> allUnitProps = new HashMap<Integer, UnitProps>();
	static long[][] karboniteCount;
	static boolean initialized = false;
	private static int totalKarbonite = 1000;

	public static void init() {
		duties = new HashMap<>();
		paths = new HashMap<>();
		updateEnemyList();
		updateUnitAges();
		///BattleCodePathfinder x = new BattleCodePathfinder(map);
		if (!initialized && gc.planet().equals(Planet.Earth)) {
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

	public static void factoryStep(Unit unit) {
		 boolean lessThanFactoryCountDesired = false;
		 if (Player.numberOfUnitType(UnitType.Factory) < NUM_FACTORIES) {
			 lessThanFactoryCountDesired = true;

		 }



		 boolean able = !lessThanFactoryCountDesired && !timeToBuildaRocket;

		boolean beingAttacked = false;
		if (unit.health() < 70)
			beingAttacked = true;

		if (unit.structureIsBuilt() == 1) {
			//unloading
			if (unit.structureGarrison().size() != 0
					&& !UnitPathfinding.firstAvailableUnloadDirection(unit).equals(Direction.Center)) {
				gc.unload(unit.id(), UnitPathfinding.firstAvailableUnloadDirection(unit));
			}
			// building robots only if the minimum factory count has been met
			else if (able || beingAttacked) {
				if (gc.karbonite() > 25 && unit.isFactoryProducing() == 0 ) {
					// choose a robot to create
					    if(spot == createRobotList.length)
					    	spot = 0;
						gc.produceRobot(unit.id(), createRobotList[spot++]);
				}
			}

		}
	}

	public static void meshStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		int numRockets = Player.numberOfUnitType(UnitType.Rocket);
		Unit bestRocket = getBestRocket(unit);
		if (numRockets > 0 && bestRocket != null && gc.canLoad(bestRocket.id(), unit.id())) {
			VecUnit neighbors = gc.senseNearbyUnitsByTeam(unit.location().mapLocation(), 2, gc.team());
			for (int i = 0; i < neighbors.size(); i++) {
				int id = neighbors.get(i).id();
				UnitProps props = UnitProps.get(id);
				props.movesInStartDirection = 0;
			}
			gc.load(bestRocket.id(), unit.id());
		} else {
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
	}

	public static boolean checkForDutiesAndActorNot(Unit unit) {
		System.out.println("I am actually in the method");
		System.out.println("Duties? " + duties.get(unit.id()) == null );
		// location can be a rocket
		// or an enemy robot or factory
		if (!unit.location().isOnMap())
			return false;
		if (unit.unitType() == UnitType.Knight || unit.unitType() == UnitType.Mage) {
		VecUnit enemies = gc.senseNearbyUnits(unit.location().mapLocation(),
				(long) Math.floor(Math.sqrt(unit.attackRange())));
		if (unit.attackHeat() < 10)
			for (int i = 0; i < enemies.size(); i++) {
				Unit enemy = enemies.get(i);
				if (!enemy.team().equals(gc.team()))
					if (gc.canAttack(unit.id(), enemy.id())) {
						gc.attack(unit.id(), enemy.id());
						return true;
					}
			}
		}
		if (duties.get(unit.id()) == null)
		  return false;
		if (paths.get(unit.id()) == null)  {
			Stack<MapLocation> path = UnitPathfinding.pathToTarget(unit.location().mapLocation(), duties.get(unit.id()));
			if (unit.movementHeat() < 10 && !path.isEmpty()) {
					if (path.size() == 0) {
						duties.put(unit.id(), null); // absolve of duties
						return false;
					}
	 				MapLocation loc = path.peek();
	 				path.pop();
	 				if (gc.canMove(unit.id(),unit.location().mapLocation().directionTo(loc) )) {
	 				 paths.put(unit.id(), path); // update version
	 				 gc.moveRobot(unit.id(), unit.location().mapLocation().directionTo(loc));
	 				 return true;
	 				}


			}
			/// unit is already right next to friend or enemy
			else if (path.isEmpty()) {
				duties.put(unit.id(), null); // absolve of duties
				return false;
			}
			/// unit doesn't have a low enough movementHeat
			else {
				paths.put(unit.id(),path);
				return false; // nothing done
			}
		}
		/// already has existing path
		else {

			Stack<MapLocation> path = paths.get(unit.id());
			if (unit.movementHeat() < 10 && !path.isEmpty()) {
				System.out.print("Am I empty? " + path.isEmpty());
				MapLocation loc = path.peek();
				path.pop();
				if (gc.canMove(unit.id(),unit.location().mapLocation().directionTo(loc) )) {
				 paths.put(unit.id(),path); // save some computation time
				 gc.moveRobot(unit.id(), unit.location().mapLocation().directionTo(loc));
				 return true;
			   }
				/// perhaps bad path so give it one more try
			   else {
				 path = UnitPathfinding.pathToTarget(unit.location().mapLocation(), duties.get(unit.id()));
				 loc = path.peek();
				 path.pop();
				 if (gc.canMove(unit.id(),unit.location().mapLocation().directionTo(loc) )) {
					 paths.put(unit.id(),path); // save some computation time perhaps
					 gc.moveRobot(unit.id(), unit.location().mapLocation().directionTo(loc));
					 return true;
	 			 }
				 System.out.println("absolved of duties");
 				 duties.put(unit.id(), null);
				 return false;
			   }
			}
			else if (path.isEmpty()) {
				// copy of last time
				System.out.println("absolved of duties");
				duties.put(unit.id(), null); // absolve of duties
				return false;
			}
			/// need cooldown
			else {
				paths.put(unit.id(),path);
				return false;
			}
		}
	 System.out.println("somehow here");
		return false;
	}
	public static void healerStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		VecUnit friends = gc.senseNearbyUnits(unit.location().mapLocation(),
				(long) Math.floor(Math.sqrt(unit.attackRange())));
		if (unit.attackHeat() < 10 && unit.health() > 50)
			for (int i = 0; i < friends.size(); i++) {
				Unit friend = friends.get(i);
				if (friend.team().equals(gc.team()))
					if (gc.canHeal(unit.id(), friend.id())) {
						if (friend.health() < 50) {
							gc.heal(unit.id(), friend.id());
							break;
						}

					}
			}
		meshStep(unit);

	}
  
	public static void knightStep(Unit unit) {

			if (!unit.location().isOnMap())
				return;
			VecUnit enemies = gc.senseNearbyUnits(unit.location().mapLocation(),
					(long) Math.floor(Math.sqrt(unit.attackRange())));
			if (unit.attackHeat() < 10)
				for (int i = 0; i < enemies.size(); i++) {
					Unit enemy = enemies.get(i);
					if (!enemy.team().equals(gc.team()))
						if (gc.canAttack(unit.id(), enemy.id())) {
							gc.attack(unit.id(), enemy.id());
							break;
						}
				}
			meshStep(unit);
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

	public static void rangerStep(Unit unit) {
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
				/// if enemy is too close get the knights or mages to attack that square
				else {
						HashMap<Integer,Unit> list = getAllUnitsByType(UnitType.Knight);
						for (Integer id1 : list.keySet()) {
							if(duties.get(id1) == null) {
							duties.put(id1, enemy.location().mapLocation());
							break;
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

	public static void rocketStep(Unit unit) {
		if (unit.structureIsBuilt() == 1) {
			PlanetMap map = gc.startingMap(Planet.Mars);
			int x = (int) Math.floor(Math.random() * map.getWidth());
			int y = (int) Math.floor(Math.random() * map.getHeight());

			if (unit.structureGarrison().size() == 8) {
				// best to find an available spot
				while (map.isPassableTerrainAt(new MapLocation(Planet.Mars, x, y)) != 1) {
					x = (int) Math.floor(Math.random() * map.getWidth());
					y = (int) Math.floor(Math.random() * map.getHeight());

				}
				if (gc.canLaunchRocket(unit.id(), new MapLocation(Planet.Mars, x, y)))
					gc.launchRocket(unit.id(), new MapLocation(Planet.Mars, x, y));
			}
			if (unit.health() < 50 || gc.round() > 720) {
				while (map.isPassableTerrainAt(new MapLocation(Planet.Mars, x, y)) != 1) {
					x = (int) Math.floor(Math.random() * map.getWidth());
					y = (int) Math.floor(Math.random() * map.getHeight());

				}
				if (gc.canLaunchRocket(unit.id(), new MapLocation(Planet.Mars, x, y)))
					gc.launchRocket(unit.id(), new MapLocation(Planet.Mars, x, y));
			}
		}

	}

	public static void workerStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		//UnitProps.get(unit.id()).path;


		boolean able = Player.numberOfUnitType(UnitType.Factory) > 2;
		timeToBuildaRocket = false;
		if (Player.numberOfUnitType(UnitType.Rocket) < desiredRocketcount ) {
			timeToBuildaRocket = true;
		}

		if (Math.random() * 100 > 95) {
			timeToBuildaRocket = true;
		}
		int numWorkers = Player.numberOfUnitType(UnitType.Worker);
		int numFactories = Player.numberOfUnitType(UnitType.Factory);
		int numRockets = Player.numberOfUnitType(UnitType.Rocket);
		Unit structure = getUnfinishedStructure();
		if(gc.round() < 150){
			MapLocation loc = unit.location().mapLocation();
			if (unit.abilityHeat() < 10 && gc.karbonite() >= WORKER_REPLICATE_COST && numWorkers < NUM_WORKERS
					&& !UnitPathfinding.firstAvailableDirection(unit).equals(Direction.Center)) {
				workerReplicate(unit);
			} else if (structure != null
					&& (loc.distanceSquaredTo(structure.location().mapLocation()) <= 2 || unit.movementHeat() < 10)) {
				workerBuildFactory(unit, structure);
			} else if ((gc.round() < MINE_TURNS || gc.karbonite() < KARBONITE_MIN) && totalKarbonite > 50) {
				workerMine(unit);
			} else if (structure == null && numFactories < NUM_FACTORIES
					&& !UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Factory).equals(Direction.Center)) {
				workerBlueprintFactory(unit);
			} else if (gc.researchInfo().getLevel(UnitType.Rocket) > 0 && structure == null && numRockets < NUM_ROCKETS
					&& !UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket).equals(Direction.Center) && timeToBuildaRocket) {
				workerBlueprintRocket(unit);
			} else if (totalKarbonite > 0) {
				workerMine(unit);
			}
		}else{
			if(structure != null){
				MapLocation loc = unit.location().mapLocation();
				if(loc.distanceSquaredTo(structure.location().mapLocation()) <= 2 || unit.movementHeat() < 10) {
					workerBuildFactory(unit, structure);
				}
			} else if (structure == null && numFactories < NUM_FACTORIES) {
				workerBlueprintFactory(unit);
			} else if (gc.researchInfo().getLevel(UnitType.Rocket) > 0 && structure == null && numRockets < NUM_ROCKETS
					&& !UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket).equals(Direction.Center) && timeToBuildaRocket) {
				gc.blueprint(unit.id(), UnitType.Rocket,UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket) );
			} else{
				//if (Player.numberOfUnitType(UnitType.Rocket) > NUM_ROCKETS ) {
					meshStep(unit);
				//}

			}
		}
	
	}

	private static void workerReplicate(Unit unit) {
		Direction firstAvailableDirection = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Factory);
		if (!firstAvailableDirection.equals(Direction.Center))
			gc.replicate(unit.id(), firstAvailableDirection);
	}

	private static void workerBuildFactory(Unit unit, Unit structure) {
		MapLocation loc = unit.location().mapLocation();
		if (loc.distanceSquaredTo(structure.location().mapLocation()) <= 2) {
			int id = structure.id();
			if (gc.canBuild(unit.id(), id))
				gc.build(unit.id(), id);
		} else if (unit.movementHeat() < 10) {
			UnitPathfinding.moveUnitTowardLocation(unit, structure.location().mapLocation());
		} else {
			// System.out.println("Worker failed to act");
		}
	}

	private static void workerBlueprintFactory(Unit unit) {
		System.out.println("Number of factories is " + Player.numberOfUnitType(UnitType.Factory));
		int numFactories = Player.numberOfUnitType(UnitType.Factory);
		MapLocation unitLocation = unit.location().mapLocation();
		if (numFactories == 0) {
			Direction direction = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Factory);
			gc.blueprint(unit.id(), UnitType.Factory, direction);
		}
		else {
			HashMap<Integer, Unit> factories = getAllUnitsByTypeOrderedByAge(UnitType.Factory);
			List<Integer> orderedKeys = new ArrayList<Integer>();
			for (int i : factories.keySet())
				orderedKeys.add(i);
			Collections.sort(orderedKeys);
			for (int i : orderedKeys) {
				System.out.println("key: " + i);
			}
			outer: for (int i : orderedKeys) {
				Unit factory = factories.get(i);
				MapLocation source = factory.location().mapLocation();
				Direction[] buildDirections = { Direction.Northwest, Direction.Northeast, Direction.Southeast,
						Direction.Southwest, Direction.North, Direction.South, Direction.East, Direction.West };
				int dist = 2;
				for (Direction d : buildDirections) {
					MapLocation newSpot = new MapLocation(source.getPlanet(), source.getX(), source.getY());
					for (int n = 0; n < dist; n++)
						newSpot = newSpot.add(d);
					if (gc.startingMap(gc.planet()).onMap(newSpot))
						if (gc.startingMap(gc.planet()).isPassableTerrainAt(newSpot) == 1
								&& !gc.hasUnitAtLocation(newSpot)) {
							if (unitLocation.distanceSquaredTo(newSpot) <= 2) {
								if (gc.canBlueprint(unit.id(), UnitType.Factory, unitLocation.directionTo(newSpot))) {
									gc.blueprint(unit.id(), UnitType.Factory, unitLocation.directionTo(newSpot));
									UnitProps props = UnitProps.props.get(unit.id());
									if (props == null)
										props = new UnitProps();
									props.hasBuiltFactory = true;
									System.out.println("Built factory");
									UnitProps.props.put(unit.id(), props);
								}
							} else if (unit.movementHeat() < 10) {
								UnitPathfinding.moveUnitTowardLocation(unit, newSpot);
							}
							break outer;
						}
				}
			}
		}
	}

	public static HashMap<Integer, Unit> getAllUnitsByType(UnitType type) {
		HashMap<Integer, Unit> ret = new HashMap<Integer, Unit>();
		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			if (unit.unitType() == type)
				ret.put(unit.id(), unit);
		}
		return ret;
	}

	public static HashMap<Integer, Unit> getAllUnitsByTypeOrderedByAge(UnitType type) {
		HashMap<Integer, Unit> ret = new HashMap<Integer, Unit>();
		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			int age = UnitProps.get(i).age;
			if (unit.unitType() == type)
				ret.put(age, unit);
		}
		return ret;
	}

	private static void workerBlueprintRocket(Unit unit) {
		Direction direction = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket);
		gc.blueprint(unit.id(), UnitType.Rocket, direction);
	}

	private static void workerMine(Unit unit) {
		long nanos = System.nanoTime();
		MapLocation deposit = bestKarboniteDeposit(unit);
		for (Direction dir : Direction.values()) {
			if (gc.canHarvest(unit.id(), dir)) {
				gc.harvest(unit.id(), dir);
				karboniteCount[deposit.getX()][deposit.getY()] -= Math.min(unit.workerHarvestAmount(),
						karboniteCount[deposit.getX()][deposit.getY()]);
				// System.out.println("Worker time elapsed (1): " +
				// (Math.round((System.nanoTime() - nanos) / 1000.0) / 1000.0));
				return;
			}
		}
		if (unit.movementHeat() < 10 && deposit.distanceSquaredTo(unit.location().mapLocation()) > 2) {
			UnitPathfinding.moveUnitTowardLocation(unit, deposit);
			// System.out.println("Worker time elapsed (2): " +
			// (Math.round((System.nanoTime() - nanos) / 1000.0) / 1000.0));
			return;
		}
		if (gc.canHarvest(unit.id(), unit.location().mapLocation().directionTo(deposit))) {
			gc.harvest(unit.id(), unit.location().mapLocation().directionTo(deposit));
			karboniteCount[deposit.getX()][deposit.getY()] -= unit.workerHarvestAmount();
			// System.out.println("Worker time elapsed (3): " +
			// (Math.round((System.nanoTime() - nanos) / 1000.0) / 1000.0));
			return;
		}
		if (deposit.distanceSquaredTo(unit.location().mapLocation()) <= 2) {
			karboniteCount[deposit.getX()][deposit.getY()] = 0;
		}
		// System.out.println("Worker time elapsed (4): " +
		// (Math.round((System.nanoTime() - nanos) / 1000.0) / 1000.0));
	}

	public static Unit getUnfinishedStructure() {
		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			UnitType type = unit.unitType();
			if (type.equals(UnitType.Factory) || type.equals(UnitType.Rocket)) {
				if (unit.structureIsBuilt() < 1)
					return unit;
			}
		}
		return null;
	}

	public static MapLocation bestKarboniteDeposit(Unit unit) {
		MapLocation ret = new MapLocation(gc.planet(), 0, 0);
		int unitX = unit.location().mapLocation().getX();
		int unitY = unit.location().mapLocation().getY();
		double maxScore = -Double.MAX_VALUE;
		totalKarbonite = 0;
		for (int x = 0; x < karboniteCount.length; x++) {
			for (int y = 0; y < karboniteCount[x].length; y++) {
				double dist = Math.sqrt(Math.pow(x - unitX, 2) + Math.pow(y - unitY, 2));
				double karbonite = karboniteCount[x][y];
				totalKarbonite += karbonite;
				double score = karbonite - dist * dist;
				if (karbonite > 0 && score > maxScore) {
					maxScore = score;
					ret.setX(x);
					ret.setY(y);
				}
			}
		}
		return ret;
	}

	public static Unit getBestRocket(Unit unit) {
		long closestDistance = Long.MAX_VALUE;
		MapLocation me = unit.location().mapLocation();
		VecUnit units = gc.myUnits();
		Unit ret = null;
		for (int i = 0; i < units.size(); i++) {
			Unit u = units.get(i);
			if (u.unitType().equals(UnitType.Rocket)) {
				if (me.distanceSquaredTo(u.location().mapLocation()) < closestDistance) {
					closestDistance = me.distanceSquaredTo(u.location().mapLocation());
					ret = u;
				}
			}
		}
		return ret;
	}
}
