import bc.Direction;
import bc.GameController;
import bc.Unit;
import bc.Direction;

public class MarsUnitController {
	private static GameController gc = Player.gc;

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
		if(Math.random() * 10 < 2){
			Direction direction = UnitPathfinding.firstAvailableDirection(unit);
			if(unit.movementHeat() == 0 && gc.canMove(unit.id(), direction))
				gc.moveRobot(unit.id(), direction);
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
		// TODO Auto-generated method stub
		if (unit.structureGarrison().size()!=0 && !UnitPathfinding.firstAvailableUnloadDirection(unit).equals(Direction.Center)) {
			Player.gc.unload(unit.id(), UnitPathfinding.firstAvailableUnloadDirection(unit));
		}
	}

	public static void workerStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.workerStep(unit);
	}

}
