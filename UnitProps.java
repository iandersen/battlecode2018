import java.util.HashMap;

import bc.Direction;
import bc.Unit;

public class UnitProps {
	public static HashMap<Integer, UnitProps> props = new HashMap<Integer, UnitProps>();
	
	boolean hasBuiltFactory = false;
	Unit factoryToStickTo = null;
	int movesInStartDirection = 0;
	int age = 0;
	
	public static UnitProps get(int id){
		if(props.get(id) == null)
			props.put(id, new UnitProps());
		return props.get(id);
	}
}
