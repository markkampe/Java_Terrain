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

public class OutsideExport extends ExportBase implements ActionListener {
	
	private static final String format = "Outside";
	private JSlider levels;			// number of height levels
	private RangeSlider altitudes;	// ground, hill, mountain
	private RangeSlider depths;		// marsh, shallow, deep
	private JTextField palette;		// tile set description file
	private JButton choosePalette;	// select palette file
	
	private Color colorMap[];		// level to preview color map
	
	// (hard coded) Outside levels
	private static final int DEEP = 0;
	private static final int SHALLOW = 1;
	private static final int PASSABLE = 2;
	private static final int PIT = 3;
	
	// hard coded preview colors
	private Color DEEP_color = new Color(0,0,128);
	private Color SHALLOW_color = Color.BLUE;
	private Color PASSABLE_color = Color.CYAN;
	private static final Color GROUND_color = new Color(205,133,63);
	
	// ranges for land preview colors
	private static final int MAX_PIT_shade = 32;;
	private static final int MIN_MOUND_shade = 160;
	private static final int SHADE_RANGE = 64;
	
	private static final long serialVersionUID = 1L;

	/**
	 * Set up the dialog (base class is fine) and
	 * register the action listeners.
	 * 
	 * @param map ... Map to be exported
	 */
	public OutsideExport(Map map) {
		super("RPGMaker " + format, map);
		
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		
		// create palette selector
		palette = new JTextField(
				parms.Out_palette == null ? format + ".json" : parms.Out_palette);
		JLabel pTitle = new JLabel("Tile Palette", JLabel.CENTER);
		choosePalette = new JButton("Browse");
		pTitle.setFont(fontLarge);
		JPanel p_panel = new JPanel(new GridLayout(2,1));
		p_panel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		p_panel.add(pTitle);
		JPanel p1_panel = new JPanel();
		p1_panel.setLayout(new BoxLayout(p1_panel, BoxLayout.LINE_AXIS));
		p1_panel.add(palette);
		p1_panel.add(Box.createRigidArea(new Dimension(40,0)));
		p1_panel.add(choosePalette);
		p_panel.add(p1_panel);
		controls.add(p_panel);
		
		// create height levels slider
		levels = new JSlider(JSlider.HORIZONTAL, 0, parms.levels_max, parms.dAltLevels);
		levels.setMajorTickSpacing(5);
		levels.setMinorTickSpacing(1);
		levels.setFont(fontSmall);
		levels.setPaintTicks(true);
		levels.setPaintLabels(true);
		JLabel lTitle = new JLabel("Alititude levels");
		lTitle.setFont(fontLarge);
		
		// create altitude RangeSlider
		JPanel aTitle = new JPanel(new GridLayout(1,3));
		JLabel aT1 = new JLabel("Pit");
		aT1.setFont(fontLarge);
		aTitle.add(aT1);
		JLabel aT2 = new JLabel("Ground", JLabel.CENTER);
		aT2.setFont(fontLarge);
		aTitle.add(aT2);
		JLabel aT3 = new JLabel("Hills", JLabel.RIGHT);
		aT3.setFont(fontLarge);
		aTitle.add(aT3);
		
		altitudes = new RangeSlider(0, 100);
		altitudes.setValue(parms.dGroundMin);
		altitudes.setUpperValue(parms.dGroundMax);
		altitudes.setMajorTickSpacing(10);
		altitudes.setMinorTickSpacing(5);
		altitudes.setFont(fontSmall);
		altitudes.setPaintTicks(true);
		altitudes.setPaintLabels(true);

		// create depth RangeSlider
		JPanel dTitle = new JPanel(new GridLayout(1,3));
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
		
		// add sliders to the controls
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10,10,0,15));
		locals.add(new JLabel("    "));
		locals.add(lTitle);
		locals.add(levels);
		locals.add(new JLabel("    "));
		locals.add(aTitle);
		locals.add(altitudes);
		locals.add(new JLabel("    "));
		locals.add(dTitle);
		locals.add(depths);
		controls.add(locals);
		
		// we handle window and button events
		choosePalette.addActionListener(this);
		preview.addActionListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);
		
		// the standard dialog is all we need
		pack();
		setVisible(true);
	}
	
	/**
	 * click events on ACCEPT/PREVIEW/CANCEL/CHOOSE buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == accept && selected) {
			Exporter exporter = new RpgmExporter(palette.getText(), x_points, y_points);
			export(exporter);
			levelMap((RpgmExporter) exporter);
			
			// get the output file name
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				
				// write out the file
				exporter.writeFile(export_file);

				// make the selected values defaults
				parms.map_name = sel_name.getText();
				parms.dGroundMin = altitudes.getValue();
				parms.dGroundMax = altitudes.getUpperValue();
				parms.dWaterMin = depths.getValue();
				parms.dWaterMax = depths.getUpperValue();
				parms.dAltLevels = levels.getValue();
				parms.Out_palette = palette.getText();
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
		} else if (e.getSource() == preview && selected) {
			Exporter exporter = new RpgmExporter(palette.getText(), x_points, y_points);
			export(exporter);
			levelMap((RpgmExporter) exporter);
			exporter.preview(Exporter.WhichMap.HEIGHTMAP, colorMap);
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
		}
	}
	
	/**
	 * generate the percentile maps for land and water
	 * 
	 * @param exporter
	 */
	void levelMap(RpgmExporter exporter) {

		// note the water classifications (by percentile)
		int waterLevels[] = new int[100];
		int low = depths.getValue();
		int high = depths.getUpperValue();
		for(int i = 0; i < 100; i++)
			if (i < low)
				waterLevels[i] = PASSABLE;
			else if (i >= high)
				waterLevels[i] = DEEP;
			else
				waterLevels[i] = SHALLOW;
		
		colorMap = new Color[3 + levels.getValue()];
		colorMap[DEEP] = DEEP_color;
		colorMap[SHALLOW] = SHALLOW_color;
		colorMap[PASSABLE] = PASSABLE_color;
		
		// note the land classifications (by percentile)
		low = altitudes.getValue();
		high = altitudes.getUpperValue();
		int lowLevels =  (int) (((double) low) * (levels.getValue() - 1) / 100.0);
		if (lowLevels == 0)
			lowLevels = 1;
		int highLevels = levels.getValue() - (1 + lowLevels);
		if (highLevels == 0)
			highLevels = 1;
		int groundLevel = PIT + lowLevels;
		
		// figure out class and level for each altitude percentile
		int landLevels[] = new int[100];
		for(int i = 0; i < 100; i++)
			if (i < low) {
				int pitLevel = PIT + (i * lowLevels)/low;
				landLevels[i] = pitLevel;
				int shade = MAX_PIT_shade + ((SHADE_RANGE * (pitLevel - PIT))/lowLevels);
				colorMap[pitLevel] = new Color(shade, shade, shade);
			} else if (i >= high) {
				int moundLevel = groundLevel + 1 + (((i - high) * highLevels)/(100 - high));
				landLevels[i] = moundLevel;
				int shade = MIN_MOUND_shade + ((SHADE_RANGE * (moundLevel - (groundLevel + 1))/highLevels));
				colorMap[moundLevel] = new Color(shade, shade, shade);
			} else {
				landLevels[i] = groundLevel;
				// colorMap[groundLevel] = new Color(GROUND_shade, GROUND_shade, GROUND_shade);
				colorMap[groundLevel] = GROUND_color;
			}
		exporter.levelMap(landLevels, waterLevels);
	}
}
