/**
 * This class is used to generate a single unique MapPoint for 
 * each unique coordinate pair, and find the already allocated 
 * point when coordinates are repeated.
 * 
 * I have chosen to do this with an open hash table for time
 * efficiency.
 */
public class MapPointHasher {
	public MapPoint[] vertices;	// list of known vertices
	public int numVertices;		// number of vertices in list
	private int tableSize;		// total size of hash table
	private int[] hashTable;	// index known vertices (or -1)
	private double x_extent;	// needed to normalize hash values
	private double y_extent;	// needed to normalize hash values

	/**
	 * allocate a hash table and vertex list
	 * 
	 * @param max ... max # of vertices
	 * @param x_extent ... x diameter
	 * @param y_extent ... y_diameter
	 */
	public MapPointHasher(int max, double x_extent, double y_extent) {
		vertices = new MapPoint[max];
		numVertices = 0;
		tableSize = max * 3 / 2; // hash table efficiency
		hashTable = new int[tableSize];
		for (int i = 0; i < tableSize; i++)
			hashTable[i] = -1;
		this.x_extent = x_extent;
		this.y_extent = y_extent;
	}

	/**
	 * find or create reference to MapPoint(x,y)
	 * 
	 * We use an open hash table to note the index associated with a particular hash
	 * value
	 * 
	 * @param x
	 *            x coordinate
	 * @param y
	 *            y coordinate
	 * @return associated MapPoint
	 */
	public MapPoint findPoint(double x, double y) {
		double value = ((x + x_extent / 2) + (y + y_extent / 2)) * tableSize;
		int guess = ((int) value) % tableSize;
		while (hashTable[guess] != -1) {
			MapPoint m = vertices[hashTable[guess]];
			if (m.x == x && m.y == y)
				return m; // this one is already known
			guess = (guess == tableSize - 1) ? 0 : guess + 1;
		}
		// add a new MapPoint to the list
		MapPoint m = new MapPoint(x, y, numVertices);
		hashTable[guess] = numVertices;
		vertices[numVertices++] = m;
		return m;
	}
}