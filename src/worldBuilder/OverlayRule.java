package worldBuilder;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * An extension of ResourceRules that includes information about RPGM tiles 
 */
public class OverlayRule extends ResourceRule {
	
	private static final String DEFAULT_ICONS = "/icons";
	private static final double MAX_SLOPE = 10.0;

	/** RPGMaker tile set	*/
	public static int tile_size;
	/** dimensions (if this is a stamp)	*/
	public int height, width;
	/** range of acceptable Z percentages	*/
	public int z_min, z_max;
	/** range of acceptable depth percentages	*/
	public int d_min, d_max;
	/** range of acceptable slopes	*/
	public double s_min, s_max;
	/** preview icon image	*/
	public BufferedImage icon;

	// save extended parameters to store in next factory-instantiated object
	private static int n_height = 1, n_width = 1;
	private static int n_z_min = 0, n_z_max = 100;
	private static int n_d_min = 0, n_d_max = 0;
	private static double n_s_min = 0.0, n_s_max = MAX_SLOPE;
	private static String n_icon_file = null;

	/**
	 * create a new rule
	 * @param name of this rule
	 */
	public OverlayRule(String name) {
		super(name);
		
		// initialize our extended attributes
		this.height = 1; this.width = 1;
		this.z_min = 0; this.z_max = 100;
		this.s_min = 0.0; this.s_max = MAX_SLOPE;
		this.d_min = 0; this.d_max = 0;
		this.icon = null;
	}
	
	/**
	 * Factory method (to permit subclass instantiation by ResourceRule.loadFile)
	 * @param name of this rule
	 * 
	 * Note: this method is called by ResourceRule.read() when the rule 
	 * 		 reading is complete, which means that any extended attributes
	 * 		 have already been set (in the static save variables).  We 
	 * 		 copy those into the new rule, and then reinitialize them.
	 */
	public OverlayRule factory(String name) {
		OverlayRule newRule = new OverlayRule(name);
		
		// copy extended attributes into the new rule
		newRule.height = n_height;
		newRule.width = n_width;
		newRule.z_min = n_z_min;
		newRule.z_max = n_z_max;
		newRule.s_min = n_s_min;
		newRule.s_max = n_s_max;
		newRule.d_min = n_d_min;
		newRule.d_max = n_d_max;
		
		// if there is an icon file, read it in
		if (n_icon_file != null && !n_icon_file.equals("")) {
			String icon_file = n_icon_file;
			try {
				if (n_icon_file.charAt(0) != '/') {
					icon_file = DEFAULT_ICONS + "/" + n_icon_file;
					InputStream s = getClass().getResourceAsStream(icon_file);
					if (s != null)
						newRule.icon = ImageIO.read(s);
					else
						throw new IOException("nonesuch");
				} else
					newRule.icon = ImageIO.read(new File(icon_file));
			} catch (IOException x) {
				System.err.println("ERROR: unable to read icon file " + icon_file);
			}
		}

		// reset their values for the next rule
		n_height = 1; n_width = 1;
		n_z_min = 0; n_z_max = 100;
		n_d_min = 0; n_d_max = 0;
		n_s_min = 0.0; n_s_max = MAX_SLOPE;
		n_icon_file = null;
		
		// and return the newly fabricated object (to ResourceRule.read)
		return newRule;
	}
	
	/**
	 * called from ResourceRule.loadFile ... set an extended attribute (string value)
	 * @param name of the attribute being set
	 * @param value to be set 
	 */
	public void set_attribute(String name, String value) {
		switch (name) {
		case "stamp":	// width x height
			int x = value.indexOf('x');
			n_width = Integer.parseInt(value.substring(0,x));
			n_height = Integer.parseInt(value.substring(x+1));
			return;
		case "icon":	// file name
			n_icon_file = value;
			return;
		}
		System.err.println(ruleFile + ": Unrecognized attribute (" + name + "=\"" + value + "\")");
	}
	
	/**
	 * called from ResourceRule.loadFile ... set an extended attribute (integer value)
	 * @param name of the attribute being set
	 * @param value to be set
	 */
	public void set_attribute(String name, int value) {
		switch (name) {
		case "tile_size":
			tile_size = value;	// this is for the entire rule set
			return;
		}
		System.err.println(ruleFile + ": Unrecognized attribute: (" + name + "=" + value + ")");
	}
	
	/**
	 * called from ResourceRule.loadFile ... set an extended attribute (min/max double value)
	 * @param name of the attribute being set
	 * @param limit (min or max)
	 * @param value to be set
	 */
	public void set_range(String name, String limit, double value) {
		switch(name) {
		case "z":
			if (limit.equals("max"))
				n_z_max = (int) value;
			else
				n_z_min = (int) value;
			return;
		case "d":
			if (limit.equals("max"))
				n_d_max = (int) value;
			else
				n_d_min = (int) value;
			return;
		case "slope":
			if (limit.equals("max"))
				n_s_max = value;
			else
				n_s_min = value;
			return;
		}
		System.err.println(ruleFile + ": Unrecognized RPGMRule attribute (" + name + "." + limit + "=" + value + ")");
	}
	
	/**
	 * dump out the extended field attributes (for debugging)
	 */
	public void dump(String prefix) {
		// start with the standard info
		super.dump(prefix);
		String indent = "      ";
		
		if (z_min > 0 || z_max < 100)
			System.out.println(prefix + indent + "z:       " + z_min + "-" + z_max);
		if (d_min > 0 || d_max < 100)
			System.out.println(prefix + indent + "d:       " + d_min + "-" + d_max);
		if (s_min > 0 || s_max < MAX_SLOPE)
			System.out.println(prefix + indent + "slope:   " + s_min + "-" + s_max);
		if (height > 1 || width > 1)
			System.out.println(prefix + indent + "stamp:   " + width + "x" + height);
	}
}
