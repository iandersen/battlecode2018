import bc.Direction;
import bc.GameController;
import bc.Unit;
import bc.UnitType;
import bc.VecUnit;
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
		if(Math.random() * 10 < .5){
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
		// TODO Auto-generated method stub
		if(Player.numberOfUnitType(UnitType.Knight) < 20)
			if (unit.structureGarrison().size()!=0 && !UnitPathfinding.firstAvailableUnloadDirection(unit).equals(Direction.Center)) {
				Player.gc.unload(unit.id(), UnitPathfinding.firstAvailableUnloadDirection(unit));
			}
	}

	public static void workerStep(Unit unit) {
		// TODO Auto-generated method stub
		DefaultUnitController.workerStep(unit);
	}

}
