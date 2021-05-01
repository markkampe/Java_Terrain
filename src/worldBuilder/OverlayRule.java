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

	/** RPGMaker tile set	*/
	public static int tile_size;
	/** dimensions (if this is a stamp)	*/
	public int height, width;
	/** range of acceptable Z percentages	*/
	public int a_min, a_max;
	/** range of acceptable depth percentages	*/
	public int d_min, d_max;

	/** preview icon image	*/
	public BufferedImage icon;

	// save extended parameters to store in next factory-instantiated object
	private static int n_height = 1, n_width = 1;
	private static int n_a_min = 0, n_a_max = 100;
	private static int n_d_min = 0, n_d_max = 0;
	private static String n_icon_file = null;

	/**
	 * create a new rule
	 * @param name of this rule
	 */
	public OverlayRule(String name) {
		super(name);
		
		// initialize our extended attributes
		this.height = 1; this.width = 1;
		this.a_min = 0; this.a_max = 100;
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
		newRule.a_min = n_a_min;
		newRule.a_max = n_a_max;
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
		n_a_min = 0; n_a_max = 100;
		n_d_min = 0; n_d_max = 0;
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
		case "a%":	// altitude percentile
			if (limit.equals("max"))
				n_a_max = (int) value;
			else
				n_a_min = (int) value;
			return;
		case "d%":	// depth percentile
			if (limit.equals("max"))
				n_d_max = (int) value;
			else
				n_d_min = (int) value;
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
		
		if (a_min > 0 || a_max < 100)
			System.out.println(prefix + indent + "z:       " + a_min + "-" + a_max);
		if (d_min > 0 || d_max < 100)
			System.out.println(prefix + indent + "d:       " + d_min + "-" + d_max);
		if (height > 1 || width > 1)
			System.out.println(prefix + indent + "stamp:   " + width + "x" + height);
	}
}
