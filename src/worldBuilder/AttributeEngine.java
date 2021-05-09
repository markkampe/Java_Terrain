/**
 * this class implements operations to update MeshPoint attributes within
 * rectangular areas
 */
package worldBuilder;

public class AttributeEngine {

	private Map map;
	private Parameters parms;
	
	// current and last committed attribute maps
	private double thisRain[], prevRain[];
	private double thisRock[], prevRock[];
	private double thisFlora[], prevFlora[];
	private double thisFauna[], prevFauna[];
	
	// auto placement engines
	private Placement rockPlacer, floraPlacer, faunaPlacer;
	private String[] rockClasses, floraClasses, faunaClasses;

	// changes since the last commit
	boolean newRain, newRock, newFlora, newFauna;
	private int adjusted;
	
	private static final int ATTRIBUTE_DEBUG = 2;
	
	public enum WhichMap {RAIN, MINERAL, FLORA, FAUNA};
	private static final int AUTO_PLACEMENT = -666;
	
	public AttributeEngine(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * make sure we have a copy of the previously committed map
	 * @param whichmap attribute map to be updated
	 * @return WIP map for specified attributes
	 */
	private double[] getMap(WhichMap whichmap) {
		switch(whichmap) {
		case RAIN:
			if (thisRain == null) {
				thisRain = map.getRainMap();
				prevRain = new double[thisRain.length];
				for(int i = 0; i < thisRain.length; i++)
					prevRain[i] = thisRain[i];
			}
			return thisRain;
		case MINERAL:
			if (thisRock== null) {
				thisRock = map.getSoilMap();
				prevRock = new double[thisRock.length];
				for(int i = 0; i < thisRock.length; i++)
					prevRock[i] = thisRock[i];
			}
			return thisRock;
		case FLORA:
			if (thisFlora== null) {
				thisFlora = map.getFloraMap();
				prevFlora = new double[thisFlora.length];
				for(int i = 0; i < thisFlora.length; i++)
					prevFlora[i] = thisFlora[i];
			}
			return thisFlora;
		case FAUNA:
			if (thisFauna == null) {
				thisFauna = map.getFaunaMap();
				prevFauna = new double[thisFauna.length];
				for(int i = 0; i < thisFauna.length; i++)
					prevFauna[i] = thisFauna[i];
			}
			return thisFauna;
		default:
			return null;
		}
	}
	
	/**
	 * push the updated attributes back to the active map
	 * @param whichmap attribute map to be pushed
	 * @param value stored
	 */
	private void putMap(WhichMap whichmap, double value) {
		String mapName = "???";
		switch(whichmap) {
		case RAIN:
			map.setRainMap(thisRain);
			newRain = (adjusted > 0);
			mapName = "rainfall";
			break;
		case MINERAL:
			map.setSoilMap(thisRock);
			newRock = (adjusted > 0);
			mapName = "mineral distribution";
			break;
		case FLORA:
			map.setFloraMap(thisFlora);
			newFlora = (adjusted > 0);
			mapName = "floral ecotope assignment";
			break;
		case FAUNA:
			map.setFaunaMap(thisFauna);
			newFauna = (adjusted > 0);
			mapName = "fauna distribution";
			break;
		}
		if (parms.debug_level >= ATTRIBUTE_DEBUG) {
			if (value == AUTO_PLACEMENT)
				System.out.println(String.format("Auto-placement for %d points of %s", this.adjusted, mapName));
			else
				System.out.println(String.format("Updated %s for %d points to %f", mapName, this.adjusted, value));
		}
	}
	
	/**
	 * update the MeshPoint attributes for every selected
	 * @param selected array of per-MeshPoint booleans, True->selected
	 * @param whichmap	RAIN, MINERAL, FLORA< FAUNA
	 * @param value	new attribute value
	 */
	public boolean placement(boolean[] selected, WhichMap whichmap, double value) {
		double[] thisMap = getMap(whichmap);
		
		// set this attribute for every point in the box
		this.adjusted = 0;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			MeshPoint m = map.mesh.vertices[i];
			if (selected[m.index]) {
				thisMap[i] = value;
				this.adjusted++;
			}
		}
		
		// tell the map about the changes
		putMap(whichmap, value);
		return true;
	}
	
	/**
	 * instantiate an auto-placement engine for the desired resource
	 * @param ruleFile name of file containing placement rules
	 * @param classNames names of the placement classes
	 * @param whichmap MINERAL, FLORA, FAUNA
	 */
	public boolean placementRules(String ruleFile, String[] classNames, WhichMap whichmap) {
		double[] thisMap = getMap(whichmap);
		switch(whichmap) {
		case MINERAL:
			rockPlacer = new Placement(ruleFile, map, thisMap);
			rockClasses = classNames;
			return true;
		case FLORA:
			floraPlacer = new Placement(ruleFile, map, thisMap);
			floraClasses = classNames;
			return true;
		case FAUNA:
			faunaPlacer = new Placement(ruleFile, map, thisMap);
			faunaClasses = classNames;
			return true;
		default:
			return false;
		}
	}
	
