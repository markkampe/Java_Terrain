package JsonDump;

import java.awt.Color;

public class SoilPreview extends PreviewMap {

	private static final long serialVersionUID = -1;
	// TODO ... read these (and preview colors) in from geotopes.json
	private static String[] soil_types = {
		"Alluvial", "Sand Stone", "Granite", "Basalt", 
		"Lime Stone", "Marble",
		"Copper Ore", "Iron Ore", "Silver", "Gold"	
	};
	private static int[][] preview_colors = {
		{143,76,0}, {255,204,153}, {224,224,224}, {128,128,128},
		{229,255,204}, {255,209,204},
		{153,153,0}, {153,0,0}, {192,192,192}, {255,215,0}
	};
	
	public SoilPreview(MapReader r) {
		super("Soil/Minerals", r);
	}
	public void display() {
		
		// assign rainfall-proportional grey-scales to each square
		for(int i = 0; i < rows; i++)
			for(int j = 0; j < cols; j++) {
				colorMap[i][j] = Color.BLACK;
				String s = reader.soilType(i, j);
				if (s == null)
					continue;
				// look up the preview color
				for(int x = 0; x < soil_types.length; x++)
					if (s.equals(soil_types[x])) {
						colorMap[i][j] = new Color(preview_colors[x][0], preview_colors[x][1], preview_colors[x][2]);
						break;
					}
			}
		super.display();
	}
}
