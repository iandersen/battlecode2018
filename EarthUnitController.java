import java.util.HashMap;
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
	private static GameController gc = Player.gc;
	private static final int WORKER_REPLICATE_COST = 15;
	private static final int NUM_WORKERS = 8;
	private static final int NUM_FACTORIES = 5;
	private static final int NUM_ROCKETS = 1;
	private static final int NUM_KNIGHTS = 25;
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
		// TODO Auto-generated method stub
		// if there is something in the garrison, and there is space to unload, unload
		// else if there is available karbonite, choose a robot to create
		int numKnights = Player.numberOfUnitType(UnitType.Ranger);
		if (unit.structureIsBuilt()==1) {
			System.out.println("Garrison size: " + unit.structureGarrison().size());
			System.out.println("Unload direction: " + UnitPathfinding.firstAvailableUnloadDirection(unit));
			if (unit.structureGarrison().size()!=0 && !UnitPathfinding.firstAvailableUnloadDirection(unit).equals(Direction.Center)) {
				gc.unload(unit.id(), UnitPathfinding.firstAvailableUnloadDirection(unit));
			}
			else if (gc.karbonite() > 25 && unit.isFactoryProducing() == 0) {
				// choose a robot to create
				if (Player.numberOfUnitType(UnitType.Worker) < NUM_WORKERS) {
					gc.produceRobot(unit.id(), UnitType.Worker);
				}
				
				else if(numKnights < NUM_KNIGHTS){
					int random = (int)Math.floor(Math.random()*4);
					switch(random) {
					case(3) : gc.produceRobot(unit.id(), UnitType.Ranger); break;
					case(2) : gc.produceRobot(unit.id(), UnitType.Knight); break;
					case(1) : gc.produceRobot(unit.id(), UnitType.Ranger); break;
					}

					
					
				}
			}
		}
	}

	public static void healerStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.healerStep(unit);
	}

	public static void knightStep(Unit unit) {
		if(!unit.location().isOnMap())
			return;
		int numRockets = Player.numberOfUnitType(UnitType.Rocket);
		if(numRockets > 0){
			Unit rocket = getBestRocket(unit);
			if(rocket != null){
				if(gc.canLoad(rocket.id(), unit.id())){
					gc.load(rocket.id(), unit.id());
				}else{
					if(unit.movementHeat() < 10)
						UnitPathfinding.moveUnitTowardLocation(unit, rocket.location().mapLocation());
				}
			}
		}else if(Math.random() * 10 < 2){
				VecUnit enemies = gc.senseNearbyUnits(unit.location().mapLocation(), Math.min(unit.attackRange(), Math.min(unit.visionRange(), unit.abilityRange())));
				boolean attacked = false;
				if(unit.attackHeat() < 10)
					for(int i = 0; i < enemies.size(); i++){
						Unit enemy = enemies.get(i);
						if(!enemy.team().equals(gc.team()))
							if(gc.canAttack(unit.id(), enemy.id())){
								gc.attack(unit.id(), enemy.id());
								attacked = true;
								break;
							}
					}
				if(!attacked){
					Direction direction = UnitPathfinding.firstAvailableDirection(unit);
					//System.out.println("knight wants to move in direction: " + direction);
					if(unit.movementHeat() == 0 && gc.canMove(unit.id(), direction))
						gc.moveRobot(unit.id(), direction);
				}
			}
	}

	public static void mageStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.mageStep(unit);
	}

	public static void rangerStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.rangerStep(unit);
	}

	public static void rocketStep(Unit unit) {
		if (unit.structureIsBuilt()==1) {
			PlanetMap map = gc.startingMap(Planet.Mars);
			int x = (int)Math.floor(Math.random()*map.getWidth());
			int y = (int)Math.floor(Math.random()*map.getHeight());
			
			if(unit.structureGarrison().size() == 8) {
				// best to find an available spot
				while (map.isPassableTerrainAt(new MapLocation(Planet.Mars,x,y)) != 1) {
					x = (int)Math.floor(Math.random()*map.getWidth());
					y = (int)Math.floor(Math.random()*map.getHeight());
					
				}
				if(gc.canLaunchRocket(unit.id(), new MapLocation(Planet.Mars,x,y)))
					gc.launchRocket(unit.id(), new MapLocation(Planet.Mars,x,y));
			}
			if (unit.health() < 50) {
				while (map.isPassableTerrainAt(new MapLocation(Planet.Mars,x,y)) != 1) {
					x = (int)Math.floor(Math.random()*map.getWidth());
					y = (int)Math.floor(Math.random()*map.getHeight());
					
				}
				if(gc.canLaunchRocket(unit.id(), new MapLocation(Planet.Mars,x,y)))
					gc.launchRocket(unit.id(), new MapLocation(Planet.Mars,x,y));
			}
	}
	
	}

	public static void workerStep(Unit unit) {
		int numWorkers = Player.numberOfUnitType(UnitType.Worker);
		int numFactories = Player.numberOfUnitType(UnitType.Factory);
		int numRockets = Player.numberOfUnitType(UnitType.Rocket);
		Unit structure = getUnfinishedStructure();
		if(!unit.location().isOnMap())
			return;
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
				&& !UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket).equals(Direction.Center)) {
			System.out.println("Blueprinting rocket");
			workerBlueprintRocket(unit);
		} else if (totalKarbonite > 0) {
			workerMine(unit);
		}
	}

	private static void workerReplicate(Unit unit) {
		Direction firstAvailableDirection = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Factory);
		if(!firstAvailableDirection.equals(Direction.Center))
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
			System.out.println("Worker failed to act");
		}
	}

	private static void workerBlueprintFactory(Unit unit) {
		Direction direction = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Factory);
		gc.blueprint(unit.id(), UnitType.Factory, direction);
	}
	
	private static void workerBlueprintRocket(Unit unit) {
		Direction direction = UnitPathfinding.firstAvailableBuildDirection(unit, UnitType.Rocket);
		gc.blueprint(unit.id(), UnitType.Rocket, direction);
	}

	private static void workerMine(Unit unit) {
		MapLocation deposit = bestKarboniteDeposit(unit);
		for (Direction dir : Direction.values()) {
			if (gc.canHarvest(unit.id(), dir)) {
				gc.harvest(unit.id(), dir);
				karboniteCount[deposit.getX()][deposit.getY()] -= Math.min(unit.workerHarvestAmount(),
						karboniteCount[deposit.getX()][deposit.getY()]);
				return;
			}
		}
		if (unit.movementHeat() < 10 && deposit.distanceSquaredTo(unit.location().mapLocation()) > 2) {
			UnitPathfinding.moveUnitTowardLocation(unit, deposit);
			return;
		}
		if (gc.canHarvest(unit.id(), unit.location().mapLocation().directionTo(deposit))) {
			gc.harvest(unit.id(), unit.location().mapLocation().directionTo(deposit));
			karboniteCount[deposit.getX()][deposit.getY()] -= unit.workerHarvestAmount();
			return;
		}
		if (deposit.distanceSquaredTo(unit.location().mapLocation()) <= 2) {
			karboniteCount[deposit.getX()][deposit.getY()] = 0;
		}
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
			if (u.unitType().equals(UnitType.Rocket)){
				if(me.distanceSquaredTo(u.location().mapLocation()) < closestDistance){
					closestDistance = me.distanceSquaredTo(u.location().mapLocation());
					ret = u;
				}
			}
		}
		return ret;
	}
}
