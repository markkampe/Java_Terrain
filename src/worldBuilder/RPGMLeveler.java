package worldBuilder;

/**
 * maps altitude/depth/slope information into RPGMaker "levels",
 * through use of corresponding percentile-to-level maps.
 * 
 * RPGMaker L1/2 tiling is more commonly based on (bucketized) 
 * levels rather than altitudes and depths.  This makes it much
 * easier for a game designer to tweak the (percentile-to-level)
 * mapping to get the desired output.
 * 
 * The per-tile altitude/depth/slope are obtained from the Tiler
 * (into which they have already been loaded).  The percentile 
 * to level maps are computed by the Exporter (based on the
 * corresponding sliders).
 */
public class RPGMLeveler {
	
	/**
	 * compute an abstract level for every map square
	 * 
	 * @param altMap	altitude pctile to level map
	 * @param waterMap	depth pctile to level map
	 * @param plateau	maximum slope for a plateau
	 * @param typeMap	map from levels to terrain types
	 * 
	 * @return int[][]	level of every square
	 */
	public int[][] getLevels(RPGMTiler tiler, int[] altMap, int[] waterMap, double plateau, int typeMap[]) {
		
		// figure out the map size
		int y_points = tiler.heights.length;
		int x_points = tiler.heights[0].length;
		
		// figure out minimum and maximum heights/depths/slopes in the region
		double minDepth = 666666, maxDepth = 0;
		double minHeight = 666, maxHeight = 0;
		for (int i = 0; i < y_points; i++)
			for (int j = 0; j < x_points; j++) {
				double h = tiler.depths[i][j];
				if (h > 0) {	// under water
					if (h < minDepth)
						minDepth = h;
					else if (h > maxDepth)
						maxDepth = h;
				} else {		// dry land
					if (tiler.heights[i][j] < minHeight)
						minHeight = tiler.heights[i][j];
					if (tiler.heights[i][j] > maxHeight)
						maxHeight = tiler.heights[i][j];
				}
			}
		
		// now figure out what the full range is for each characteristic
		double aRange = (maxHeight > minHeight) ? maxHeight - minHeight : 0.000001;
		double dRange = (maxDepth > minDepth) ? maxDepth - minDepth : 1;
		
		// call plateaus the highest ground level
		int plateau_level;
		for (plateau_level = typeMap.length - 1; plateau_level > 0; plateau_level--)
			if (typeMap[plateau_level] == TerrainType.GROUND)
				break;

		// convert every alt/depth into a percentile and map those to levels
		int levels[][] = new int[y_points][x_points];
		for(int i = 0; i < y_points; i++)
			for(int j = 0; j < x_points; j++) {
				if (tiler.depths[i][j] > 0) {	// under water
					double h = tiler.depths[i][j];
					double pctile = 99 * (h - minDepth) / dRange;
					levels[i][j] = waterMap[(int) pctile];
				} else {	// land form (based on height and slope)
					double a = tiler.heights[i][j];
					double pctile = 99 * (a - minHeight) / aRange;
					levels[i][j] = altMap[(int) pctile];

					// see if slope turns mountain tops into plateaus
					if (!TerrainType.isHighLand(typeMap[levels[i][j]]))
						continue;
					if (tiler.slope(i, j) >= plateau)
						continue;
					levels[i][j] = plateau_level;
				}
			}

		return levels;
	}
}
