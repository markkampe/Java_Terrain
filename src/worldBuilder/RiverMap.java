package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class RiverMap {
	private Map map;		// mesh to which we correspond
	private double height[]; // height of each MeshPoint
	private int byHeight[];	// MeshPoints sorted by height
	private Parameters parms;
	
	public RiverMap(Map m) {
		this.map = m;
		this.parms = Parameters.getInstance();
	}
	
	/*
	 * notes on the mathematics of rivers
	 * 	Flow = width * depth * Velocity
	 * 	velocity: 	.1m/s - 3M/s	3*slope?
	 *  width/depth: 2-20			6/V?
	 *  
	 * 	use slope to compute velocity
	 * 	use flow & velocity to compute WxD
	 * 	use velocity to compute W/D
	 *  solve for W,D
	 *  
	 *  simplified Hjulstrom curves
	 *    use velocity to compute erosion and burden
	 *  	erosion starts 1 .2m/s, is major at 2m/s
	 *    use velocity and burden to compute deposition
	 *  	soil deposition happens between .1 - .005 m/s
	 *  call deposited soil area 10x river width
	 */
	
	/**
	 * Display the streams and rivers
	 * 
	 * @param graphics context
	 * @param display map width
	 * @param display map height
	 */
	public void paint(Graphics g, int width, int height) {
		
		Mesh mesh = map.getMesh();
		double[] heightMap = map.getHeightMap();
		int downHill[] = map.getDownHill();

		// calculate the minimum flow to qualify as a stream
		//	m2 = estimated water-shed area for this MeshPoint
		//	year = number of seconds in a year
		//	parms.min_flux = minimum M3/sec for a stream
		//	minFlow = minimum annual cm of flow for a stream
		double m2 = parms.xy_range * parms.xy_range * 1000000 / mesh.vertices.length;
		double year = 365 * 24 * 60 * 60;
		double minFlow = parms.min_flux * year * 100 / m2;

		// calculate the color curve (green vs flow)
		//	a river is RIVER_FLOW * minFlow
		double river_threshold = minFlow * Parameters.RIVER_FLOW;
		double dGdF = 255/(river_threshold - minFlow);
		double intercept = river_threshold * dGdF;
		
		// draw the streams, rivers, lakes and oceans
		double flux[] = this.calculate();
		for(int i = 0; i < flux.length; i++) {
			if (heightMap[i] < parms.sea_level)
				continue;	// don't display rivers under the ocean
			if (flux[i] < minFlow)
				continue;	// don't display flux below stream cut-off
			if (downHill[i] >= 0) {
				int d = downHill[i];
				double x1 = (mesh.vertices[i].x + Parameters.x_extent/2) * width;
				double y1 = (mesh.vertices[i].y + Parameters.y_extent/2) * height;
				double x2 = (mesh.vertices[d].x + Parameters.x_extent/2) * width;
				double y2 = (mesh.vertices[d].y + Parameters.y_extent/2) * height;
				
				// if a river segment flows into the sea, halve its length
				if (heightMap[d] < parms.sea_level) {
					x2 = (x1 + x2)/2;
					y2 = (y1 + y2)/2;
				}
				
				// interpolate a color: river=BLUE, stream=CYAN
				double green = Math.max(0, intercept - (dGdF * flux[i]));
				g.setColor(new Color(0, (int) green, 255));
				g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
			} else {
				// TODO: render basins
			}
		}
	}
	
	
	/**
	 * (re)calculate the water flux in each MeshPoint
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
		
		// make sure we know what is downhill from what
		int downHill[] = map.getDownHill();
		
		// processing MeshPoints from highest to lowest
		//	flux[this] += this MeshPoint's rainfall
		//	flux[downhill] += flux[this]
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
