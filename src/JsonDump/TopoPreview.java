package JsonDump;

import java.awt.Color;

public class TopoPreview extends PreviewMap {

	private static final int LIGHT = 220;	// brightest color shade to use
	private static final int DARK = 30;		// darkest color shade to use
	private static final long serialVersionUID = -1;
	
	public TopoPreview(MapReader r) {
		super("Topography", r);
	}
	public void display() {
		
		// get the range of heights and depths
		double min_alt = 666666;
		double max_alt = -666666;
		double max_depth = -666666;
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++) {
				double h = reader.altitude(i, j);
				if (h < min_alt)
					min_alt = h;
				if (h > max_alt)
					max_alt = h;
				
				double d = reader.depth(i, j);
				if (d > max_depth)
					max_depth = d;
			}
		
		// assign altitude-proportional grey or depth-proportional blue to each square
		int shade_range = LIGHT - DARK;
		double alt_range = max_alt - min_alt;
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++) {
				double h = reader.altitude(i, j);
				double d = reader.depth(i, j);
				if (d > 0) {	// water depth indications
					double d_pctile = d / max_depth;
					double shade = LIGHT - (d_pctile * shade_range);
					colorMap[i][j] = new Color(0, 0, (int) shade);
				} else {		// altitude indicationw
					double h_pctile = (h - min_alt) / alt_range;
					double shade = DARK + (h_pctile * shade_range);
					colorMap[i][j] = new Color((int) shade, (int) shade, (int) shade);
				}
			}
				
		super.display();
	}
}
