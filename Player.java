
// import the API.
// See xxx for the javadocs.
import java.util.Stack;

import bc.*;

public class Player {
	public static GameController gc;
	static Stack<UnitType> researchTrees = new Stack<UnitType>();

	public static void main(String[] args) {

		// MapLocation is a data structure you'll use a lot.
		MapLocation loc = new MapLocation(Planet.Earth, 10, 20);
		System.out.println("loc: " + loc + ", one step to the Northwest: " + loc.add(Direction.Northwest));
		System.out.println("loc x: " + loc.getX());

		// One slightly weird thing: some methods are currently static methods
		// on a static class called bc.
		// This will eventually be fixed :/
		System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));

		// Connect to the manager, starting the game
		gc = new GameController();
		researchTrees.push(UnitType.Rocket);
		researchTrees.push(UnitType.Knight);
		researchTrees.push(UnitType.Knight);
		researchTrees.push(UnitType.Knight);
		researchTrees.push(UnitType.Rocket);
		
		while (true) {
			EarthUnitController.init();
			UnitPathfinding.updateMap();
			System.out.println("Current round: " + gc.round() + " time left: " + gc.getTimeLeftMs());
			if (gc.round() % 5 == 0) { System.gc(); }
			if (gc.planet().equals(Planet.Earth))
				earthTurn();
			else
				marsTurn();
			gc.nextTurn();
		}
	}

	public static void earthTurn() {
		VecUnit units = gc.myUnits();
		
		ResearchInfo info = gc.researchInfo();
		if(info.queue().size() == 0){
			gc.queueResearch(UnitType.Rocket);
		}
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			switch (unit.unitType()) {
			case Factory:
				EarthUnitController.factoryStep(unit);
				break;
			case Healer:
				EarthUnitController.healerStep(unit);
				break;
			case Knight:
				EarthUnitController.knightStep(unit);
				break;
			case Mage:
				EarthUnitController.mageStep(unit);
				break;
			case Ranger:
				EarthUnitController.rangerStep(unit);
				break;
			case Rocket:
				EarthUnitController.rocketStep(unit);
				break;
			case Worker:
				EarthUnitController.workerStep(unit);
				break;
			}
		}
	}

	public static void marsTurn() {
		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			switch (unit.unitType()) {
			case Factory:
				MarsUnitController.factoryStep(unit);
				break;
			case Healer:
				MarsUnitController.healerStep(unit);
				break;
			case Knight:
				MarsUnitController.knightStep(unit);
				break;
			case Mage:
				MarsUnitController.mageStep(unit);
				break;
			case Ranger:
				MarsUnitController.rangerStep(unit);
				break;
			case Rocket:
				MarsUnitController.rocketStep(unit);
				break;
			case Worker:
				MarsUnitController.workerStep(unit);
				break;
			}
		}
	}

	public static int numberOfUnitType(UnitType type) {
		int count = 0;
		VecUnit units = gc.myUnits();
		for (int i = 0; i < units.size(); i++) {
			Unit unit = units.get(i);
			if (unit.unitType().equals(type))
				count++;
		}
		return count;
	}
}