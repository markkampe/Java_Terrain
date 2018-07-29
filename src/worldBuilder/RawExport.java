package worldBuilder;

import java.awt.FileDialog;
import java.awt.event.*;

public class RawExport extends ExportBase implements ActionListener {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Set up the dialog (base class is fine) and
	 * register the action listeners.
	 * 
	 * @param map ... Map to be exported
	 */
	public RawExport(Map map) {
		super("Raw JSON", map);
		
		// create sub-class-specific controls
		// controls.add(whatever);
		
		// we handle window and button events
		preview.addActionListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);
		
		// the standard dialog is all we need
		pack();
		setVisible(true);
	}
	
	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == accept && selected) {
			Exporter exporter = new JsonExporter(x_points, y_points);
			export(exporter);
			
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
		} else if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
		} else if (e.getSource() == preview && selected) {
			Exporter exporter = new JsonExporter(x_points, y_points);
			export(exporter);
			exporter.preview(Exporter.WhichMap.HEIGHTMAP, null);
		}
	}
}
