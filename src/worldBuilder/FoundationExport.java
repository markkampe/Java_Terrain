package worldBuilder;

import java.awt.event.*;

import javax.swing.JFileChooser;

/**
 * Dialog to collect information for a Raw (JSON) export
 */
public class FoundationExport extends ExportBase implements ActionListener {
	private FoundExporter exporter = null;
	
	// state variables to advice us of need to recompute
	private boolean exported = false;
	
	/*
	 * All Foundation maps are 1024x1024, but that is slow and noisy.
	 * Since it will all be interpolated anyway, I export a lower
	 * resolution and expand it in FoundExporter..
	 */
	private static final int EXPORT_SIZE = 256;

	private static final long serialVersionUID = 1L;

	/**
	 * Register our own action listeners (BaseExport dialog is fine)
	 * 
	 * @param map ... Map to be exported
	 */
	public FoundationExport(Map map) {
		super("Foundation", map, 1, 1, MapWindow.Selection.SQUARE);
		
		// 1024x1024 is slow and noisy, start small and interpolate
		x_points = EXPORT_SIZE;
		y_points = EXPORT_SIZE;
		if (selected)
			regionSelected(box_x, box_y, box_width, box_height, true);
		
		// add our controls to those in the base class
		create_GUI();

		// we handle window and button events
		previewT.addActionListener(this);
		previewF.addActionListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);

		pack();
		setVisible(true);
	}
	
	/**
	 * create Foundation-specific widgets
	 */
	private void create_GUI() {
		// Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		//  Font fontLarge = new Font("Serif", Font.ITALIC, 15);
	}

	/**
	 * entry and exit points (typically harbors)
	 */
	private class Point {
		public int row;		// export row (/EXPORT_SIZE)
		public int col;		// export col (/EXPORT SIZE)
		
		/**
		 * convert map <x,y> into <row,col>
		 */
		public Point(double x, double y) {
			double dx = (x - box_x)/box_width;
			col = (int) (EXPORT_SIZE * dx);
			double dy = (y - box_y)/box_height;
			row = (int) (EXPORT_SIZE * dy);
		}
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
			exporter = new FoundExporter(x_points, y_points);
			exported = false;
			newSelection = false;
		}
		if (!exported) {
			export(exporter);
			
			// look for designated entry and exit points
			Point[] entrypoints = new Point[FoundExporter.MAX_PORTS];
			Point[] exitpoints = new Point[FoundExporter.MAX_PORTS];
			int entrances = 0;
			int exits = 0;
			String[] names = map.getNameMap();
			for(int i = 0; i < map.mesh.vertices.length; i++) {
				// make sure this is a named point within the box
				if (names[i] == null)
					continue;
				MeshPoint p = map.mesh.vertices[i];
				if (p.x < box_x || p.y < box_y)
					continue;
				if (p.x >= box_x + box_width || p.y >= box_y + box_height)
					continue;
				
				// see if it is an entry or exit point
				String type = CityDialog.lexType(names[i]);
				if (type.equals("entrypoint")) {
					entrypoints[entrances++] = new Point(p.x, p.y);
				} else if (type.equals("exitpoint")) {
					exitpoints[exits++] = new Point(p.x, p.y);
				}
			}
			// pass them to the exporter
			for(int i = 0; i < entrances && i < exits; i++)
				exporter.entryPoint(entrypoints[i].col, entrypoints[i].row, 
									exitpoints[i].col, exitpoints[i].row);

			exported = true;
		}
	
		if (e.getSource() == accept && selected) {
			// create files in a chosen directory
			JFileChooser d = new JFileChooser(parms.project_dir);
			d.setDialogTitle("Export Directory");
			d.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			d.setAcceptAllFileFilterUsed(false);
			if (d.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				String dir = d.getSelectedFile().getPath();
				parms.project_dir = dir;
				parms.map_name = sel_name.getText();
				exporter.writeFile(dir);
				
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
