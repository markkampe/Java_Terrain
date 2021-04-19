package worldBuilder;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to collect the parameters for an RPGMaker export.
 */
public class RPGMexport extends ExportBase implements ActionListener, ChangeListener{

	private String format;		// export type/ rules file
	
	private RPGMTiler tiler = null;
	private boolean exported = false;
	private boolean levelsChanged;

	// dialog control options
	private boolean need_palette; 	// tile palette selection
	private boolean need_levels; 	// slider for # levels
	private boolean need_alt_3; 	// slider for ground/hill/mountain
	private boolean need_alt_n; 	// slider for pit/ground/mound levels
	private boolean need_plateau; 	// slider for plateau slope threshold
	private boolean need_depths; 	// slider for passable/shallow/deep
	private boolean need_flora_3;	// slider for tall grass/brush/trees

	// control widgets
	private JSlider levels; 	// number of height levels
	private RangeSlider altitudes; // ground, hill, mountain OR pit, ground, mound
	private JSlider slopes; 	// plateau, hill/mountain
	private RangeSlider depths; // marsh, shallow, deep
	private RangeSlider flora_3;	// types of plant cover
	private JTextField palette; // tile set description file
	private JButton choosePalette; // select palette file

	private Color[] colorTopo; // level to preview color map
	private Color[] colorFlora;	// flora class to preview color map

	// preview colors
	private static final Color GROUND_COLOR = new Color(102,51,0);
	private static final int MIN_WATER_SHADE = 96; // blue
	private static final int MIN_LOW_SHADE = 32; // dark grey
	private static final int MIN_MID_SHADE = 128; // medium grey
	private static final int MIN_HIGH_SHADE = 160; // light grey
	private static final int SHADE_RANGE = 64; // total range (per TerrainType)
	
	// indices into the tileRules list
	private static final int OW_TILES = 0;
	private static final int OUT_TILES = 1;

	private static final int EXPORT_DEBUG = 2;
	
	private static final long serialVersionUID = 1L;

	/**
	 * figure out what export control widgets we need and create them
	 * 
	 * @param map ... Map to be exported
	 */
	public RPGMexport(String format, Map map) {
		super("RPGMaker " + format, map, 
			  format.equals("Outside") ? 1 : 10,
			  format.equals("Outside") ? 1000 : 100000,
			  Map.Selection.RECTANGLE);
		this.format = format;

		// figure out which export control widgets we need
		if (format.equals("Outside")) { // many terrain levels
			need_levels = true;
			need_alt_n = true;
			need_alt_3 = false;
			need_depths = true;
			need_plateau = false;
		} else { // Overworld ... few levels, but more types
			need_levels = false;
			need_alt_3 = true;
			need_alt_n = false;
			need_plateau = true;	// turn mountains into plateaus
			need_depths = true;
		}
		need_palette = true;
		need_flora_3 = true;

		// add our controls to those in the base class
		create_GUI();
		setVisible(true);
		
		colorFlora = map.getFloraColors();
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("new RPGMexport(" + format + ")");
	}

