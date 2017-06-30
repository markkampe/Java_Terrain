

/**
 * a MapPoint has an X and Y coordinate
 *
 */
public class MapPoint {
	public double x;		// X coordinate
	public double y;		// Y coordinate
	public int index;		// point index #
	public int neighbors[];	// neighbor's index #s
	
	private static final String format="%7.5f";
	
	public MapPoint(double x,double y) {
		this.x = x;
		this.y = y;
		neighbors = new int[3];
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
