package worldMaps;

public class MapReader {

	// returned soil types
	public static final int UNKNOWN = 0;
	public static final int IGNIOUS = 1;
	public static final int METAMORPHIC = 2;
	public static final int SEDIMENTARY = 3;
	public static final int ALUVIAL = 4;
	
	// per map state
	private SleazyJson reader;
	private String filename;
	private String region;
	private double lat;
	private double lon;
	private int height;
	private int width;
	private int tileSize;
	
	// per cell state
	private double altitude[][];
	private double rainfall[][];
	
	public MapReader(String filename) {
		reader = new SleazyJson(filename);		
		this.filename = filename;
	}
	
	public boolean readMap() {
		if (!reader.enter("map_grid")) {
			System.err.println("File " + filename + " does not contain a map_grid");
			return false;
		}
		reader.push();
		
		// read the descriptive properties
		region = reader.property("name");
		if (region == null) {
			System.err.println("ERROR: File " + filename + " does not contain a name");
		}
		
		String s = reader.property("tilesize");
		if (s == null) {
			System.err.println("ERROR: File " + filename + " does not contain a tilesize");
		} else {
			int x = s.indexOf('m');
			if (x != -1)
				s = s.substring(0, x);
			tileSize = new Integer(s);
		}
		
		if (reader.enter("center")) {
			s = reader.property("latitude");
			if (s == null)
				System.err.println("ERROR: File " + filename + " center does not contain a center.latitude");
			else
				lat = new Double(s);
			s = reader.property("longitude");
			if (s == null)
				System.err.println("ERROR: File " + filename + " center does not contain a center.longitude");
			else
				lon = new Double(s);
			reader.pop();
		}
		if (!reader.enter("dimensions")) {
			System.err.println("ERROR: File " + filename + " contains no dimensions");
			return false;
		} else {
			s = reader.property("height");
			if (s == null) {
				System.err.println("FATAL: File " + filename + " does not contain a dimensions.height");
				return false;
			}
			height = new Integer(s);
			s = reader.property("width");
			if (s == null) {
				System.err.println("FATAL: File " + filename + " does not contain a dimensions.width");
				return false;
			}
			width = new Integer(s);
		}
		reader.pop();
		
		// find the data points
		if (!reader.enter("points")) {
			System.err.println("FATAL File " + filename + " contains no points");
			return false;
		}
		
		// allocate and read in the per-cell arrays
		altitude = new double[height][width];
		rainfall = new double[height][width];
		for(int r = 0; r < height; r++)
			for(int c = 0; c < width; c++) {
				if (!reader.nextObject()) {
					System.err.println("FATAL: File " + filename + "ends after row " + r + ", col " + c);
					return false;
				}
				
				s = reader.property("altitude");
				if (s == null) {
					System.err.println("FATAL: File " + filename + "[" + r + "]]" + c + "] contains no altitude");
					return false;
				}
				int x = s.indexOf('m');
				if (x != -1)
					s = s.substring(0, x);
				altitude[r][c] = new Double(s);
				
				s = reader.property("rainfall");
				if (s == null) {
					System.err.println("ERROR: File " + filename + "[" + r + "]]" + c + "] contains no rainfall");
				} else {
					x = s.indexOf("cm");
					if (x != -1)
						s = s.substring(0, x);
					rainfall[r][c] = new Double(s);
				}
				
				s = reader.property("hydration");
				if (s == null) {
					System.err.println("ERROR: File " + filename + "[" + r + "]]" + c + "] contains no hydration");
				} else {
					x = s.indexOf("%");
					if (x != -1)
						s = s.substring(0, x);
					// FIX: implement hydration
				}
				
				s = reader.property("soil");
				if (s == null) {
					System.err.println("ERROR: File " + filename + "[" + r + "]]" + c + "] contains no soil");
				} else {
					// FIX: implement soil
				}
			}
		
		return true;
	}
	
	/**
	 * methods to return scalar map attributes
	 */
	public String name() { return region; }
	public double latitude() { return lat; }
	public double longitude() { return lon; }
	public int height() { return height; }
	public int width() { return width; }
	public int tileSize() {return tileSize; };
	
	/**
	 * methods to return per-cell information
	 */
	public double altitude(int row, int col) {
		return altitude[row][col];
	}
	
	public double rainfall(int row, int col) {
		return rainfall[row][col];
	}
	
	public double slope(int row, int col ) {
		// FIX: implement slope
		return(0);
	}
	
	public int face(int row, int col ) {
		// FIX: implement face
		return(0);
	}
	
	public int hydration(int row, int col) {
		return 50;	// FIX: implement hydration
	}
	
	public int soilType(int row, int col) {
		return UNKNOWN;	// FIX: implement soilTYpe
	}
	public void close() {
		reader.close();
	}
}
