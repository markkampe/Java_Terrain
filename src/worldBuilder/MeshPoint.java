package worldBuilder;


/**
 * a MeshPoint has X and Y coordinates, and index, and neighbors
 */
public class MeshPoint {
	public double x;			// X coordinate
	public double y;			// Y coordinate
	
	public int index;			// point index #
	public int neighbors;		// number of neighbors
	public MeshPoint[] neighbor;	// neighboring points
	
	public boolean immutable;
	
	private static final String format="%7.5f";
	
	public MeshPoint(double x,double y) {
		this.x = x;
		this.y = y;
		this.index = -1;
	}
	
	public MeshPoint(double x,double y, int index) {
		this.x = x;
		this.y = y;
		this.index = index;
	}
	
	/**
	 * note that we have a neighbor
	 * @param p ... address of new neighbor
	 */
	public void addNeighbor(MeshPoint p) { 
		if (neighbors == 0) {
			// allocate array on first add
			neighbor = new MeshPoint[3];
		} else {
			// make sure this neighbor isn't already known
			for(int i = 0; i < neighbors; i++)
				if (neighbor[i] == p)
					return;
		}
		assert(neighbors < 3);
		neighbor[neighbors++] = p;
	}
	
	public boolean isNeighbor(MeshPoint p) {
		for(int i = 0; i < neighbors; i++)
			if (neighbor[i] == p)
				return(true);
		return(false);
	}
	
	/**
	 * compute the distance between two points
	 * 
	 * @param other
	 * @return distance
	 */
	public double distance(MeshPoint other) {
		double dx = this.x - other.x;
		double dy = this.y - other.y;
		return Math.sqrt((dx*dx) + (dy*dy));
	}
	
	/**
	 * compute the distance to a line defined by (x0,y0)-(x1,y1)
	 *
	 * @param ... two points on the line
	 * @return distance (which can be positive or negative)
	 */
	public double distanceLine(double x1, double y1, double x2, double y2) {
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
	static public void quickSort(MeshPoint[] arr, int left, int right) {
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
	                MeshPoint tmp = arr[i];
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
