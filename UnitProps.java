import java.util.HashMap;

import bc.Unit;

public class UnitProps {
	public static HashMap<Integer, UnitProps> props = new HashMap<Integer, UnitProps>();
	
	boolean hasBuiltFactory = false;
	Unit factoryToStickTo = null;
}
