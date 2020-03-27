class JPanel {}
class JFrame {}
/** @opt operations */ interface ActionListener { void actionPerformed() {}; }
/** @opt operations */ interface ChangeListener { void stateChanged() {}; }
/** @opt operations */ interface WindowListener { void windowClosing() {}; }
/** @opt operations */ interface MouseMotionListener { void mouseDragged() {}; }
/** @opt operations */ interface MouseListener { void mousePressed() {}; 
											     void mouseClicked() {}; 
											     void mouseReleased() {}; }
/** * @opt operations */ interface MapListener {
	boolean regionSelected(double x0, double y0, double width, double height, boolean complete) {};
	boolean pointSelected(double x, double y) {};
}


/** @opt all */
interface Exporter {
	void tileSize(int meters) {};
	void temps(double meanTemp, double meanSummer, double meanWinter) {};
	void heigthMap(double[][] heights) {};
	void erodeMap(double[][] erode) {};
	void rainMap(double[][] rain) {};
	void soilMap(double[][] soil) {};
	void waterMap(double[][] hydration) {};
	boolean writeFile(String filename) {};
	enum WhichMap {};
	void preview(WhichMap chosen, Color colorMap[]) {};
}

/** @opt all */
class JsonExporter implements Exporter {
	JsonExporter(int width, int height) {};
}

/**
 * @opt all
 * @depend - - - Bidder
 * @depend - - - RPGMwriter
 * @depend - - - TileRules
 * @depend - - - PreviewMap
 */
class RPGMTiler implements Exporter {
	RPGMTiler(String rulesFile, int width, int height) {};
}

/** 
 * @opt all
 * @depend - - - Map
 * @depend - - - Cartesian
 */
class ExportBase extends JFrame implements WindowListener, MapListener {
	ExportBase(String format, Map map) {};
	protected void export(Exporter export) {};
	protected void add_rivers(double[][] hydration, int tilesize) {};
}

/** 
 * @opt all
 * @depend - - - JsonExporter
 */
class RawExport extends ExportBase implements ActionListener {
	RawExport(Map map) {};
}

/** 
 * @opt all
 * @depend - - - RPGMLeveler
 * @depend - - - RPGMFlora
 * @depend - - - RPGMTiler
 * @depend - - - TerrainType
 */
class RPGMexport extends ExportBase implements ActionListener, ChangeListener{
	RPGMexport(String format, Map map) {};
	void hydroMap() {};
	void florMap() {};
}

/** @opt all */
class RPGMFlora {
	RPGMFlora(RPGMTiler tiler, String paletteFile) {};
	String[] getFloraNames() {};
	Color[] getFloraColors() {};
	int getFlora(String[] classes, int[] quotas) {};
}

/** @opt all */
class RPGMLeveler {
	int[][] getLevels(RPGMTiler tiler, int[] altMap, int[] waterMap, int slopeMap[]){};
}

/** @opt all */
class Bidder {
	Bidder(int maxBidders) {};
	boolean bid(int bidder, double bid) {};
	int winner(double value) {};
	void reset() {};
}

/**
 * @opt all
 * @depend - - - TerrainType
 */
class RPGMwriter {
	RPGMwriter(FileWriter outfile, TileRules rules) {};
	void prologue(int height, int width, int tileset) {};
	void writeTable(int[][] tiles, boolean last) {};
	void writeAdjustedTable(int[][] baeTiles, int[][] levels) {};
}

/** 
 * @opt all
 * @depend - - - TileRule
 */
class TileRules {
	LinkedList  rules;

	TileRules(String rulesfile) {};
	int neighbors(int base) {};
	boolean landBarrier(int base) {};
}

/** 
 * @opt all
 * @depend - - - TerrainType
 */
class TileRule {
	TileRule(String name, int tile_set, int level, int base) {};
	double range_bid(double value, double min, double max) {};
	double bid(double alt, double hydro, double winter, double summer, double soil, double slope, double direction) {};
}

/** @opt all */
class PreviewMap {
	PreviewMap(String name, Color array[][]) {};
	void paint(Graphics g) {};
}

/**
 * @opt constructor
 * @opt attributes
 * @opt operations
 * @opt types
 */
class Map extends JPanel implements MouseListener, MouseMotionListener {
	double[] getHeightMap() {};
	double[] getRainMap() {};
	double[] getSoilMap() {};
	double[] getFluxMap() {};
	double[] getErodeMap() {};
	double[] getHydrationMap() {};
	int[] getdownHill() {};
	int getErosion() {};

	enum Selection {NONE, POINT, LINE, RECTANGLE, GROUP, ANY};
	void addMapListener(MapListener interested) {};
	void removeMapListener(MapListener which) {};
	void selectMode(Selection type) {};
	boolean checkSelection(Selection type) {};
	void selectRect(int x0, int y0, int x1, int y1) {};
}

/**
 * @opt operations
 * @opt types
 */
class Cartesian {
	double[][] interpolate(double[] meshValues) {};
}


/** @hidden */ class Graphics {}
/** @hidden */ class Color {}
/** @hidden */ class LinkedList {}
/** @hidden */ class FileWriter {}
