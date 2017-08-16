package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class RiverMap {
	private Map map;		// mesh to which we correspond
	private double height[]; // height of each MeshPoint
	private int byHeight[];	// MeshPoints sorted by height
	private double maxFlux;	// maximum flow on map
	
	private static final int WATER_DIM = 128;	// dimmest water
	
	private Parameters parms;
	
	public RiverMap(Map m) {
		this.map = m;
		this.parms = Parameters.getInstance();
	}
	
	
	/**
	 * estimated flow velocity
	 * @param slope ... dZ/dX
	 * @return flow speed (meters/second)
	 * 
	 * NOTE: velocity ranges from .1m/s to 3m/s
	 * 		 max sustainable slope is 1/1
	 */
	public static double velocity(double slope) {
		return 3 * slope;
	}
	
	/**
	 * estimated river width and depth
	 * 
	 * @param flow ... rate (cubic meters/second)
	 * @param velocity ... flow speed (meters/second)
	 * 
	 * NOTE: W/D ranges from 2-20 ... call it 6/V
	 * 	1. Area = Flow/Velocity
	 * 	2. Area = W x D
	 * 	3. W / D = 6/V
	 * 
	 *  from (3):
	 *  	(3a) W=6D/V or (3b) D=WV/6
	 *  substituting (3a) into (2)
	 *  	A = 6D/V * D = 6D^2/V; D = sqrt(AV/6)
	 *  substituting (3b) into (2)
	 *  	A = W * WV/6 = W^2V/6; W = sqrt(6A/V)
	 */
	public static double width(double flow, double velocity) {
		double area = flow / velocity;
		return Math.sqrt(6 * area / velocity);
	}

	public static double depth(double flow, double velocity) {
		double area = flow / velocity;
		return Math.sqrt(area * velocity / 6);
	}
	
	/**
	 * estimated erosion and deposition
	 * 
	 * NOTE:
	 * 	The Hjulstrom curve says that 
	 * 		erosion starts at 1m/s and is major by 2m/s
	 * 			slopes between .6 and 1.2
	 * 		sedimentation starts at .1m/s and is done by .005m/s
	 * 			slopes between .03 and .0015
	 */
	// returns M3 soil per M3/s of flow
	public double erosion( double v ) {
		double Ve = parms.Ve;
		return (v < Ve) ? 0 : parms.Ce * (v * v)/(Ve * Ve);
	}
	
	// returns fraction of carried load per km
	public double sedimentation( double v) {
		double Vs = parms.Vd;
		return (v > Vs) ? 0 : parms.Cd/v;
	}
	
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

		// calculate the color curve (blue vs flow)
		double flux[] = this.calculate();
		int blue_range = 255 - WATER_DIM;
		double min_stream = parms.stream_flux;
		double dBdF = blue_range/(maxFlux - min_stream);
		
		// draw the streams, rivers, lakes and oceans
		for(int i = 0; i < flux.length; i++) {
			if (heightMap[i] < parms.sea_level)
				continue;	// don't display rivers under the ocean
			if (flux[i] < min_stream)
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
				// blue gets brighter, green dimmer w/increasing flow
				double delta = (flux[i] - min_stream) * dBdF;
				double blue = WATER_DIM + delta;
				double green = Math.max(0, WATER_DIM - delta);
				g.setColor(new Color(0, (int) green, (int) blue));
				g.drawLine((int) x1, (int) y1, (int) x2, (int) y2);
			} else {
				// TODO: render basins
			}
		}
	}
	
	
	/**
	 * (re)calculate the water flux in each MeshPoint
	 * 	and (as side-effect), sets maxFlux
	 */
	public double[]calculate() {
		double[] flux = new double[map.getMesh().vertices.length];
		maxFlux = 0;
		
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
		
		// figure out the mapping from rainfall to water flow
		double area = (parms.xy_range * parms.xy_range) / byHeight.length;
		double year = 365.25 * 24 * 60 * 60;
		double rain_to_flow = .01 * area * 1000000 / year;
		
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
			flux[x] += rain[x] * rain_to_flow;

			// each cell gets what drains from above
			if (downHill[x] >= 0)
				flux[downHill[x]] += flux[x]; 
			
			// note the greatest flux we have seen
			if (flux[x] > maxFlux)
				maxFlux = flux[x];
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
