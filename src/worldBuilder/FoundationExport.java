package worldBuilder;

import java.awt.event.*;
import javax.swing.JFileChooser;

/**
 * Dialog to collect information for a Raw (JSON) export
 */
public class FoundationExport extends ExportBase implements ActionListener {
	private FoundExporter exporter = null;
	
	//private JSlider flora_pct;		// percent plant cover
	//private RangeSlider flora_3;	// types of plant cover
	//private JSlider goose_temp;		// temperature up/down
	//private JSlider goose_hydro1;	// hydration up/down
	//private JSlider goose_hydro2;	// hydration multiplier
	
	// state variables to advice us of need to recompute
	boolean floraChanged = false;	// flora distribution has changed
	boolean hydroChanged = false;	// hydration has changed
	boolean tempChanged = false;	// temperatures have changed
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
		super("Foundation", map, 1, 1, Map.Selection.SQUARE);
		
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
			
			// FIX implement entry/exit point selection
			exporter.entryPoint(  2,  84,   2,  62);	// bogus value for testing
			exporter.entryPoint(  2,  18,   2,  40);	// bogus value for testing
			exporter.entryPoint(100,   2,  31,   2);	// bogus value for testing
			exporter.entryPoint(126,  34, 126,  90);	// bogus value for testing
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
