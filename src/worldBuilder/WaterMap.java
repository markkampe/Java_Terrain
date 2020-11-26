package worldBuilder;

import java.awt.Color;
import java.awt.Graphics;

/**
 * a class to render rivers, lakes, and oceans in shares of blue.
 */
public class WaterMap {

	private Map map;
	// private Parameters parms;
	
	double[][] waterMap;	// + = above water, - = below water
	
	// returned bitmap from check_neighbors
	private static final int UL  = 0x01;	// upper left neighbor
	private static final int TOP = 0x02;	// top neighbor
	private static final int UR  = 0x04;	// upper right neighbor
	private static final int RGT = 0x08;	// right side neighbor
	private static final int LR  = 0x10;	// lower right neighbor
	private static final int BOT = 0x20;	// bottom neighbor
	private static final int LL  = 0x40;	// lower left neighbor
	private static final int LFT = 0x80;	// left side neighbor
	private static final int ALL = 0xff;	// all neighbors
	
	/**
	 * instantiate a river and water-body map renderer
	 * @param map	to be rendered
	 */
	public WaterMap(Map map) {
		this.map = map;
		// this.parms = Parameters.getInstance();
	}
	
	/**
	 * Render bodies of water
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 * @param cellWidth - pixels per cell
	 */
	public void paint(Graphics g, int width, int height, int cellWidth) {
		int h = height/cellWidth;
		int w = width/cellWidth;
		
		// interpolate per-cell water depth from the mesh
		waterMap = map.getCartesian().interpolate(map.getDepthMap());
		
		/*
		 * We paint blue any point that is under water, and
		 * leave land points as already painted.  But if we
		 * just painted squares of water, the shore-lines
		 * would be very jaggetd.
		 * 
		 * If a water-corner is surrounded (on both sides) by
		 * land, we round it off by adjusting the starting and
		 * ending points of each scan line to leave a triangle
		 * of land in that corner.
		 * 
		 * If a land-corner is surrounded (on both sides) by
		 * water, we round it off by drawing a triangle of 
		 * water in that corner.
		 */
		int starts[] = new int[cellWidth];
		int ends[] = new int[cellWidth];
		for(int i = 0; i < cellWidth; i++) {
			starts[i] = 0;			// all water-cell scan lines
			ends[i] = cellWidth;	// default to full width
		}
		
		g.setColor(Color.BLUE);		// we only draw water
		for(int r = 0; r < h; r++)
			for(int c = 0; c < w; c++) {
				// locate upper left-hand corner of this cell
				int x = c * cellWidth;
				int y = r * cellWidth;
				
				if (waterMap[r][c] < 0) {	// this cell is under-water
					int drys = check_neighbors(r, c, true);
					if ((drys & (LFT+UL+TOP)) == (LFT+UL+TOP)) {
						// round off the upper left corner
						starts[0] = 2;
						starts[1] = 1;
					} else {
						starts[0] = 0;
						starts[1] = 0;
					}
					if ((drys & (TOP+UR+RGT)) == (TOP+UR+RGT)) {
						// round off the upper right corner
						ends[0] = cellWidth - 2;
						ends[1] = cellWidth - 1;
					} else {
						ends[0] = cellWidth;
						ends[1] = cellWidth;
					}
					if ((drys & (LFT+LL+BOT)) == (LFT+LL+BOT)) {
						// round off the lower left corner
						starts[cellWidth-2] = 1;
						starts[cellWidth-1] = 2;
					} else {
						starts[cellWidth-2] = 0;
						starts[cellWidth-1] = 0;
					}
					if ((drys & (RGT+LR+BOT)) == (RGT+LR+BOT)) {
						// round off the lower right corner
						ends[cellWidth-2] = cellWidth - 1;
						ends[cellWidth-1] = cellWidth - 2;
					} else {
						ends[cellWidth-2] = cellWidth;
						ends[cellWidth-1] = cellWidth;
					}
					for(int i = 0; i < cellWidth; i++)
						g.drawLine(x + starts[i], y+i, x + ends[i], y+i);
				} else {
					// cell is above water, round off any corner surrounded by water
					int wets = check_neighbors(r, c, false);
					if ((wets & (LFT+UL+TOP)) == (LFT+UL+TOP)) {
						// round off the upper left corner
						g.drawLine(x, y, x+2, y);
						g.drawLine(x, y+1, x+1, y+1);
					}
					if ((wets & (TOP+UR+RGT)) == (TOP+UR+RGT)) {
						// round off the upper right corner
						g.drawLine(x+3, y, x+cellWidth, y);
						g.drawLine(x+cellWidth-1, y+1, x+cellWidth, y);
					}
					if ((wets & (LFT+LL+BOT)) == (LFT+LL+BOT)) {
						// round off the lower left corner
						g.drawLine(x, y+cellWidth-2, x+1, y+cellWidth-2);
						g.drawLine(x, y+cellWidth-1, x+2, y+cellWidth-1);
					}
					if ((wets & (RGT+LR+BOT)) == (RGT+LR+BOT)) {
						// round off the lower right corner
						g.drawLine(x+cellWidth-1, y+cellWidth-2, x+cellWidth, y+cellWidth-2);
						g.drawLine(x+cellWidth-2, y+cellWidth-1, x+cellWidth, y+cellWidth-1);
					}
				}
			}
	}

	/**
	 * check to see which of our neighbors are dry/wet
	 * 
	 * @param row of point to be checked
	 * @param col of point to be checked
	 * @param dry do we want to know about dry (vs wet) neighbors
	 * @return bit mask of which neighbors are dry (or wet)
	 */
	int check_neighbors(int row, int col, boolean dry) {
		
		int dry_neighbors = 0;
		if (row > 0) {
			if (col > 0)
				dry_neighbors += waterMap[row-1][col-1] >= 0 ? UL : 0;
			dry_neighbors += waterMap[row-1][col]   >= 0 ? TOP : 0;
			if (col < waterMap[0].length - 1)
				dry_neighbors += waterMap[row-1][col+1] >= 0 ? UR : 0;
		}
		if (col > 0)
			dry_neighbors += waterMap[row][col-1]   >= 0 ? LFT : 0;
		if (col < waterMap[0].length - 1)
			dry_neighbors += waterMap[row][col+1]   >= 0 ? RGT : 0;
		if (row < waterMap.length - 1) {
			if (col > 0)
				dry_neighbors += waterMap[row+1][col-1] >= 0 ? LL : 0;
			dry_neighbors += waterMap[row+1][col]   >= 0 ? BOT : 0;
			if (col < waterMap[0].length-1)
				dry_neighbors += waterMap[row+1][col+1] >= 0 ? LR: 0;
		}
		
		// if asked for wet points, complement the mask
		return dry ? dry_neighbors : dry_neighbors ^ ALL;
	}
}
