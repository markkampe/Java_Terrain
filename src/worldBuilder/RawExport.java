package worldBuilder;

import java.awt.event.*;
import java.io.File;

import javax.swing.JFileChooser;

/**
 * Dialog to collect information for a Raw (JSON) export
 */
public class RawExport extends ExportBase implements ActionListener {

	private Map map;

	private static final long serialVersionUID = 1L;

	private JsonExporter exporter = null;
	private boolean exported = false;

	/**
	 * Register our own action listners (BaseExport dialog is fine)
	 * 
	 * @param map ... Map to be exported
	 */
	public RawExport(Map map) {
		super("Raw JSON", map, 1, 100000, Map.Selection.RECTANGLE);
		this.map = map;

		// we handle window and button events
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
			JFileChooser c = new JFileChooser();
			if (parms.export_dir != null)
				c.setCurrentDirectory(new File(parms.export_dir));
			c.setSelectedFile(new File(sel_name.getText()+".json"));
			int retval = c.showSaveDialog(this);
			if (retval == JFileChooser.APPROVE_OPTION) {
				File chosen = c.getSelectedFile();
				exporter.writeFile(chosen.getPath());
				
				// update the defaults
				parms.map_name = chosen.getName();
				parms.export_dir = c.getCurrentDirectory().getPath();
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == previewT && selected) {
			exporter.preview(Exporter.WhichMap.HEIGHTMAP, null);
		} else if (e.getSource() == previewF && selected) {
			exporter.preview(Exporter.WhichMap.FLORAMAP, map.getFloraColors());
		}
	}
}
