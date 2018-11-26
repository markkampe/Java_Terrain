package worldBuilder;

import java.awt.FileDialog;
import java.awt.event.*;

public class RawExport extends ExportBase implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
	private JsonExporter exporter = null;
	private boolean exported = false;

	/**
	 * Set up the dialog (base class is fine) and
	 * register the action listeners.
	 * 
	 * @param map ... Map to be exported
	 */
	public RawExport(Map map) {
		super("Raw JSON", map);
		
		// we handle window and button events
		previewT.addActionListener(this);
		previewF.setEnabled(false);		// no Flora in raw json
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);

		// put up the dialog
		pack();
		setVisible(true);
	}
	
	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
			return;
		}
		
		// make sure we have an exporter ready
		if (exporter == null || newSelection) {
			exporter = new JsonExporter(x_points, y_points);
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
