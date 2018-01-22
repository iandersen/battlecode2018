import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bc.GameController;
import bc.MapLocation;
import bc.Unit;
import bc.Direction;
import bc.Planet;
import bc.PlanetMap;
import bc.UnitType;
import bc.VecUnit;

public class EarthUnitController extends DefaultUnitController {
	private static int[] createRobotList = { 2, 2, 3, 3, 3, 4, 5 };
	private static HashMap<Integer, Integer> factoryList = new HashMap<>();
	private static GameController gc = Player.gc;
	private static final int WORKER_REPLICATE_COST = 15;
	private static final int NUM_WORKERS = 8;
	private static final int NUM_FACTORIES = 100;
	private static final int NUM_ROCKETS = 1;
	private static final int NUM_KNIGHTS = 300;
	private static final int MINE_TURNS = 50;
	private static final int KARBONITE_MIN = 300;
	static Map<Integer, UnitProps> allUnitProps = new HashMap<Integer, UnitProps>();
	static long[][] karboniteCount;
	static boolean initialized = false;
	private static int totalKarbonite = 1000;

	public static void init() {
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

	public static void factoryStep(Unit unit) {

		int numKnights = Player.numberOfUnitType(UnitType.Knight);
		if (unit.structureIsBuilt() == 1) {
			// System.out.println("Garrison size: " +
			// unit.structureGarrison().size());
			// System.out.println("Unload direction: " +
			// UnitPathfinding.firstAvailableUnloadDirection(unit));
			if (unit.structureGarrison().size() != 0
					&& !UnitPathfinding.firstAvailableUnloadDirection(unit).equals(Direction.Center)) {
				gc.unload(unit.id(), UnitPathfinding.firstAvailableUnloadDirection(unit));
			} else if (gc.karbonite() > 25 && unit.isFactoryProducing() == 0) {
				// choose a robot to create
				if (Player.numberOfUnitType(UnitType.Worker) < NUM_WORKERS) {
					gc.produceRobot(unit.id(), UnitType.Worker);
				}

				else {
					if (factoryList.get(unit.id()) == createRobotList.length - 2) {
						factoryList.put(unit.id(), -1);
					}
					factoryList.put(unit.id(), factoryList.get(unit.id()) + 1);
					switch (createRobotList[factoryList.get(unit.id())]) {
					case (2):
						gc.produceRobot(unit.id(), UnitType.Knight);
						break;
					case (3):
						gc.produceRobot(unit.id(), UnitType.Ranger);
						break;
					case (4):
						gc.produceRobot(unit.id(), UnitType.Mage);
						break;
					case (5):
						gc.produceRobot(unit.id(), UnitType.Healer);
						break;
					}

				}
			}
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
				Direction[] ds = { Direction.Northwest, Direction.Northeast, Direction.Southeast, Direction.Southwest };
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

	public static void healerStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		meshStep(unit);
	}

	public static void knightStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		meshStep(unit);
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
	}

	public static void mageStep(Unit unit) {
		if (!unit.location().isOnMap())
			return;
		meshStep(unit);
		/*
		int earthWidth = (int)gc.startingMap(Planet.Earth).getWidth();
		int earthHeight = (int)gc.startingMap(Planet.Earth).getHeight();
		for(int i = unit.location().mapLocation().getX(); i > 0 && i < earthWidth; i++) {
			for (int j = unit.location().mapLocation().getY(); j > 0 && j < earthHeight;j++) {
				
			}
		}
		*/
		ArrayList<MapLocation> list = new ArrayList<>();
		VecUnit enemies = gc.senseNearbyUnits(unit.location().mapLocation(),
				(long) Math.floor(Math.sqrt(unit.attackRange())));
		if (unit.attackHeat() < 10) {
			for (int i = 0; i < enemies.size(); i++) {
				Unit enemy = enemies.get(i);
				if (!enemy.team().equals(gc.team()))
					if (gc.canAttack(unit.id(), enemy.id())) {
						// keep track
						list.add(enemy.location().mapLocation());
					}
			}
			gc.attack(unit.id(), gc.senseUnitAtLocation(nearby(list)).id());
		}
		
	}
	
	public static MapLocation nearby(ArrayList<MapLocation> list) {
		// return the best square
		int[] cation = new int[list.size()];
		for (int number : cation) {
			number = 0;
		}
		int i = 0;
		for(MapLocation place : list) {
			for(MapLocation other : list) {
				if (place.getX() == other.getX() && place.getY() == other.getY() + 1 
						|| place.getX() == other.getX() && place.getY() == other.getY() - 1
						|| place.getX() == other.getX() + 1 && place.getY() == other.getY()
						|| place.getX() == other.getX() - 1 && place.getY() == other.getY()
						|| place.getX() == other.getX() + 1 && place.getY() == other.getY() - 1
						|| place.getX() == other.getX() + 1 && place.getY() == other.getY() + 1
						|| place.getX() == other.getX() - 1 && place.getY() == other.getY() - 1
						|| place.getX() == other.getX() - 1 && place.getY() == other.getY() + 1) {
					cation[i]++;
				}
				i++;
			}
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
			if (unit.health() < 50) {
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
		int numWorkers = Player.numberOfUnitType(UnitType.Worker);
		int numFactories = Player.numberOfUnitType(UnitType.Factory);
		int numRockets = Player.numberOfUnitType(UnitType.Rocket);
		boolean stayByFactory = !((UnitProps.props.get(unit.id()) == null
				|| !UnitProps.props.get(unit.id()).hasBuiltFactory) && numFactories < NUM_FACTORIES);
		Unit structure = getUnfinishedStructure();
		if (!unit.location().isOnMap())
			return;
		MapLocation loc = unit.location().mapLocation();
		stayByFactory = false;
		if (stayByFactory) {
			// Unit factoryToStickTo =
			// UnitProps.props.get(unit.id()).factoryToStickTo;
			// if(factoryToStickTo != null){
			// Direction d =
			// UnitPathfinding.firstAvailableDiagDirection(factoryToStickTo);
			// MapLocation newLoc =
			// factoryToStickTo.location().mapLocation().add(d);
			// if(loc.distanceSquaredTo(newLoc) > 0){
			// if(unit.movementHeat() < 10)
			// gc.moveRobot(unit.id(), loc.directionTo(newLoc));
			// }
			// }
		} else if (unit.abilityHeat() < 10 && gc.karbonite() >= WORKER_REPLICATE_COST && numWorkers < NUM_WORKERS
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
				&& !UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket).equals(Direction.Center)) {
			workerBlueprintRocket(unit);
		} else if (totalKarbonite > 0) {
			workerMine(unit);
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
			gc.build(unit.id(), id);
		} else if (unit.movementHeat() < 10) {
			UnitPathfinding.moveUnitTowardLocation(unit, structure.location().mapLocation());
		} else {
			// System.out.println("Worker failed to act");
		}
	}

	private static void workerBlueprintFactory(Unit unit) {
		int numFactories = Player.numberOfUnitType(UnitType.Factory);
		MapLocation unitLocation = unit.location().mapLocation();
		if (numFactories == 0) {
			Direction direction = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Factory);
			gc.blueprint(unit.id(), UnitType.Factory, direction);
		} else {
			HashMap<Integer, Unit> factories = getAllUnitsByType(UnitType.Factory);
			List<Integer> orderedKeys = new ArrayList<Integer>();
			for (int i : factories.keySet())
				orderedKeys.add(i);
			Collections.sort(orderedKeys);
			outer: for (int i : orderedKeys) {
				Unit factory = factories.get(i);
				MapLocation source = factory.location().mapLocation();
				Direction[] buildDirections = { Direction.Northwest, Direction.Northeast, Direction.Southeast,
						Direction.Southwest };
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
