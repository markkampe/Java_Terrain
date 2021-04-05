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
	 * @param slopeMap	slope pctile to level map
	 * 
	 * @return int[][]	level of every square
	 */
	public int[][] getLevels(RPGMTiler tiler, int[] altMap, int[] waterMap, int[] slopeMap) {
		
		// figure out the map size
		int y_points = tiler.heights.length;
		int x_points = tiler.heights[0].length;
		
		// figure out minimum and maximum heights/depths/slopes in the region
		double minDepth = 666666, maxDepth = 0;
		double minHeight = 666, maxHeight = 0;
		double minSlope = 666, maxSlope = 0;
		double slopes[][] = (slopeMap == null) ? null : new double[y_points][x_points];
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
				
				if (slopeMap != null) {
					double m = tiler.slope(i,j);
					slopes[i][j] = m;
					if (m < 0)
						m = -m;
					if (m < minSlope)
						minSlope = m;
					if (m > maxSlope)
						maxSlope = m;
				}
			}
		
		// now figure out what the full range is for each characteristic
		double aRange = (maxHeight > minHeight) ? maxHeight - minHeight : 0.000001;
		double dRange = (maxDepth > minDepth) ? maxDepth - minDepth : 1;
		double mRange = (maxSlope > minSlope) ? maxSlope - minSlope : 1;

		// convert every alt/depth/slope into a percentile and map those to levels
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

					// see if slope would reduce the terrain type
					if (slopeMap != null) {
						double m = slopes[i][j];
						pctile = 99 * (m - minSlope) / mRange;
						int mLevel = slopeMap[(int) pctile];
						if (mLevel < levels[i][j])
							levels[i][j] = mLevel;
					}
				}
			}

		return levels;
	}
}
