package worldBuilder;

/**
 * a MeshPoint has X and Y coordinates, and index, and neighbors
 */
public class MeshPoint {
	/** Map coordinates of this point */
	public double x, y;
	
	/** index (in Mesh array) of this MeshPoint */
	public int index;

	/** number of neighboring MeshPoints	*/
	public int neighbors;
	/** array of neighboring MeshPoints	*/
	public MeshPoint[] neighbor;
	
	/** corresponds to main-map point and cannot be changed	*/
	public boolean immutable;
	
	private static final String format="%7.5f";
	
	/**
	 * instantiate a MeshPoint w/no index
	 * @param x position (-0.5 to 0.5 map coordinate)
	 * @param y position (-0.5 to 0.5 map coordinate)
	 */
	public MeshPoint(double x,double y) {
		this.x = x;
		this.y = y;
		this.index = -1;
	}
	
	/**
	 * instantiate a MeshPoint w/known index
	 * @param x position (-0.5 to 0.5 map coordinate)
	 * @param y position (-0.5 to 0.5 map coordinate)
	 * @param index (in Mesh) of this point
	 */
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
	
	/**
	 * is a particular MeshPoint one of my neighbors
	 * @param point MeshPoint to be checked
	 * @return true if it is one of my neighbors
	 */
	public boolean isNeighbor(MeshPoint point) {
		for(int i = 0; i < neighbors; i++)
			if (neighbor[i] == point)
				return(true);
		return(false);
	}
	
	/**
	 * compute the (positive) distance between two points
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
	 * compute the distance to a specified line
	 *
	 *	Notes:
	 *    - this algorithm computes the area of the triangle
	 *	    (between the line points and point in question), and
	 *		divides it by the base (between the line points).
	 *    - even though end-points are given, this algorithm
	 *		treates the base-line as infinite, so off the end
	 *	    can still be very close.
	 *
	 * @param x1 map position (-0.5 to 0.5) of one end of line
	 * @param y1 map position (-0.5 to 0.5) of one end of line
	 * @param x2 map position (-0.5 to 0.5) of other end of line
	 * @param y2 map position (-0.5 to 0.5) of other end of line
	 * @return distance (which can be positive or negative)
	 */
	public double distanceLine(double x1, double y1, double x2, double y2) {
		double d = ((y2 - y1)*this.x - (x2 - x1)*this.y + x2*y1 - y2*x1) / Math.sqrt((y2-y1)*(y2-y1) + (x2-x1)*(x2-x1));
		return d;
	}
	
	/**
	 * is a point on the edge of the map
	 * @return true if point has fewer than three neighbors
	 */
	boolean isOnEdge() {
		return neighbors < 3;
	}
	
	/**
	 * Find the point where a river/route enters/exits a box
	 * @param p1 source
	 * @param p2 destination
	 * @param x left edge of box
	 * @param y top of box
	 * @param width of box
	 * @param height of box
	 * @return MeshPoint for the desired crossing point
	 */
	static public MeshPoint crossingPoint(MeshPoint p1, MeshPoint p2, double x, double y, double width, double height) {
		// SOMEDAY replace (imprecise) interpolation w/solving for intersection
		
		// see how much of the x range is inside the box
		double min = (p1.x < p2.x) ? p1.x : p2.x;
		double max = (p1.x < p2.x) ? p2.x : p1.x;
		double left = x;
		double right = x + width;
		double x_range = 1.0;
		if (min < left && max >= left)
			x_range = (max - left)/(max - min);
		else if (min <= right && max > right)
			x_range = (right-min)/(max - min);
		// see how much of the y range is inside the box
		min = (p1.y < p2.y) ? p1.y : p2.y;
		max = (p1.y < p2.y) ? p2.y : p1.y;
		double top = y;
		double bot = y + height;
		double y_range = 1.0;
		if (min < top && max >= top)
			y_range = (max - top)/(max - min);
		else if (min <= bot && max > bot)
			y_range = (bot-min)/(max - min);
			
		// use the lesser of those as our interpolation factor
		double dx = p2.x - p1.x;
		double dy = p2.y - p1.y;
		double inside = (x_range < y_range) ? x_range : y_range;
		
		// our estimate of where the line crosses the border
		return new MeshPoint(p1.x + (dx * inside), p1.y + (dy * inside));
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
	
	/**
	 * @return (string) coordinates of point
	 */
	public String toString() {
		return "<" + String.format(format,x) + "," + String.format(format,y) + ">";
	}	
}
