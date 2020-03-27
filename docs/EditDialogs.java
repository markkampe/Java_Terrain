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

/**
 * @opt all
 * @depend - - - Map
 */
class WorldDialog  implements ActionListener, ChangeListener, WindowListener {
}

/**
 * @opt all
 * @depend - - - Map
 */
class MountainDialog  implements MapListener, ActionListener, ChangeListener, WindowListener {
	MountainDialog(Map map) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class RainDialog  implements ActionListener, ChangeListener, WindowListener {
	RainDialog(Map map) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class RiverDialog  implements MapListener, ActionListener, ChangeListener, WindowListener {
	RiverDialog(Map map) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class SlopeDialog  implements ActionListener, ChangeListener, WindowListener {
	SlopeDialog(Map map) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class ErosionDialog  implements ActionListener, ChangeListener, WindowListener {
	ErosionDialog(Map map) {};
}

/**
 * @opt all
 * @depend - - - AltitudeMap
 * @depend - - - ErodeMap
 * @depend - - - RainMap
 * @depend - - - RiverMap
 * @depend - - - SoilMap
 * @depend - - - TopoMap
 * @depend - - - WaterMap
 * @depend - - - Cartesian
 * @depend - - - Hydrology
 */
class Map implements MouseListener, MouseMotionListener {

	double map_x(int screen_x) {};	// SlopeDialog only
	double map_y(int screen_y) {};	// SlopeDialog only
	int screen_x(double map_x) {};	// ZoomDialog only
	int screen_y(double map_y) {};	// ZoomDialog only
	boolean on_screen(double x, double y) {};	// RiverMap only

	double[] getHeightMap() {};
	double[] setHeightMap(double newHeight[]) {};
	double[] getRainMap() {};
	double[] setRainMap(double newRain[]) {};
	double[] getSoilMap() {};
	double[] setSoilMap(double newSoil[]) {};
	double[] getFluxMap() {};
	double[] getErodeMap() {};
	double[] getHydrationMap() {};
	int[] getdownHill() {};
	int getErosion() {};
	int setErosion(int cycles) {};
	double[] getIncoming() {};
	void setIncoming() {};

	enum Selection {NONE, POINT, LINE, RECTANGLE, GROUP, ANY};
	void addMapListener(MapListener interested) {};
	void removeMapListener(MapListener which) {};
	void selectMode(Selection type) {};
	boolean checkSelection(Selection type) {};
	void selectLine(int x0, int y0, int x1, int y1) {};
	void selectRect(int x0, int y0, int x1, int y1) {};
	boolean inTheBox(double x, double y) {};

	//soilmap and rainmap only
	static double linear(int min, int max, double value) {};
	static double logarithmic(int min, int max, double value, double base) {};
}


/**
 * @opt all
 */
class AltitudeMap {
	AltitudeMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 * @opt constructor
 * @opt attributes
 * @opt operations
 * @opt types
 * @opt types
 */
class ErodeMap {
	ErodeMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 * @opt constructor
 * @opt attributes
 * @opt operations
 * @opt types
 */
class SoilMap {
	SoilMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 */
class RainMap {
	RainMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 */
class RiverMap {
	RiverMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 */
class TopoMap {
	TopoMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 */
class WaterMap {
	WaterMap(Map map) {};
	void paint(Graphics g, int width, int height, int cellWidth) {};
}

/**
 * @opt all
 * @depend - - - Vicninity
 */
class Cartesian {
	double[][] interpolate(double[] meshValues) {};
}

/**
 * @opt all
 */
class Vicinity {
	void consider(int index, double distance) {};
	double interpolate(double values[]) {};
}

/**
 * @opt all
 */
class Hydrology {
	Hydrology(Map map) {};
	void reCalculate(boolean reset) {};
	static double velocity(double slope) {};
	static double widthToDepth(double velocity) {};
	static double width(double flow, double velocity) {};
	static double depth(double flow, double velocity) {};
}

/** @hidden */ class Color {}
/** @hidden */ class Graphics {}
