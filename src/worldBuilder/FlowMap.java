package worldBuilder;

public class FlowMap {
	private Map map;		// mesh to which we correspond
	private double height[]; // height of each MeshPoint
	private int byHeight[];	// MeshPoints sorted by height
	private Parameters parms;
	
	public FlowMap(Map m) {
		this.map = m;
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * (re)calculate the water flux in each MeshPoint
	 * 		may have been changes in height/rainfall
	 */
	public double[]calculate() {
		double[] flux = new double[map.getMesh().vertices.length];
		
		// make sure we have water flow to calculate
		Mesh m = map.getMesh();
		if (m == null)
			return flux;
		height = map.getHeightMap();
		if (height == null)
			return flux;
		double[] rain = map.getRainMap();
		if (rain == null)
			return flux;
		
		// sort the MeshPoints by descending height
		byHeight = new int[height.length];
		for( int i = 0; i < byHeight.length; i++ )
			byHeight[i] = i;
		heightSort(0, byHeight.length - 1);
		
		// compute the flow into each cell, from highest to lowest
		int downHill[] = map.getDownHill();
		for( int i = 0; i < byHeight.length; i++ ) {
			int x = byHeight[i];
			
			// flow stops once we hit sea level
			if (height[x] < parms.sea_level) {
				flux[x] = 0;
				continue;
			}
			
			// each cell gets its own rainfall
			flux[x] += rain[x];
			// each cell gets what drains from above
			if (downHill[x] >= 0)
				flux[downHill[x]] += flux[x]; 
		}
		return(flux);
	}
	
	/**
	 * (recursive) QuickSort a list of points by height
	 * @param left ... left most index of sort region
	 * @param right ... right most index of sort region
	 */
	private void heightSort(int left, int right) {
		// find the X coordinate of my middle element
        int pivotIndex = left + (right - left) / 2;
        double pivotValue = height[byHeight[pivotIndex]];
 
        // for every point in my range
        int i = left, j = right;
        while(i <= j) {
        	// find the first thing on left that belongs on right
            while(height[byHeight[i]] >  pivotValue)
                i++;
            // find first thing on right that belongs on left
            while(height[byHeight[j]] < pivotValue)
                j--;
 
            // swap them
            if(i <= j) {
            	if (i < j) {
            		int tmp = byHeight[i];
            		byHeight[i] = byHeight[j];
            		byHeight[j] = tmp;
            	}
                i++;
                j--;
            }
        }
    
        // recursively sort everything to my left
        if(left < j)
            heightSort(left, j);
        // recursively sort everything to my right
        if(right > i)
            heightSort(i, right);
    }

}
