package worldBuilder;


/**
 * a MapPoint has an X and Y coordinate
 * 		if part of a grid, it has an index and neighbors
 */
public class MapPoint {
	public double x;			// X coordinate
	public double y;			// Y coordinate
	public double z;			// Z coordinate
	
	public int index;			// point index #
	public int neighbors;		// number of neighbors
	public MapPoint[] neighbor;	// neighboring points
	
	private static final String format="%7.5f";
	
	public MapPoint(double x,double y) {
		this.x = x;
		this.y = y;
		this.z = 0;
	}
	
	public MapPoint(double x,double y, int index) {
		this.x = x;
		this.y = y;
		this.z = 0;
		this.index = index;
	}
	
	/**
	 * note that we have a neighbor
	 * @param p ... address of new neighbor
	 */
	void addNeighbor(MapPoint p) { 
		if (neighbors == 0) {
			// allocate array on first add
			neighbor = new MapPoint[3];
		} else {
			// make sure this neighbor isn't already known
			for(int i = 0; i < neighbors; i++)
				if (neighbor[i] == p)
					return;
		}
		assert(neighbors < 3);
		neighbor[neighbors++] = p;
	}
	
	/**
	 * compute the distance between two points
	 * 
	 * @param other
	 * @return distance
	 */
	double distance(MapPoint other) {
		double dx = this.x - other.x;
		double dy = this.y - other.y;
		return Math.sqrt((dx*dx) + (dy*dy));
	}
	
	/**
	 * compute the distance to a line defined by <x0,y0>-<x1,y1>
	 *
	 * @param ... two points on the line
	 * @return distance (which can be positive or negative)
	 */
	double distanceLine(double x1, double y1, double x2, double y2) {
		double d = ((y2 - y1)*this.x - (x2 - x1)*this.y + x2*y1 - y2*x1) / Math.sqrt((y2-y1)*(y2-y1) + (x2-x1)*(x2-x1));
		return d;
	}
	
	/**
	 * is a point on the edge
	 */
	boolean isOnEdge() {
		return neighbors < 3;
	}
	
	/**
	 * (recursive) QuickSort an array of MapPoints
	 * @param arr ... array to be sorted
	 * @param left ... left most index of sort region
	 * @param right ... right most index of sort region
	 */
	static public void quickSort(MapPoint[] arr, int left, int right) {
		// find the X coordinate of my middle element
        int pivotIndex = left + (right - left) / 2;
        double pivotValue = arr[pivotIndex].x;
 
        // for every point in my range
        int i = left, j = right;
        while(i <= j) {
        	// find the first thing on left that belongs on right
            while(arr[i].x < pivotValue)
                i++;
            // find first thing on right that belongs on left
            while(arr[j].x > pivotValue)
                j--;
 
            // swap them
            if(i <= j) {
            	if (i < j) {
	                MapPoint tmp = arr[i];
	                arr[i] = arr[j];
	                arr[j] = tmp;
            	}
                i++;
                j--;
            }
        }
    
        // recursively sort everything to my left
        if(left < j)
            quickSort(arr, left, j);
        // recursively sort everything to my right
        if(right > i)
            quickSort(arr, i, right);
    }
	
	public String toString() {
		return "<" + String.format(format,x) + "," + String.format(format,y) + ">";
	}	
}
