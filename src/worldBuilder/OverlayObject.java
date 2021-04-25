package worldBuilder;

import java.awt.image.BufferedImage;

class OverlayObject {
	public String name;	// tile name (of the entry)
	public String id;	// tile ID (for exporter)
	public int height;	// height (in tiles)
	public int width;	// width (in tiles)

	// attributes of tile definitions
	public int z_min;			// minimum altitude (%z)
	public int z_max;			// maximum altitude (%z)
	public double slope_min;	// min slope (dz/dx)
	public double slope_max;	// max slope (dz/dx)
	public BufferedImage icon;	// preview icon;
	
	/**
	 * define a new OverlayObject
	 * @param name		name when placed
	 * @param height	height (in tiles)
	 * @param width		width (in tiles)
	 */
	public OverlayObject(String name, int height, int width) {
		this.name = name;
		this.id = null;
		this.height = height;
		this.width = width;
		this.slope_min = 0.0;
		this.slope_max = 10.0;
		this.z_min = 0;
		this.z_max = 100;
	}
	
	/**
	 * print out an object description (for debug)
	 * 
	 * @param prefix ... string to precede the output
	 */
	public void dump(String prefix) {
		System.out.print(prefix + name);
		if (id != null)
			System.out.print(", id=" + id);
		System.out.print("(" + height + "x" + width + ")"); 
		System.out.print(", z=" + z_min + "-" + z_max + "%");
		System.out.print(", slope=" + slope_min + "-" + slope_max);
		System.out.print("\n");
	}
}
