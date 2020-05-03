/** @opt operations */ interface ActionListener { void actionPerformed() {}; }
/** @opt operations */ interface WindowListener { void windowClosing() {}; }
/** * @opt operations */ interface MapListener {
	boolean regionSelected(double x0, double y0, double width, double height, boolean complete) {};
	boolean pointSelected(double x, double y) {};
}
class JFrame {}
class JPanel {}

/**
 * @opt constructor
 * @opt attributes
 * @opt operations
 * @opt types
 * @composed - Has - Map
 * @depend - - - Parameters
 */
class WorldBuilder extends JFrame implements WindowListener {
	Map map;

	WorldBuilder(String filename) {};
	static void main(String[] args) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class MeshDialog  implements ActionListener, WindowListener {
	MeshDialog(Map map) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class RegionDialog  implements MapListener, ActionListener, WindowListener {
	RegionDialog(Map map) {};
}

/**
 * @opt constructor
 * @opt attributes
 * @opt operations
 * @opt types
 * @opt enumconstants
 * @composed - Has - Mesh
 * @depend - - - Parameters
 * @depend - - - Vicinity
 */
class Map extends JPanel {
	Mesh mesh;
	boolean isSubRegion;

	Map(int width, int height) {};
	void read(String filename) {};
	boolean write(String filename) {};

	Mesh getMesh() {};
	void setMesh(Mesh mesh) {};

	void subRegion(int num_points);

	void paintComponent(Graphics g) {};
}

/**
 * @opt attributes
 * @opt operations
 * @opt types
 * @composed 1..* Has - MeshPoint
 */
 class Mesh {
	MeshPoint[] vertices;

	MeshPoint[] makePoints(int numpoints) {};
	void makeMesh( MeshPoint[] points ) {};
	void read(String filename) {};
	MeshPoint choosePoint(double x, double y) {};
}

/**
 * @opt constructor
 * @opt attributes
 * @opt operations
 * @opt types
 * @depend - - - MeshPointHasher
 */
class MeshPoint {
	double x;
	double y;
	int neighbors;
	MeshPoint[] neighbor;

	MeshPoint(double x, double y) {};
	MeshPoint(double x, double y, int index) {};
	void addNeighbor(MeshPoint p) {};
	boolean isNeighbor(MeshPoint p) {};
	double distance(MeshPoint other) {};
	static void quickSort(MeshPoint[] array, int left, int right) {};
}

/**
 * @opt constructor
 * @opt operations
 * @opt types
 */
class MeshPointHasher {
	MeshPointHasher(int vax_vertices, double x_extent, double y_extent) {};
	MeshPoint findPoint(double x, double y) {};
}

/**
 * @opt all
 */
class Vicinity {
	Vicinity(Mesh mesh, double x, double y} {};
	double interpolate(double values[]) {};
}

class Parameters {}
/** @hidden */
class Graphics {}
/** @hidden */
class Color {}
