package worldBuilder;
/**
 * class generates a single unique MapPoint for each unique coordinate pair, 
 * and find the already allocated point when coordinates are repeated.
 * 
 * I have chosen to do this with an open hash table for time
 * efficiency.
 */
public class MeshPointHasher {
	/** number of (already) recorded MeshPoints	*/
	public int numVertices;
	/** list of (already) recorded MeshPoints	*/
	public MeshPoint[] vertices;

	private int tableSize;		// total size of hash table
	private int[] hashTable;	// index known vertices (or -1)
	private double x_extent;	// needed to normalize hash values
	private double y_extent;	// needed to normalize hash values

	/**
	 * allocate a hash table and vertex list
	 * 
	 * @param max ... max # of vertices
	 * @param x_extent ... x diameter (in -0.5 to 0.5 map coordinates)
	 * @param y_extent ... y_diameter (in -0.5 to 0.5 map coordinates)
	 */
	public MeshPointHasher(int max, double x_extent, double y_extent) {
		vertices = new MeshPoint[max];
		numVertices = 0;
		tableSize = max * 3 / 2; // hash table efficiency
		hashTable = new int[tableSize];
		for (int i = 0; i < tableSize; i++)
			hashTable[i] = -1;
		this.x_extent = x_extent;
		this.y_extent = y_extent;
	}

	/**
	 * find or create reference to MeshPoint at MapPoint(x,y)
	 * 
	 * @param x desired location (in -0.5 to 0.5 map coordinates)
	 * @param y desired location (in -0.5 to 0.5 map coordinates)
	 * @return new (or already associated) MeshPoint
	 *
	 * We use an open hash table to note index associated w/each hash value
	 */
	public MeshPoint findPoint(double x, double y) {
		double value = ((x + x_extent / 2) + (y + y_extent / 2)) * tableSize;
		int guess = ((int) value) % tableSize;
		while (hashTable[guess] != -1) {
			MeshPoint m = vertices[hashTable[guess]];
			if (m.x == x && m.y == y)
				return m; // this one is already known
			guess = (guess == tableSize - 1) ? 0 : guess + 1;
		}
		// add a new MapPoint to the list
		MeshPoint m = new MeshPoint(x, y, numVertices);
		hashTable[guess] = numVertices;
		vertices[numVertices++] = m;
		return m;
	}
}
