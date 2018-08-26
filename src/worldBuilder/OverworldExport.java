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
import javax.swing.JTextField;

public class OverworldExport extends ExportBase implements ActionListener {
	
	private static final String format = "Overworld";
	private RangeSlider altitudes;	// ground, hill, mountain
	private RangeSlider slopes;		// ground, hill, mountain
	private RangeSlider depths;		// passable, shallow, deep
	private JTextField palette;		// tile set description file
	private JButton choosePalette;	// select palette file
	
	// level to color mapping for land form previews
	Color previewColors[] = {
			Color.BLACK,			// NONE
			new Color(0,0,128),		// DEEP water
			Color.BLUE,				// SHALLOW water
			Color.CYAN,				// PASSABLE wetland/creek
			Color.DARK_GRAY,		// PIT
			Color.GREEN,			// LOWLANDS
			Color.GRAY,				// HILLS
			Color.LIGHT_GRAY		// MOUNTAINS
	};
	
	// level to terrain type map for Overworld is static
	private int levelToTerrain[] = {
			TerrainType.NONE,
			TerrainType.DEEP_WATER, TerrainType.SHALLOW_WATER, TerrainType.PASSABLE_WATER,
			TerrainType.PIT,
			TerrainType.GROUND, TerrainType.HILL, TerrainType.MOUNTAIN			
	};
	
	private static final long serialVersionUID = 1L;

	/**
	 * Set up the dialog (base class is fine) and
	 * register the action listeners.
	 * 
	 * @param map ... Map to be exported
	 */
	public OverworldExport(Map map) {
		super("RPGMaker " + format, map);
		
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		
		// create palette selector		
		palette = new JTextField(
				parms.OW_palette == null ? format + ".json" : parms.OW_palette);
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
		
		// create altitude RangeSlider
		JPanel aTitle = new JPanel(new GridLayout(1,3));
		JLabel aT1 = new JLabel("Ground");
		aT1.setFont(fontLarge);
		aTitle.add(aT1);
		JLabel aT2 = new JLabel("Hills", JLabel.CENTER);
		aT2.setFont(fontLarge);
		aTitle.add(aT2);
		JLabel aT3 = new JLabel("Mountains", JLabel.RIGHT);
		aT3.setFont(fontLarge);
		aTitle.add(aT3);
		
		altitudes = new RangeSlider(0, 100);
		altitudes.setValue(parms.dHillMin);
		altitudes.setUpperValue(parms.dHillMax);
		altitudes.setMajorTickSpacing(10);
		altitudes.setMinorTickSpacing(5);
		altitudes.setFont(fontSmall);
		altitudes.setPaintTicks(true);
		altitudes.setPaintLabels(true);
		
		// create slope RangeSlider
		JPanel sTitle = new JPanel(new GridLayout(1,3));
		JLabel sT1 = new JLabel("Ground");
		sT1.setFont(fontLarge);
		sTitle.add(sT1);
		JLabel sT2 = new JLabel("Hills", JLabel.CENTER);
		sT2.setFont(fontLarge);
		sTitle.add(sT2);
		JLabel sT3 = new JLabel("Mountains", JLabel.RIGHT);
		sT3.setFont(fontLarge);
		sTitle.add(sT3);
		
		slopes = new RangeSlider(0, 100);
		slopes.setValue(parms.dSlopeMin);
		slopes.setUpperValue(parms.dSlopeMax);
		slopes.setMajorTickSpacing(10);
		slopes.setMinorTickSpacing(5);
		slopes.setFont(fontSmall);
		slopes.setPaintTicks(true);
		slopes.setPaintLabels(true);
				
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
		locals.add(aTitle);
		locals.add(altitudes);
		JLabel l = new JLabel("Terrain Altitude (percentile)", JLabel.CENTER);
		l.setFont(fontSmall);
		locals.add(l);
		locals.add(new JLabel("    "));
		locals.add(sTitle);
		locals.add(slopes);
		l = new JLabel("Terrain Slope (percentile)", JLabel.CENTER);
		l.setFont(fontSmall);
		locals.add(l);
		locals.add(new JLabel("    "));
		locals.add(dTitle);
		locals.add(depths);
		l = new JLabel("Water Depth (percentile)", JLabel.CENTER);
		l.setFont(fontSmall);
		locals.add(l);
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
			OverworldTiler exporter = new OverworldTiler(palette.getText(), x_points, y_points);
			export(exporter);
			levelMap(exporter);
			
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				
				exporter.writeFile(export_file);
				
				// make the selected values defaults
				parms.map_name = sel_name.getText();
				parms.dHillMin = altitudes.getValue();
				parms.dHillMax = altitudes.getUpperValue();
				parms.dSlopeMin = slopes.getValue();
				parms.dSlopeMax = slopes.getUpperValue();
				parms.dWaterMin = depths.getValue();
				parms.dWaterMax = depths.getUpperValue();
				parms.OW_palette = palette.getText();
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
		} else if (e.getSource() == preview && selected) {
			OverworldTiler exporter = new OverworldTiler(palette.getText(), x_points, y_points);
			export(exporter);
			levelMap(exporter);
			exporter.preview(Exporter.WhichMap.HEIGHTMAP, previewColors);
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
	void levelMap(OverworldTiler exporter) {
		// initialize altitude percentile to terrain class map
		int landLevels[] = new int[100];
		int low = altitudes.getValue();
		int high = altitudes.getUpperValue();
		for(int i = 0; i < 100; i++)
			if (i < low)
				landLevels[i] = TerrainType.GROUND;
			else if (i >= high)
				landLevels[i] = TerrainType.MOUNTAIN;
			else
				landLevels[i] = TerrainType.HILL;
		
		// initialize depth percentile to terrain class map
		int waterLevels[] = new int[100];
		low = depths.getValue();
		high = depths.getUpperValue();
		for(int i = 0; i < 100; i++)
			if (i < low)
				waterLevels[i] = TerrainType.PASSABLE_WATER;
			else if (i >= high)
				waterLevels[i] = TerrainType.DEEP_WATER;
			else
				waterLevels[i] = TerrainType.SHALLOW_WATER;
		
		// initialize slope percentile to terrain class map
		int slopeLevels[] = new int[100];
		low = slopes.getValue();
		high = slopes.getUpperValue();
		for(int i = 0; i < 100; i++)
			if (i < low)
				slopeLevels[i] = TerrainType.GROUND;
			else if (i >= high)
				slopeLevels[i] = TerrainType.MOUNTAIN;
			else
				slopeLevels[i] = TerrainType.HILL;
		
		// the level to terrain class map is hard-coded
		exporter.levelMap(landLevels, waterLevels, slopeLevels, levelToTerrain);
	}
}
