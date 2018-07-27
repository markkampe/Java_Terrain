package worldBuilder;

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
	
	private JSlider levels;			// number of height levels
	private RangeSlider altitudes;	// ground, hill, mountain
	private RangeSlider depths;		// marsh, shallow, deep
	private JTextField palette;		// tile set description file
	private JButton choosePalette;	// select palette file
	
	private static final long serialVersionUID = 1L;

	/**
	 * Set up the dialog (base class is fine) and
	 * register the action listeners.
	 * 
	 * @param map ... Map to be exported
	 */
	public OutsideExport(Map map) {
		super("RPGMaker Outside", map);
		
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		
		// create palette selector
		// FIX use saved palette
		palette = new JTextField("Outside.json");
		JLabel pTitle = new JLabel("Tile Palette", JLabel.CENTER);
		choosePalette = new JButton("Choose");
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
		altitudes = new RangeSlider(0, 100);
		altitudes.setValue(parms.dGroundMin);
		altitudes.setUpperValue(parms.dGroundMax);
		altitudes.setMajorTickSpacing(10);
		altitudes.setMinorTickSpacing(5);
		altitudes.setFont(fontSmall);
		altitudes.setPaintTicks(true);
		altitudes.setPaintLabels(true);
		JLabel aTitle = new JLabel("Ground level (vs pit/hill)", JLabel.CENTER);
		aTitle.setFont(fontLarge);

		// create depth RangeSlider
		depths = new RangeSlider(0, 100);
		depths.setValue(parms.dWaterMin);
		depths.setUpperValue(parms.dWaterMax);
		depths.setMajorTickSpacing(10);
		depths.setMinorTickSpacing(5);
		depths.setFont(fontSmall);
		depths.setPaintTicks(true);
		depths.setPaintLabels(true);
		JLabel dTitle = new JLabel("Shallow water (vs marsh/deep)", JLabel.CENTER);
		dTitle.setFont(fontLarge);
		
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
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				Exporter exporter = new RpgmExporter(export_file, palette.getText(), x_points, y_points);
				
				// FIX ... we need special processing
				export(exporter, export_file);
				
				// make the selected values defaults
				parms.map_name = sel_name.getText();
				parms.dGroundMin = altitudes.getValue();
				parms.dGroundMax = altitudes.getUpperValue();
				parms.dWaterMin = depths.getValue();
				parms.dWaterMax = depths.getUpperValue();
				parms.dAltLevels = levels.getValue();
				// FIX save palette
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
		} else if (e.getSource() == preview) {
			System.out.println("PREVIEW NOT IMPLEMENTED"); // FIX
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
}
