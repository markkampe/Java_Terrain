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

	// changes since the last commit
	boolean newRain, newRock, newFlora, newFauna;
	private int adjusted;
	
	private static final int ATTRIBUTE_DEBUG = 2;
	
	public enum WhichMap {RAIN, MINERAL, FLORA, FAUNA};
	
	public AttributeEngine(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * update the MeshPoint attributes for every selected
	 * @param selected array of per-MeshPoint booleans, True->selected
	 * @param whichmap	RAIN, MINERAL, FLORA< FAUNA
	 * @param value	new attribute value
	 */
	public boolean setRegion(boolean[] selected, WhichMap whichmap, double value) {
		double thisMap[];
		String mapName = null;
		switch(whichmap) {
		case RAIN:
			if (thisRain == null) {
				thisRain = map.getRainMap();
				prevRain = new double[thisRain.length];
				for(int i = 0; i < thisRain.length; i++)
					prevRain[i] = thisRain[i];
			}
			thisMap = thisRain;
			mapName = "rainfall";
			break;
		case MINERAL:
			if (thisRock== null) {
				thisRock = map.getSoilMap();
				prevRock = new double[thisRock.length];
				for(int i = 0; i < thisRock.length; i++)
					prevRock[i] = thisRock[i];
			}
			thisMap = thisRock;
			mapName = "mineral";
			break;
		case FLORA:
			if (thisFlora== null) {
				thisFlora = map.getFloraMap();
				prevFlora = new double[thisFlora.length];
				for(int i = 0; i < thisFlora.length; i++)
					prevFlora[i] = thisFlora[i];
			}
			thisMap = thisFlora;
			mapName = "flora";
			break;
		case FAUNA:
			if (thisFauna == null) {
				thisFauna = map.getFaunaMap();
				prevFauna = new double[thisFauna.length];
				for(int i = 0; i < thisFauna.length; i++)
					prevFauna[i] = thisFauna[i];
			}
			thisMap = thisFauna;
			mapName = "fauna";
			break;
		default:
			return(false);
		}
		
		// set this attribute for every point in the box
		this.adjusted = 0;
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			MeshPoint m = map.mesh.vertices[i];
			if (selected[m.index]) {
				thisMap[i] = value;
				this.adjusted++;
			}
		}
		
		if (parms.debug_level >= ATTRIBUTE_DEBUG) {
			System.out.println(String.format("Updated %s for %d points to %f", mapName, this.adjusted, value));
		}
		
		// tell the map about the changes
		switch(whichmap) {
		case RAIN:
			map.setRainMap(thisRain);
			newRain = adjusted > 0;
			break;
		case MINERAL:
			map.setSoilMap(thisRock);
			newRock = adjusted > 0;
			break;
		case FLORA:
			map.setFloraMap(thisFlora);
			newFlora = adjusted > 0;
			break;
		case FAUNA:
			map.setFaunaMap(thisFauna);
			newFauna = adjusted > 0;
			break;
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
			newRock = false;
		}
		if (prevFlora != null) {
			for(int i = 0; i < prevRain.length; i++)
				thisFlora[i] = prevFlora[i];
			map.setFloraMap(thisFlora);
			newFlora = false;
		}
		if (prevFauna != null) {
			for(int i = 0; i < prevFauna.length; i++)
				thisFauna[i] = prevFauna[i];
			map.setFaunaMap(thisFauna);
			newFauna = false;
		}
		adjusted = 0;
		return true;
	}
}
