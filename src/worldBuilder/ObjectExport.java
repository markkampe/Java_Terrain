package worldBuilder;

import java.awt.FileDialog;
import java.awt.event.*;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Dialog to collect information for a experimental object export
 */
public class ObjectExport extends ExportBase implements ActionListener {
	
	private static final int MIN_TILE = 100;		// 100m/tile
	private static final int MAX_TILE = 10000;		// 10km/tile
	private static final long serialVersionUID = 1L;
	
	private ObjectExporter exporter = null;
	private boolean exported = false;

	// additional dialog widgets for this export
	private JTextField palette;
	private JButton choosePalette;

	/**
	 * create additional widgets and action listeners
	 * 
	 * @param map ... Map to be exported
	 */
	public ObjectExport(Map map) {
		super("Object Overlay", map, MIN_TILE, MAX_TILE, Map.Selection.RECTANGLE);
		
		// additional widgets for overlay export
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);

		// we need a list of overlay objects w/heights and footprints
		palette = new JTextField(parms.overlay_objects);
		if (parms.overlay_objects != null)
			palette.setText(parms.overlay_objects);
		JLabel pTitle = new JLabel("Overlay Objects", JLabel.CENTER);
		pTitle.setFont(fontLarge);
		choosePalette = new JButton("Browse");
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

		// we also handle standard window and button events
		previewT.addActionListener(this);
		previewF.addActionListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);

		// put up the dialog
		pack();
		setVisible(true);
	}
	
	/**
	 * process ACCEPT/CANCEL button events
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
			return;
		}
		
		if (e.getSource() == choosePalette) {
			FileDialog d = new FileDialog(this, "Overlay Object Definitions", FileDialog.LOAD);
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
		
		// make sure we have an exporter ready
		if (exporter == null || newSelection) {
			exporter = new ObjectExporter(palette.getText(), x_points, y_points);
			exported = false;
			newSelection = false;
		}
		if (!exported) {
			export(exporter);
			exported = true;
		}
		
		if (e.getSource() == accept && selected) {
			// flush the it out to a file
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				
				exporter.writeFile(export_file);
				
				// make this the new default output file name
				parms.map_name = sel_name.getText();
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == previewT && selected) {
			exporter.preview(Exporter.WhichMap.HEIGHTMAP, null);
		} else if (e.getSource() == previewF && selected) {
			exporter.preview(Exporter.WhichMap.FLORAMAP, null);
		}
	}
}
