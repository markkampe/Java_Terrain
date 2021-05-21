package JsonDump;

import java.awt.Color;

public class RainPreview extends PreviewMap {

	private static final int LIGHT = 220;	// brightest color shade to use
	private static final int DARK = 30;		// darkest color shade to use
	private static final long serialVersionUID = -1;
	
	public RainPreview(MapReader r) {
		super("Rain-Fall", r);
	}
	public void display() {
		
		// get the range of heights and depths
		double max_rain = -666666;
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++) {
				int r = reader.rainfall(i, j);
				if (r > max_rain)
					max_rain = r;
			}
		
		// assign rainfall-proportional grey-scales to each square
		int shade_range = LIGHT - DARK;
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++) {
				int r = reader.rainfall(i, j);
				double r_pctile = r / max_rain;
				double shade = (r > 0) ? DARK + (r_pctile * shade_range) : 0;
				colorMap[i][j] = new Color((int) shade, (int) shade, (int) shade);
			}
		super.display();
	}
}
