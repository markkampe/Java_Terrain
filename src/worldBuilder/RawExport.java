package worldBuilder;

import java.awt.FileDialog;
import java.awt.event.*;

public class RawExport extends ExportBase implements ActionListener, WindowListener {
	
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
		preview.setEnabled(false);
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
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(sel_name.getText()+".json");
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				Exporter exporter = new JsonExporter(export_file, x_points, y_points);
				
				// RAW JSON requires no special processing
				export(exporter, export_file);
				
				// make this the new default output file name
				parms.map_name = sel_name.getText();
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
		}
	}

	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
		WorldBuilder.activeDialog = false;
	}
	
	// perfunctory handlers for events we don't care about
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}
