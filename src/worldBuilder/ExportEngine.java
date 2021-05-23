package worldBuilder;

/**
 * convert MeshPoint attributes into Cartesian tile attributes and pass
 * them to an exporter
 */ 
public class ExportEngine {	
	/*
	 * the following fields/widgets are created by this base class, to be
	 * used by what ever export sub-class is being used
	 */
	/** map from which we will export	*/
	protected Map map;
	/** parameters Singleton			*/
	protected Parameters parms;

	protected double box_x, box_y;		// selection box map coordinates
	protected double box_width, box_height;	// selection box size (map units)
	public int tile_size;				// size of a tile (in meters)
	public int x_points;				// export with (in tiles)
	public int y_points;				// export height (in tiles)
	
	protected static final int EXPORT_DEBUG = 2;
	
	//private static final long serialVersionUID = 1L;
	
	/**
	 * create the initial export selection dialog and register the listeners
	 * 
	 * @param map ... the map from which we are exporting
	 * @param x ... map x of upper left corner of export region
	 * @param y ... map y of upper left corner of export region
	 * @param width ... of export region (in xy_units)
	 * @param height ... of export region (in xy_units)
	 */
	public ExportEngine( Map map, double x, double y, double width, double height) {
		// pick up references and parameters
		this.map = map;
		this.parms = Parameters.getInstance();
		this.box_x = x;
		this.box_y = y;
		this.box_width = width;
		this.box_height = height;
	}
	
	/**
	 * set the size of a tile for this export
	 * @param meters
	 */
	public void tile_size(int meters) {
		tile_size = meters;
	}
	
	/**
	 * return the export row associated with a y @return coordinate
	 */
	private int box_row(double y) {
		if (y < box_y)
			return(0);
		if (y >= box_y + box_height)
			return(y_points - 1);
		double dy = (y - box_y)/box_height;
		dy *= y_points;
		return (int) dy;
	}
	
	/**
	 * return the export column associated with an x coordinate
	 */
	private int box_col(double x) {
		if (x < box_x)
			return(0);
		if (x >= box_x + box_width)
			return(x_points - 1);
		double dx = (x - box_x)/box_width;
		dx *= x_points;
		return (int) dx;
	}
	
	/**
	 * create a full set of per-tile maps and pass them to the Exporter
	 * 
	 * We need to re-call export whenever the Cartesian changes
	 *  - selected region changes
	 *  - tile size (number of tiles) changes
	 */
	protected void export(Exporter exporter) {
		// get the export region size
		x_points = exporter.export_width();
		y_points = exporter.export_height();

		// tell the exporter the new tilesize
		exporter.tileSize(tile_size);
		
		// export the temperature range
		exporter.temps(parms.meanTemp(), parms.meanSummer(), parms.meanWinter());
		
		double lat = parms.latitude(box_y + box_height/2);
		double lon = parms.longitude(box_x + box_width/2);
		exporter.position(lat, lon);

		// get Cartesian interpolations of tile characteristics
		Cartesian cart = new Cartesian(map.getMesh(), 
										box_x, box_y, box_x+box_width, box_y+box_height,
										x_points, y_points, Cartesian.vicinity.POLYGON);
		
		// figure out which maps we need to up-load
		int needed = exporter.neededInfo();
		double heights[][] = cart.interpolate(map.getHeightMap());
		exporter.heightMap(heights);
		
		double erosion[][] = cart.interpolate(map.getErodeMap());
		if ((needed & Exporter.EROSION) != 0)
			exporter.erodeMap(erosion);
		
		// unclassified soil with sedimentation is alluvial
		if ((needed & Exporter.MINERALS) != 0) {
			int alluvial = map.getSoilType("Alluvial");
			double soil[][] = cart.nearest(map.getSoilMap());
			for(int i = 0; i < soil.length; i++)
				for(int j = 0; j < soil[0].length; j++)
					if (soil[i][j] == 0 && erosion[i][j] < 0)
						soil[i][j] = alluvial;
			exporter.soilMap(soil, map.rockNames);
		}
		
		// per-tile water depth must be computed
		double[] waterLevel = map.getWaterLevel();
		double depth[][] = new double[y_points][x_points];
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++) {
				double water = cart.cells[i][j].nearestValid(waterLevel, WaterFlow.UNKNOWN);
				double height = heights[i][j] - erosion[i][j];
				if (water > height)
					depth[i][j] = water - height;
			}
		add_rivers(depth, tile_size);
		exporter.waterMap(depth);
		