	private static final String[] None = {"NONE", "NONE", "NONE", "NONE"};
	/**
	 * rule based resource placement for the selected points
	 * @param selected per-point booleans for selected points
	 * @param quotas fraction of points desired per class
	 * @param whichmap MINERAL, FLORA, FAUNA
	 */
	public boolean autoPlacement(boolean[] selected, double[] quotas, WhichMap whichmap) {
		// empty and count the selected points
		double[] thisMap = getMap(whichmap);
		int num_points = 0;
		for(int i = 0; i < selected.length; i++)
			if (selected[i]) {
				thisMap[i] = 0;
				num_points++;
			}

		// compute the per-class quotas
		int[] perClass = new int[quotas.length];
		for(int i = 0; i < quotas.length; i++)
			perClass[i] = (int) (quotas[i] * num_points);
		
		// run the placement engine
		String[] classNames = None;
		int[] classCounts;
		switch(whichmap) {
		case MINERAL:
			if (rockPlacer == null)
				return false;
			classNames = rockClasses;
			classCounts = rockPlacer.update(selected,  perClass,  classNames);
			newRock = true;
			break;
		case FLORA:
			if (floraPlacer == null)
				return false;
			classNames = floraClasses;
			classCounts = floraPlacer.update(selected,  perClass,  classNames);
			newFlora = true;
			break;
		case FAUNA:
			if (faunaPlacer == null)
				return false;
			classNames = faunaClasses;
			classCounts = faunaPlacer.update(selected,  perClass,  classNames);
			newFauna = true;
			break;
		default:
			return false;
		}
		
		// and up date the display
		putMap(whichmap, AUTO_PLACEMENT);
		
		// report on how many points assigned to each class
		if (parms.debug_level >= ATTRIBUTE_DEBUG) {
			String names = "";
			String counts = "";
			for(int i = 0; i < classNames.length; i++) {
				if (i > 0) {
					names += "/";
					counts += "/";
				}
				names += classNames[i];
				counts += String.format("%d", classCounts[i]);
			}
			System.out.println(names + " = " + counts);
		}
		
		return true;
	}

	/**
	 * make the current values the fall-backs (in case of abort)
	 */
	public boolean commit() {
		if (prevRain != null) {
			for(int i = 0; i < prevRain.length; i++)
				prevRain[i] = thisRain[i];
			if (newRain && parms.debug_level > 0)
				System.out.println(String.format("Updated rain map for %d points", this.adjusted));
			newRain = false;
		}
		if (prevRock != null) {
			for(int i = 0; i < prevRock.length; i++)
				prevRock[i] = thisRock[i];
			if (newRock && parms.debug_level > 0)
				System.out.println(String.format("Updated mineral types for %d points", this.adjusted));
			newRock = false;
		}
		if (prevFlora != null) {
			for(int i = 0; i < prevFlora.length; i++)
				prevFlora[i] = thisFlora[i];
			if (newFlora && parms.debug_level > 0)
				System.out.println(String.format("Updated flora ecotopes for %d points", this.adjusted));
			newFlora = false;
		}
		if (prevFauna != null) {
			for(int i = 0; i < prevFauna.length; i++)
				prevFauna[i] = thisFauna[i];
			if (newFauna && parms.debug_level > 0)
				System.out.println(String.format("Updated fauna distribution for %d points", this.adjusted));
			newFauna = false;
		}
		adjusted = 0;
		return true;
	}
	
	/**
	 * fall back to the last committed values (only for changed maps)
	 */
	public boolean abort() {
		if (prevRain != null) {
			for(int i = 0; i < prevRain.length; i++)
				thisRain[i] = prevRain[i];
			map.setRainMap(thisRain);
			newRain = false;
		}
		if (prevRock != null) {
			for(int i = 0; i < prevRock.length; i++)
				thisRock[i] = prevRock[i];
			map.setSoilMap(thisRock);
			// XXX restore previous-ruleset preview colors?
			newRock = false;
			
		}
		if (prevFlora != null) {
			for(int i = 0; i < prevFlora.length; i++)
				thisFlora[i] = prevFlora[i];
			map.setFloraMap(thisFlora);
			// XXX restore previous-ruleset preview colors?
			newFlora = false;
		}
		if (prevFauna != null) {
			for(int i = 0; i < prevFauna.length; i++)
				thisFauna[i] = prevFauna[i];
			map.setFaunaMap(thisFauna);
			// XXX restore previous-ruleset preview colors?
			newFauna = false;
		}
		adjusted = 0;
		return true;
	}
}
