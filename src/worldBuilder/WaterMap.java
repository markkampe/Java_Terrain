package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

public class WaterMap {

	private Map map;
	private Parameters parms;
	
	public WaterMap(Map map) {
		this.map = map;
		this.parms = Parameters.getInstance();
	}

	/**
	 * Render bodies of water
	 * 
	 * @param graphics context
	 * @param display map width
	 * @param display map height
	 * @param pixels per displayed cell (MUST BE 5)
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {
		int h = height/cellWidth;
		int w = width/cellWidth;
		
		// interpolate Z values from the latest mesh
		double zArray[][] = map.getCartesian().interpolate(map.getHeightMap());
		
		// identify local depressions
		
		// use height to generate background colors
		g.setColor(Color.BLUE);
		double seaLevel = parms.sea_level;
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				int sum = 0;
				if (r < h-1 && c < w-1) {
					// FIX identify and fill depressions
					// see which of our neighbors are under water
					if (zArray[r][c] >= seaLevel)
						sum += 8;
					if (zArray[r][c + 1] >= seaLevel)
						sum += 4;
					if (zArray[r + 1][c] >= seaLevel)
						sum += 1;
					if (zArray[r + 1][c + 1] >= seaLevel)
						sum += 2;
				} else
						sum = (zArray[r][c] >= seaLevel) ? 15 : 0;
				
				// render a cell of the appropriate shape
				waterCell(g, r, c, cellWidth, sum);
			}
	}
	
	/**
	 * render a cellWidth*cellWidth block of a water, with
	 * 	the particular pixels determined by the neighbor sum
	 * 
	 * @param Graphics context
	 * @param topographic row
	 * @param topographic column
	 * @param pixel width of a cell (MUST BE 5)
	 * @param sum of corners (from Marching Square)
	 */
	private void waterCell(Graphics g, int r, int c, int cellWidth, int sum) {
	
		// if it is all above water, there is nothing to draw
		if (sides[sum] == 0)
			return;
		
		// load up the coordinates
		int Xs[] = new int[6];
		int Ys[] = new int[6];
		for(int i = 0; i < 6; i++) {
			Xs[i] = c * cellWidth + xOffsets[sum][i];
			Ys[i] = r * cellWidth + yOffsets[sum][i];
		}
		
		g.fillPolygon(Xs, Ys, sides[sum]);
	}
	
	// TODO: why is the coast line ragget?
	
	// how many sides in the water polygon
	private static final int sides[] = {
			4,	// 0: all under water
			5,	// 1: lower left above
			5,	// 2: lower right above
			4,	// 3: top half under
			5,	// 4: top right above
			6,	// 5: SW/NE saddle above
			4,	// 6: left half under
			3,	// 7: top left under
			5,	// 8: top left above
			4,	// 9: right half under
			6,	// 10: NW/SE saddle above
			3,	// 11: top right under
			4,	// 12: bottom half under
			3,	// 13: bottom right under
			3,	// 14: bottom left under
			0	// 15: all above water	
	};
	
	// x offsets for each vertex, for each sum
	private static final int xOffsets[][] = {
			{ 0, 5, 5, 0, 0, 0 },	// 0: all under water
			{ 1, 5, 5, 3, 1, 0 },	// 1: lower left above
			{ 0, 3, 3, 1, 0, 0 },	// 2: lower right above
			{ 0, 5, 5, 0, 0, 0 },	// 3: top half under
			{ 0, 1, 3, 3, 0, 0 },	// 4: top right above
			{ 0, 1, 5, 5, 3, 0 },	// 5: SW/NE saddle above
			{ 0, 2, 2, 0, 0, 0 },	// 6: left half under
			{ 0, 2, 0, 0, 0, 0 },	// 7: top left under
			{ 3, 5, 5, 0, 0, 0 },	// 8: top left above
			{ 3, 5, 5, 3, 0, 0 },	// 9: right half under
			{ 3, 5, 5, 1, 0, 0 },	// 10: NW/SE saddle above
			{ 3, 5, 5, 0, 0, 0 },	// 11: top right under
			{ 0, 5, 5, 0, 0, 0 },	// 12: bottom half under
			{ 5, 5, 3, 0, 0, 0 },	// 13: bottom right under
			{ 0, 2, 0, 0, 0, 0 },	// 14: bottom left under
			{ 0, 0, 0, 0, 0, 0 }	// 15: all above water
	};
	
	// y offsets for each vertex, for each sum
	private static final int yOffsets[][] = {
			{ 0, 0, 5, 5, 0, 0 },	// 0: all under water
			{ 0, 5, 5, 3, 1, 0 },	// 1: lower left above
			{ 0, 0, 1, 3, 5, 0 },	// 2: lower right above
			{ 0, 0, 2, 2, 0, 0 },	// 3: top half under
			{ 0, 0, 3, 5, 5, 0 },	// 4: top right above
			{ 0, 0, 3, 5, 5, 1 },	// 5: SW/NE saddle above
			{ 0, 0, 5, 5, 0, 0 },	// 6: left half under
			{ 0, 0, 2, 0, 0, 0 },	// 7: top left under
			{ 0, 0, 5, 5, 3, 0 },	// 8: top left above
			{ 0, 0, 5, 5, 0, 0 },	// 9: right half under
			{ 0, 0, 1, 3, 5, 3 },	// 10: NW/SE saddle above
			{ 0, 0, 2, 0, 0, 0 },	// 11: top right under
			{ 3, 3, 5, 5, 0, 0 },	// 12: bottom half under
			{ 3, 5, 5, 0, 0, 0 },	// 13: bottom right under
			{ 3, 5, 5, 0, 0, 0 },	// 14: bottom left under
			{ 0, 0, 0, 0, 0, 0 }	// 15: all above water
	};
}