	/**
	 * create the control widgets and register listeners for this export
	 */
	private void create_GUI() {
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		boolean overworld = this.format.equals("Overworld");

		if (need_palette) { // create palette selector in main panel
			String rulesFile = parms.exportRules.get(overworld ? OW_TILES : OUT_TILES);
			palette = new JTextField(rulesFile);
			JLabel pTitle = new JLabel("Tile Palette", JLabel.CENTER);
			choosePalette = new JButton("Browse");
			pTitle.setFont(fontLarge);
			JPanel p_panel = new JPanel(new GridLayout(2, 1));
			p_panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
			p_panel.add(pTitle);
			JPanel p1_panel = new JPanel();
			p1_panel.setLayout(new BoxLayout(p1_panel, BoxLayout.LINE_AXIS));
			p1_panel.add(palette);
			p1_panel.add(Box.createRigidArea(new Dimension(40, 0)));
			p1_panel.add(choosePalette);
			p_panel.add(p1_panel);
			controls.add(p_panel);

			choosePalette.addActionListener(this);
		}

		// create a local panel for the export-type controls
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));		// start out with from-the-model hydration

		if (need_levels) { // create height levels slider
			levels = new JSlider(JSlider.HORIZONTAL, 0, parms.levels_max, parms.dAltLevels);
			levels.setMajorTickSpacing(5);
			levels.setMinorTickSpacing(1);
			levels.setFont(fontSmall);
			levels.setPaintTicks(true);
			levels.setPaintLabels(true);
			JLabel lTitle = new JLabel("Alititude levels");
			lTitle.setFont(fontSmall);

			// add this to the local panel
			locals.add(new JLabel("    "));
			locals.add(levels);
			locals.add(lTitle);
			
			levels.addChangeListener(this);
		}

		if (need_alt_3 || need_alt_n) { // create altitude slider
			JPanel aTitle = new JPanel(new GridLayout(1, 3));
			JLabel aT1 = new JLabel(need_alt_n ? "Pit" : "Ground");
			aT1.setFont(fontLarge);
			aTitle.add(aT1);
			JLabel aT2 = new JLabel(need_alt_n ? "Ground" : "Hills", JLabel.CENTER);
			aT2.setFont(fontLarge);
			aTitle.add(aT2);
			JLabel aT3 = new JLabel(need_alt_n ? "Highlands" : "Mountains", JLabel.RIGHT);
			aT3.setFont(fontLarge);
			aTitle.add(aT3);

			altitudes = new RangeSlider(0, 100);
			altitudes.setValue(need_alt_3 ? parms.dGroundMin : parms.dHillMin);
			altitudes.setUpperValue(need_alt_3 ? parms.dGroundMax : parms.dHillMax);
			altitudes.setMajorTickSpacing(10);
			altitudes.setMinorTickSpacing(5);
			altitudes.setFont(fontSmall);
			altitudes.setPaintTicks(true);
			altitudes.setPaintLabels(true);

			JLabel l = new JLabel("Terrain Altitude (percentile)");
			l.setFont(fontSmall);

			// add this to the local panel
			locals.add(new JLabel("    "));
			locals.add(aTitle);
			locals.add(altitudes);
			locals.add(l);
			
			altitudes.addChangeListener(this);
		}

		if (need_plateau) { // create slope range slider
			JPanel sTitle = new JPanel(new GridLayout(1, 3));
			JLabel sT1 = new JLabel("Plateau");
			sT1.setFont(fontLarge);
			sTitle.add(sT1);
			JLabel sT3 = new JLabel("Hills/Mountains", JLabel.RIGHT);
			sT3.setFont(fontLarge);
			sTitle.add(sT3);

			slopes = new JSlider(0, 100);
			slopes.setValue(parms.dSlopeMin);
			slopes.setMajorTickSpacing(10);
			slopes.setMinorTickSpacing(5);
			slopes.setFont(fontSmall);
			slopes.setPaintTicks(true);
			slopes.setPaintLabels(true);

			JLabel l = new JLabel("Terrain Slope (percentile)");
			l.setFont(fontSmall);

			// add this to the local panel
			locals.add(new JLabel("    "));
			locals.add(sTitle);
			locals.add(slopes);
			locals.add(l);
			
			slopes.addChangeListener(this);
		}

		if (need_depths) { // create depth RangeSlider
			JPanel dTitle = new JPanel(new GridLayout(1, 3));
			JLabel dT1 = new JLabel("Passable");
			dT1.setFont(fontLarge);
			dTitle.add(dT1);
			JLabel dT2 = new JLabel("Shallow", JLabel.CENTER);
			dT2.setFont(fontLarge);
			dTitle.add(dT2);
			JLabel dT3 = new JLabel("Deep", JLabel.RIGHT);
			dT3.setFont(fontLarge);
			dTitle.add(dT3);

			depths = new RangeSlider(0, 100);
			depths.setValue(parms.dWaterMin);
			depths.setUpperValue(parms.dWaterMax);
			depths.setMajorTickSpacing(10);
			depths.setMinorTickSpacing(5);
			depths.setFont(fontSmall);
			depths.setPaintTicks(true);
			depths.setPaintLabels(true);

			JLabel l = new JLabel("Water Depth (percentile)");
			l.setFont(fontSmall);

			// add this to the local panel
			locals.add(new JLabel("    "));
			locals.add(dTitle);
			locals.add(depths);
			locals.add(l);
			
			depths.addChangeListener(this);
		}
		
		if (need_flora_3) { // create flora RangeSlider
			locals.add(new JLabel("     "));
			JPanel fTitle = new JPanel(new GridLayout(1, 3));
			JLabel fT1 = new JLabel("Tall Grass");
			fT1.setFont(fontLarge);
			fTitle.add(fT1);
			JLabel fT2 = new JLabel("Brush", JLabel.CENTER);
			fT2.setFont(fontLarge);
			fTitle.add(fT2);
			JLabel fT3 = new JLabel("Trees", JLabel.RIGHT);
			fT3.setFont(fontLarge);
			fTitle.add(fT3);

			flora_3 = new RangeSlider(0, 100);
			flora_3.setValue(parms.dFloraMin);
			flora_3.setUpperValue(parms.dFloraMax);
			flora_3.setMajorTickSpacing(10);
			flora_3.setMinorTickSpacing(5);
			flora_3.setFont(fontSmall);
			flora_3.setPaintTicks(true);
			flora_3.setPaintLabels(true);

			JLabel l = new JLabel("Flora Distribution (percentile)");
			l.setFont(fontSmall);

			// add this to the local panel
			locals.add(new JLabel("    "));
			locals.add(fTitle);
			locals.add(flora_3);
			locals.add(l);
			
			flora_3.addChangeListener(this);
		}
		
		// add the local panel to the main panel
		controls.add(locals);
		pack();

		// we handle window and button events
		previewT.addActionListener(this);
		previewF.addActionListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);
	}

	/**
	 * record slider changes and note a re-export will be required
	 */
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
	
		if (source == levels || source == altitudes || source == slopes || source == depths) {
			levelsChanged = true;
		}
	}
	
	/**
	 * click events on ACCEPT/PREVIEW/CANCEL/CHOOSE buttons
	 */
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
			return;
		} else if (e.getSource() == choosePalette) {
			FileDialog d = new FileDialog(this, "Tile Palette", FileDialog.LOAD);
			d.setFile(palette.getText());
			d.setVisible(true);
			String palette_file = d.getFile();
			if (palette_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					palette_file = dir + palette_file;
				palette.setText(palette_file);
			}
			return;
		} 
		
		// make sure we have a ready Tiler
		if (tiler == null || newSelection) {
				tiler = new RPGMTiler(palette.getText(), x_points, y_points);
				exported = false;
		}
		if (!exported) {
			double trees = 1.0 - (flora_3.getUpperValue()/100.0);
			double brush = (flora_3.getUpperValue() - flora_3.getValue())/100.0;
			tiler.floraQuotas(1.0, brush, trees);
			export(tiler);
			
			exported = true;
			newSelection = false;
			levelsChanged = true;
		}
		
		if (levelsChanged) {
			levelMap();
			levelsChanged = false;
		}
		
		// topo previews do not require hydration and flora updates
		if (e.getSource() == previewT && selected) {
			tiler.preview(Exporter.WhichMap.HEIGHTMAP, colorTopo);
			return;
		}
		
		if (e.getSource() == previewF && selected) {
			tiler.preview(Exporter.WhichMap.FLORAMAP, colorFlora);
			return;
		}
		
		if (e.getSource() == accept && selected) {
			// get the output file name
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText() + ".json");
			if (parms.project_dir != null)
				d.setDirectory(parms.project_dir);
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null) {
					export_file = dir + export_file;
					parms.project_dir = dir;
				}

				// write out the file
				tiler.writeFile(export_file);

				// make the selected values defaults
				parms.map_name = sel_name.getText();
				if (altitudes != null)
					if (need_alt_3) {
						parms.dGroundMin = altitudes.getValue();
						parms.dGroundMax = altitudes.getUpperValue();
					} else if (need_alt_n) {
						parms.dHillMin = altitudes.getValue();
						parms.dHillMax = altitudes.getUpperValue();
					}

				if (depths != null) {
					parms.dWaterMin = depths.getValue();
					parms.dWaterMax = depths.getUpperValue();
				}
				if (levels != null)
					parms.dAltLevels = levels.getValue();
				if (palette != null)
					parms.Out_palette = palette.getText();
				
				if (flora_3 != null) {
					parms.dFloraMin = flora_3.getValue();
					parms.dFloraMax = flora_3.getUpperValue();
				}
			}
		}
	}

	/**
	 * generate the percentile to TerrainType maps for this export
	 *
	 *  slider inputs: levels total # levels (Outside) depths
	 *                 passable/shallow/deep altitudes ground/hill/mountain
	 *                 (Overworld) or pit/ground/mound (Outside) slopes
	 *                 ground/hill/mountain (Overworld)
	 *
	 *       TODO: better color ranges for terrain types		
	 */
	void levelMap() {

		// maps created by this method
		int depthMap[]; // depth pctile to terrain level
		int altMap[]; // alt pctile to terrain level
		int slopeMap[]; // slope pctile to terrain level
		int typeMap[]; // terrain level to TerrainType

		// number of levels of each type
		int totLevels; // total # of terrain levels
		int waterLevels; // # of water levels
		int lowLevels; // # of pit/ground levels
		int midLevels; // # of ground/hill levels
		int highLevels; // # of mound/mountain levels

		boolean have_pits = false;

		// figure out how many levels we have of which types
		if (levels == null) { // Overworld
			waterLevels = 3;// DEEP/SHALLOW/PASSABLE
			lowLevels = 1;	// GROUND
			midLevels = 1;	// HILL
			highLevels = 1;	// MOUNTAIN
		} else { // Outside
			waterLevels = 3;// DEEP/SHALLOW/PASSABLE
			int landLevels = levels.getValue();
			lowLevels = 0; // TODO - tiler can't yet do PITs
			midLevels = 1; // all GROUND is at the same level
			highLevels = landLevels - (lowLevels + midLevels);

			have_pits = true; // at least in principle
		}
		totLevels = waterLevels + lowLevels + midLevels + highLevels;

		// figure out the base level for each TerrainType
		int water_base = 0;
		int low_base = water_base + waterLevels;
		int mid_base = low_base + lowLevels;
		int high_base = mid_base + midLevels;

		// create and initialize the depth percentile->level map
		// NOTE: everything assumes ONLY three depth levels
		if (depths != null) {
			depthMap = new int[100];
			int low = depths.getValue();
			int high = depths.getUpperValue();
			for (int i = 0; i < 100; i++)
				depthMap[i] = (i >= high) ? 0 : (i <= low) ? 2 : 1;
		} else
			depthMap = null;

		// create and initialize the altitude percentile->level map
		if (altitudes != null) {
			altMap = new int[100];
			// figure out where the cut-offs are
			int low = (lowLevels == 0) ? 0 : altitudes.getValue();
			int high = altitudes.getUpperValue();

			// figure out the base level for each of the three groups
			for (int i = 0; i < 100; i++)
				if (i >= high) // one of the high levels
					altMap[i] = high_base + (((i - high) * highLevels) / (100 - high));
				else if (i >= low) // one of the mid levels
					altMap[i] = mid_base + (((i - low) * midLevels) / (high - low));
				else // one of the low levels
					altMap[i] = low_base + ((i * lowLevels) / low);
		} else
			altMap = null;

		// create and initialize the slope percentile->level map
		if (slopes != null) {
			slopeMap = new int[100];
			int flat = slopes.getValue();

			// figure out the base level for each of the three groups
			for (int i = 0; i < 100; i++)
				if (i >= flat) 	// too steep for a plateau
					slopeMap[i] = high_base + (((i - flat) * highLevels) / (100 - flat));
				else // this is a plateau
					slopeMap[i] = low_base + ((i * lowLevels) / flat);
		} else
			slopeMap = null;

		// create the terrain level to TerrainType/color maps
		typeMap = new int[totLevels];
		colorTopo = new Color[totLevels];
		int level = water_base;

		// water related types and colors (shades of blue)
		int shade = MIN_WATER_SHADE; // currently all shades of blue
		int delta = SHADE_RANGE;	// big change per level
		for (int i = 0; i < waterLevels; i++) {
			typeMap[level] = TerrainType.DEEP_WATER + i;
			colorTopo[level] = new Color(0, 0, shade);
			shade += delta;
			level++;
		}

		// low-land related types and colors
		if (lowLevels > 0) {
			shade = MIN_LOW_SHADE;
			delta = SHADE_RANGE / lowLevels;
			for (int i = 0; i < lowLevels; i++) {
				if (have_pits) {	// one dark gray
					typeMap[level] = TerrainType.PIT;
					colorTopo[level] = new Color(shade, shade, shade);
				} else {			// one dark brown
					typeMap[level] = TerrainType.GROUND;
					colorTopo[level] = GROUND_COLOR;
				}
				shade += delta;
				level++;
			}
		}

		// mid-land related types and colors
		shade = MIN_MID_SHADE;
		delta = SHADE_RANGE / midLevels;
		for (int i = 0; i < midLevels; i++) {
			if (have_pits) {	// one dark brown
				typeMap[level] = TerrainType.GROUND;
				colorTopo[level] = GROUND_COLOR;
			} else {			// one dark gray
				typeMap[level] = TerrainType.HILL;
				colorTopo[level] = new Color(shade, shade, shade);
			}
			shade += delta;
			level++;
		}

		// high-land related types and colors
		shade = MIN_HIGH_SHADE;
		delta = SHADE_RANGE / highLevels;
		for (int i = 0; i < highLevels; i++) {
			typeMap[level] = have_pits ? TerrainType.HILL : TerrainType.MOUNTAIN;
			colorTopo[level] = new Color(shade, shade, shade);	// shades of light gray
			shade += delta;
			level++;
		}

		// compute a terrain level for every square
		RPGMLeveler leveler = new RPGMLeveler();
		int[][] levelMap = leveler.getLevels(tiler, altMap, depthMap, slopeMap);
		tiler.levelMap(levelMap, typeMap);
	}
}
