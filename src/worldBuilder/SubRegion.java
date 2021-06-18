package worldBuilder;

import java.util.Iterator;
import java.util.LinkedList;

public class SubRegion {
	private Mesh newMesh;
	
	public SubRegion(int numPoints) {
		// create a new mesh
		newMesh = new Mesh();
		MeshPoint[] points = newMesh.makePoints(numPoints);
		newMesh.makeMesh(points);
	}
	
	public boolean newMap(Map map) {
		MapWindow window = map.window;
		Mesh oldMesh = map.mesh;
		double[] heightMap = map.getHeightMap();
		double[] erodeMap = map.getErodeMap();
		double[] rainMap = map.getRainMap();
		double[] soilMap = map.getSoilMap();
		double[] floraMap = map.getFloraMap();
		double[] faunaMap = map.getFaunaMap();
		double[] fluxMap = map.getFluxMap();
		String[] oldNames = map.getNameMap();
		LinkedList<TradeRoute> oldTrade = map.tradeRoutes();
		
		// compute x/y coordinate translation coefficients
		double xShift = window.map_x(window.sel_x0 + (window.sel_width/2));
		double yShift = window.map_y(window.sel_y0 + (window.sel_height/2));
		double xScale = Parameters.x_extent/window.map_width(window.sel_width);
		double yScale = Parameters.y_extent/window.map_height(window.sel_height);

		// allocate new per-point attribute maps
		int newlen = newMesh.vertices.length;
		double[] h = new double[newlen];	// height map
		double[] e = new double[newlen];	// erosion map
		double[] r = new double[newlen];	// rain map
		double[] m = new double[newlen];	// soil/mineral map
		double[] f = new double[newlen];	// flora map
		double[] a = new double[newlen];	// fauna map
		double[] w = new double[newlen];	// incoming water

		// interpolate per-point attributes for each mesh point
		for(int i = 0; i < newlen; i++) {
			// find the corresponding previous-map coordinates
			double x = (newMesh.vertices[i].x/xScale) + xShift;
			double y = (newMesh.vertices[i].y/yScale) + yShift;
			
			// find surrounding points from the previous map
			Vicinity poly = new Polygon(oldMesh, x, y);

			// interpolate values for spatial attributes
			h[i] = poly.interpolate(heightMap);
			e[i] = poly.interpolate(erodeMap);
			r[i] = poly.interpolate(rainMap);
			m[i] = poly.nearest(soilMap);
			f[i] = poly.nearest(floraMap);
			a[i] = poly.nearest(faunaMap);
		}
		
		// reproduce all water flows into the box
		for(int i = 0; i < oldMesh.vertices.length; i++) {
			// find out-of-the-box points that flow into the box
			if (fluxMap[i] <= 0)
				continue;
			MeshPoint p1 = oldMesh.vertices[i];
			if (window.inTheBox(p1.x, p1.y))
				continue;
			int d = map.getDrainage().downHill[i];
			if (d < 0)
				continue;
			MeshPoint p2 = oldMesh.vertices[d];
			if (!window.inTheBox(p2.x,p2.y))
				continue;
			
			// direct flow to corresponding point in new Map
			double x = (p2.x - xShift) * xScale;	// new Map x coordinate
			double y = (p2.y - yShift) * yScale;	// new Map y coordinate
			MeshPoint p3 = newMesh.choosePoint(x, y);
			w[p3.index] += fluxMap[i];
		}
		
		
		// push all of these changes back to the map
		map.isSubRegion = true;
		map.setMesh(newMesh);
		map.setRainMap(r);
		map.setSoilMap(m);
		map.setFloraMap(f);
		map.setFaunaMap(a);
	
		map.setErodeMap(e);		// FIX sub-region recomputed erosion/deposition differ
		map.setIncoming(w);		// FIX sub-region recomputed minor streams differ
								// FIX sub-region recomputed lake boundaries differ
		
		// these will force drainage and waterflow recomputation
		map.setHeightMap(h);
		
		// reproduce all named points within the box
		String[] n = map.getNameMap();
		for(int i = 0; i < oldMesh.vertices.length; i++) {
			if (oldNames[i] == null)
				continue;
			MeshPoint p = oldMesh.vertices[i];
			if (window.inTheBox(p.x, p.y)) {
				// find corresponding point in new Map
				double x = (p.x - xShift) * xScale;	// new Map x coordinate
				double y = (p.y - yShift) * yScale;	// new Map y coordinate
				MeshPoint p2 = newMesh.choosePoint(x, y);
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
					double x = (first.x - xShift) * xScale;	// new Map x coordinate
					double y = (first.y - yShift) * yScale;	// new Map y coordinate
					MeshPoint p1 = newMesh.choosePoint(x, y);
					x = (last.x - xShift) * xScale;	// new Map x coordinate
					y = (last.y - yShift) * yScale;	// new Map y coordinate
					MeshPoint p2 = newMesh.choosePoint(x, y);
					
					// note the start/end points (one of which may be the ocean)
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
}
