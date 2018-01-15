import bc.Direction;
import bc.GameController;
import bc.Unit;

public class DefaultUnitController {
	public static GameController gc = Player.gc;
	public static void factoryStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

	public static void healerStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

	public static void knightStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

	public static void mageStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

	public static void rangerStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

	public static void rocketStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

	public static void workerStep(Unit unit) {
		if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
			gc.moveRobot(unit.id(), Direction.Southeast);
		}
	}

}