		if ((needed & Exporter.RAINFALL) != 0)
			exporter.rainMap(cart.interpolate(map.getRainMap()));
		if ((needed & Exporter.FLORA) != 0)
			exporter.floraMap(cart.nearest(map.getFloraMap()), map.floraNames);
		if ((needed & Exporter.FAUNA) != 0)
			exporter.faunaMap(cart.nearest(map.getFaunaMap()), map.faunaNames);
	}
	
	/**
	 * overlay rivers on top of interpolated hydration map
	 * 
	 * Note: this cannot simply be interpolated like the rest of
	 * 		the maps because a river is not distributed over the
	 * 		entire MeshPoint, but only in specific tiles.
	 * 
	 * @param	depths (cartesian map to update)
	 * @param 	tilesize (in meters)
	 */
	protected void add_rivers(double[][] depths, int tilesize) {
		
		Mesh mesh = map.getMesh();
		double[] fluxMap = map.getFluxMap();
		int[] downHill = map.getDrainage().downHill;
		double[] waterLevel = map.getWaterLevel();
		double[] heightMap = map.getHeightMap();
		double[] erodeMap = map.getErodeMap();
		
		// consider all points in the Mesh
		for(int i = 0; i < mesh.vertices.length; i++) {
			// ignore any w/no downhill flow
			int d = downHill[i];
			if (d < 0)
				continue;
			
			// ignore any that fall below stream flux
			if (fluxMap[i] < parms.stream_flux)
				continue;
			
			// ignore segments where both src/dst are in same body
			if (waterLevel[i] == waterLevel[d] &&
				waterLevel[i] > heightMap[i] - erodeMap[i] &&
				waterLevel[d] > heightMap[d] - erodeMap[d])
				continue;
	
			// ignore flows that are entirely outside the box
			double x0 = mesh.vertices[i].x;
			double y0 = mesh.vertices[i].y;
			double x1 = mesh.vertices[d].x;
			double y1 = mesh.vertices[d].y;
			if (x0 < box_x && x1 < box_x)
				continue;		// all to the west
			if (x0 >= box_x + box_width && x1 >= box_x + box_width)
				continue;		// all to the east
			if (y0 < box_y && y1 < box_y)
				continue;		// all to the north
			if (y0 >= box_y + box_height && y1 >= box_y + box_height)
				continue;		// all to the south

			// figure out the length and slope
			double dist = 1000 * parms.km(mesh.vertices[i].distance(mesh.vertices[d]));
			double z0 = heightMap[i] - erodeMap[i];
			double z1 = heightMap[d] - erodeMap[d];
			double slope = parms.height(z0 - z1)/dist;
		
			// figure out the river depth and width
			double v = map.waterflow.velocity(slope);
			double width = map.waterflow.width(fluxMap[i],  v);
			double depth = map.waterflow.depth(fluxMap[i],  v);
			double deltaZ = parms.z(depth);
			
			// figure out how many tiles wide the river should be
			int stroke = (width <= tilesize) ? 1 : (int) ((width + width - 1) / tilesize);
			if (tilesize/width > 10 && fluxMap[i] >= parms.river_flux)
				stroke++;
			if (tilesize/width > 100 && fluxMap[i] >= parms.artery_flux)
				stroke++;
			
			// figure out starting and ending positions
			int r = box_row(y0);
			int rDest = box_row(y1);
			int c = box_col(x0);
			int cDest = box_col(x1);
			
			// figure out how far we have to go
			int drawn = 0;
			int dR = rDest - r;
			int dC = cDest - c;
			
			// fill the tiles between here and there with water
			while(drawn++ == 0 || dR != 0 || dC != 0) {
				// figure out which direction we want to move in
				if (Math.abs(dR) > Math.abs(dC)) { // vertical flow
					int start = c - (stroke/2);
					if (r >= 0 && r < y_points && start >= 0 && start + stroke <= x_points) {
						for(int j = 0; j < stroke; j++)
							if (depths[r][start + j] < deltaZ)
								depths[r][start + j] = deltaZ;
							else	// already deep water gets deeper
								depths[r][start+j] += deltaZ;
					}
					// move on to the next row
					r += (dR>0) ? 1 : -1;
					dR = rDest - r;
				} else {	// horizontal flow or last stroke
					int start = r - (stroke/2);
					if (c >= 0 && c < x_points && start >= 0 && start + stroke <= y_points) {
						for(int j = 0; j < stroke; j++)
							if (depths[start + j][c] < deltaZ)
								depths[start + j][c] = deltaZ;
							else	// already deep water gets deeper
								depths[start + j][c] += deltaZ;
					}
					// move on to the next column
					if (dC != 0) {
						c += (dC>0) ? 1 : -1;
						dC = cDest - c;
					}
				}
			}
		}
	}
}
