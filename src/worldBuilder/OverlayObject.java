package worldBuilder;

class OverlayObject {
	public String name;	// tile name
	public int height;	// height (in tiles)
	public int width;	// width (in tiles)

	// attributes of tile definitions
	public int group;	// minimum height group
	public int slope;	// minimum slope (%)
	
	/**
	 * define a new OverlayObject
	 * @param name		name when placed
	 * @param height	height (in tiles)
	 * @param width		width (in tiles)
	 */
	public OverlayObject(String name, int height, int width) {
		this.name = name;
		this.height = height;
		this.width = width;
		this.group = 0;
		this.slope = 0;
	}
	
	/**
	 * print out an object description (for debug)
	 * 
	 * @param prefix ... string to precede the output
	 */
	public void dump(String prefix) {
		System.out.print(prefix + name);
		System.out.print(":\t" + height + "x" + width);
		System.out.print(", group=" + group);
		System.out.print(", slope=" + slope + "%");
		System.out.print("\n");
	}
}