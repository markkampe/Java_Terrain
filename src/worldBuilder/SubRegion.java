package worldBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class SubRegion {
	private int numPoints;
	
	private static final int INFLUX_DEBUG = 2;
	
	public SubRegion(int numPoints) {
		this.numPoints = numPoints;
		
	}
	
	/**
	 * instantiate a new sub-region 
	 * @param map	parent map
	 * @param x0	(map) upper-left corner of sub-region
	 * @param y0	(map) upper-left corner of sub-region
	 * @param width	(map) width of sub-region
	 * @param height (map) height of sub-region
	 * 
	 * @return		success/failure
	 */
	public boolean newMap(Map map, double x0, double y0, double width, double height) {
		Parameters parms = Parameters.getInstance();
		MapWindow window = map.window;
		Mesh oldMesh = map.mesh;
		double[] heightMap = map.getHeightMap();
		double[] rainMap = map.getRainMap();
		double[] soilMap = map.getSoilMap();
		double[] floraMap = map.getFloraMap();
		double[] faunaMap = map.getFaunaMap();
		double[] fluxMap = map.getFluxMap();
		double[] erodeMap = map.getErodeMap();
		double[] e_factors = map.getE_factors();
		double[] s_factors = map.getS_factors();
		String[] oldNames = map.getNameMap();
		LinkedList<TradeRoute> oldTrade = map.tradeRoutes();
		
		// figure out the relative width/height of the new window
		double x_shrink = width / Parameters.x_extent;
		double y_shrink = height / Parameters.y_extent;
		double Ox = x0 + (width/2);
		double Oy = y0 + (height/2);
		
		// ensure river border-crossing points are in new Mesh
		ArrayList<MeshPoint> entries = new ArrayList<MeshPoint>();
		for(int i = 0; i < oldMesh.vertices.length; i++) {
			// find points that deliver flux downhill
			if (fluxMap[i] <= 0)
				continue;
			int d = map.getDrainage().downHill[i];
			if (d < 0)
				continue;
			
			// does that flow enter or exit the box
			MeshPoint p1 = oldMesh.vertices[i];
			MeshPoint p2 = oldMesh.vertices[d];
			boolean in = window.entersBox(p1.x, p1.y, p2.x, p2.y);
			boolean out = window.exitsBox(p1.x, p1.y, p2.x, p2.y);
			if (in || out) {
				// find crossing point, and add it to new Mesh
				MeshPoint p = MeshPoint.crossingPoint(p1, p2, x0, y0, width, height);
				MeshPoint n = new MeshPoint(inBox((p.x - Ox)/x_shrink), inBox((p.y - Oy)/y_shrink));
				entries.add(n);
				if (parms.debug_level >= INFLUX_DEBUG)
					System.out.println(String.format("... river %s box at (old) %s -> (new) %s",
							in ? "enters" : "exits", p, n));
				}
		}
		
		// TODO - create entries for trade route entry/exit points
	
		// create a new mesh
		Mesh newMesh = new Mesh();
		MeshPoint[] points = newMesh.makePoints(numPoints, entries);
		newMesh.makeMesh(points);

		// allocate new per-point attribute maps
		int newlen = newMesh.vertices.length;
		double[] h = new double[newlen];	// height map
		double[] r = new double[newlen];	// rain map
		double[] m = new double[newlen];	// soil/mineral map
		double[] f = new double[newlen];	// flora map
		double[] a = new double[newlen];	// fauna map
		double[] w = new double[newlen];	// incoming water
		double[] s = new double[newlen];	// incoming sediment
		double[] e = new double[newlen];	// erosion/sedimentation
		double[] ef = new double[newlen];	// erosion scaling factors
		double[] sf = new double[newlen];	// sedimentation scaling factors
		
		// interpolate per-point attributes for each mesh point
		for(int i = 0; i < newlen; i++) {
			// find the corresponding previous-map coordinates
			double x1 = (x_shrink * newMesh.vertices[i].x) + Ox;
			double y1 = (y_shrink * newMesh.vertices[i].y) + Oy;
			
			// find surrounding points from the previous map
			Vicinity poly = new Polygon(oldMesh, x1, y1);

			// interpolate/expand spatially localized attributes
			h[i] = poly.interpolate(heightMap);
			r[i] = poly.interpolate(rainMap);
			m[i] = poly.nearest(soilMap);
			f[i] = poly.nearest(floraMap);
			a[i] = poly.nearest(faunaMap);
			e[i] = poly.nearest(erodeMap);
			ef[i] = poly.nearest(e_factors);
			sf[i] = poly.nearest(s_factors);
		}
		
		// reproduce all water flows into the box
		for(int i = 0; i < oldMesh.vertices.length; i++) {
			// find all old Mesh points that deliver water downhill
			if (fluxMap[i] <= 0)
				continue;
			int d = map.getDrainage().downHill[i];
			if (d < 0)
				continue;
			
			// does that flow enter the new Mesh box
			MeshPoint p1 = oldMesh.vertices[i];
			MeshPoint p2 = oldMesh.vertices[d];
			if (window.entersBox(p1.x, p1.y, p2.x, p2.y)) {
				// create incoming flow at crossing point on new map
				MeshPoint p = MeshPoint.crossingPoint(p1, p2, x0, y0, width, height);
				double x2 = inBox((p.x - Ox) / x_shrink);
				double y2 = inBox((p.y - Oy) / y_shrink);
				p = newMesh.choosePoint(x2, y2);	// closest point in new Mesh
				w[p.index] += fluxMap[i];
				s[p.index] = map.waterflow.suspended[i];
				if (parms.debug_level >= INFLUX_DEBUG)
					System.out.println(String.format("... incoming[%d] (<%.5f,%.5f> -> %s) += %.4f, susp += %.4f", 
										p.index, x2, y2, p, fluxMap[i], map.waterflow.suspended[i]));
			}	
		}
		
		// push all of these changes back to the map
		map.isSubRegion = true;
		map.setMesh(newMesh);
		map.setRainMap(r, true);
		map.setSoilMap(m);
		map.setFloraMap(f);
		map.setFaunaMap(a);
	
		// drainage/waterflow is very expensive, avoid extra recomputes
		map.setSusp(s, false);
		map.setIncoming(w, false);
		map.setErodeMap(e);
		map.setE_factors(ef, false);
		map.setS_factors(sf, false);
		map.setHeightMap(h, true);	// force the recomputation
		
		// reproduce all named points within the box
		String[] n = map.getNameMap();
		for(int i = 0; i < oldMesh.vertices.length; i++) {
			if (oldNames[i] == null)
				continue;
			MeshPoint p = oldMesh.vertices[i];
			if (window.inTheBox(p.x, p.y)) {
				// find corresponding point in new Map
				double x2 = (p.x - Ox) / x_shrink;
				double y2 = (p.y - Oy) / y_shrink;
				MeshPoint p2 = newMesh.choosePoint(x2, y2);
				n[p2.index] = oldNames[i];
			}
		}
				
		// recreate trade routes passing through the box
		if (oldTrade != null && oldTrade.size() > 0) {
			LinkedList<TradeRoute> newTrade = map.tradeRoutes();
			TerritoryEngine te = new TerritoryEngine(map);
			for(Iterator<TradeRoute> it = oldTrade.iterator(); it.hasNext();) {
				TradeRoute oldRoute = it.next();
				// find the first and last in-box points
				MeshPoint first = null, last = null;
				for(int i = 0; i < oldRoute.path.length; i++) {
					MeshPoint p1 = oldMesh.vertices[oldRoute.path[i]];
					if (window.inTheBox(p1.x, p1.y)) {
						if (first == null)
							first = p1;
						last = p1;
					} else if (first != null)
						break;	// we have left the box
				}
				
				// see if all or part of this route was in the box
				if (first != null && last != first) {
					// find corresponding points in new Map
					double x1 = (first.x - Ox) / x_shrink;
					double y1 = (first.y - Oy) / y_shrink;
					MeshPoint p1 = newMesh.choosePoint(x1, y1);
					double x2 = (last.x - Ox) / x_shrink;
					double y2 = (last.y - Oy) / y_shrink;
					MeshPoint p2 = newMesh.choosePoint(x2, y2);
					
					// note the start/end points (one of which may be oceanic)
					boolean ocean_ok = false;
					if (map.getDrainage().oceanic[p1.index])
						ocean_ok = true;
					else
						te.startFrom(p1.index);
					if (map.getDrainage().oceanic[p2.index])
						ocean_ok = true;
					else
						te.startFrom(p2.index);
					
					// create a TradeRoute between those two points
					te.reset();
					TradeRoute newRoute = te.outwards(1,  ocean_ok);
					if (newRoute != null)
						newTrade.add(newRoute);
				}
			}
		}
		
		return true;
	}
	
	/**
	 * ensure an x/y coordinate to be entirely within the map
	 * 
	 * @param v	input value
	 * @return 	corrected value
	 */
	private static final double EPSILON = 0.00001;
	private double inBox(double v) {
		if (v <= -Parameters.x_extent/2)
			return -Parameters.x_extent/2 + EPSILON;
		else if (v >= Parameters.x_extent/2)
			return Parameters.x_extent/2 - EPSILON;
		else
			return v;
	}
}
