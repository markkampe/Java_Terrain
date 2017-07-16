package WorldBuilder;

/**
 * This class is used to generate a single unique Path for every 
 * pair of connected MapPoints, and find the already allocated 
 * Path if it already exists.
 * 
 * This is done with an open hash table, to optimize time
 * over space.
 */
public class PathHasher {
	public Path[] paths;		// list of known paths
	public int numPaths;		// number of known paths
	private int tableSize;		// total size of hash table
	private int[] hashTable;	// index of know path, or -1

	/**
	 * allocate a hash table and vertex list
	 * 
	 * @param max ... max # of paths
	 */
	public PathHasher(int max) {
		paths = new Path[max];
		numPaths = 0;
		tableSize = max * 3 / 2; // hash table efficiency
		hashTable = new int[tableSize];
		for (int i = 0; i < tableSize; i++)
			hashTable[i] = -1;
	}

	/**
	 * find or create reference to Path(p1,p2)
	 * 
	 * We use an open hash table to note the index associated with a particular hash
	 * value
	 * 
	 * @param one end
	 * @param other end

	 * @return associated Path
	 */
	public Path findPath(MapPoint p1, MapPoint p2) {
		int guess = (p1.index + p2.index) % tableSize;
		while (hashTable[guess] != -1) {
			Path p = paths[hashTable[guess]];
			if (p.source == p1 && p.target == p2)
				return p;
			if (p.source == p2 && p.target == p1)
				return p;
			guess = (guess == tableSize - 1) ? 0 : guess + 1;
		}
		// add a new MapPoint to the list
		Path p = new Path(p1, p2, numPaths);
		hashTable[guess] = numPaths;
		paths[numPaths++] = p;
		return p;
	}
}
