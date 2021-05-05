/**
 * this class implements operations to update MeshPoint attributes within
 * rectangular areas
 */
package worldBuilder;

public class AttributeEngine {

	private Map map;
	// private Parameters parms;
	
	private double thisRain[], prevRain[];
	private double thisRock[], prevRock[];
	private double thisFlora[], prevFlora[];
	private double thisFauna[], prevFauna[];
	
	public enum WhichMap {RAIN, MINERAL, FLORA, FAUNA};
	
	public AttributeEngine(Map map) {
		this.map = map;
		// this.parms = Parameters.getInstance();
	}
	
	/**
	 * update the MeshPoint attributes for every point in the box
	 * @param x1	lowest x
	 * @param y1	lowest y
	 * @param x2	highest x
	 * @param y2	highest y
	 * @param whichmap	RAIN, MINERAL, FLORA< FAUNA
	 * @param value	new attribute value
	 */
	public boolean setRegion(double x1, double y1, double x2, double y2, WhichMap whichmap, double value) {
		double thisMap[];
		switch(whichmap) {
		case RAIN:
			if (thisRain == null) {
				thisRain = map.getRainMap();
				prevRain = new double[thisRain.length];
				for(int i = 0; i < thisRain.length; i++)
					prevRain[i] = thisRain[i];
			}
			thisMap = thisRain;
			break;
		case MINERAL:
			if (thisRock== null) {
				thisRock = map.getSoilMap();
				prevRock = new double[thisRock.length];
				for(int i = 0; i < thisRock.length; i++)
					prevRock[i] = thisRock[i];
			}
			thisMap = thisRock;
			break;
		case FLORA:
			if (thisFlora== null) {
				thisFlora = map.getFloraMap();
				prevFlora = new double[thisFlora.length];
				for(int i = 0; i < thisFlora.length; i++)
					prevFlora[i] = thisFlora[i];
			}
			thisMap = thisFlora;
			break;
		case FAUNA:
			if (thisFauna == null) {
				thisFauna = map.getFaunaMap();
				prevFauna = new double[thisFauna.length];
				for(int i = 0; i < thisFauna.length; i++)
					prevFauna[i] = thisFauna[i];
			}
			thisMap = thisFauna;
			break;
		default:
			return(false);
		}
		
		// ensure <x1,y1> and <x2,y2> are in the right order
		if (x2 < x1) {
			double temp = x1;
			x1 = x2;
			x2 = temp;
		}
		if (y2 < y1) {
			double temp = y1;
			y1 = y2;
			y2 = temp;
		}
		
		// set this attribute for every point in the box
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			MeshPoint m = map.mesh.vertices[i];
			if (m.x < x1 || m.x > x2)
				continue;
			if (m.y < y1 || m.y > y2)
				continue;
			thisMap[i] = value;
		}
		
		// tell the map about the changes
		switch(whichmap) {
		case RAIN:
			map.setRainMap(thisRain);
			break;
		case MINERAL:
			map.setSoilMap(thisRock);
			break;
		case FLORA:
			map.setFloraMap(thisFlora);
			break;
		case FAUNA:
			map.setFaunaMap(thisFauna);
			break;
		}
		
		return true;
	}
	
	/**
	 * make the current values the fall-backs (in case of abort)
	 */
	public boolean commit() {
		if (prevRain != null)
			for(int i = 0; i < prevRain.length; i++)
				prevRain[i] = thisRain[i];
		if (prevRock != null)
			for(int i = 0; i < prevRock.length; i++)
				prevRock[i] = thisRock[i];
		if (prevFlora != null)
			for(int i = 0; i < prevRain.length; i++)
				prevFlora[i] = thisFlora[i];
		if (prevFauna != null)
			for(int i = 0; i < prevFauna.length; i++)
				prevFauna[i] = thisFauna[i];
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
		}
		if (prevRock != null) {
			for(int i = 0; i < prevRock.length; i++)
				thisRock[i] = prevRock[i];
			map.setSoilMap(thisRock);
		}
		if (prevFlora != null) {
			for(int i = 0; i < prevRain.length; i++)
				thisFlora[i] = prevFlora[i];
			map.setFloraMap(thisFlora);
		}
		if (prevFauna != null) {
			for(int i = 0; i < prevFauna.length; i++)
				thisFauna[i] = prevFauna[i];
			map.setFaunaMap(thisFauna);
		}
		return true;
	}
}
