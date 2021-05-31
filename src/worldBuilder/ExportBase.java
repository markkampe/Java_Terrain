package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * a super-class for exporter dialogs - creates dialogs, handles
 * handles region selection, pumps data to an Exporter
 */ 
public class ExportBase extends JFrame implements WindowListener, MapListener {	
	/*
	 * the following fields/widgets are created by this base class, to be
	 * used by what ever export sub-class is being used
	 */
	/** map from which we will export	*/
	protected Map map;
	/** displayed map for update and selection */
	protected MapWindow window;
	/** parameters Singleton			*/
	protected Parameters parms;

	/** common widget - name of region to be exported	*/
	protected JTextField sel_name;
	/** common widget - control buttons*/
	protected JButton accept, cancel, previewT, previewF;
	/** common widget - JPannel for additional controls	*/
	protected JPanel controls;
	
	/** an export region has been selected or changed	*/
	protected boolean selected, newSelection;

	/** the size (in tiles) of the region to be exported */
	protected int x_points, y_points;

	private JLabel sel_center;		// lat/lon of center
	private JLabel sel_km;			// size of region (in km)
	private JTextField sel_t_size;	// size of tile (in m)
	private JLabel sel_points;		// number of tiles in region
	private JSlider resolution;		// tile size @return selection
	
	protected double box_x, box_y;		// selection box map coordinates
	protected double box_width, box_height;	// selection box size (map units)
	
	// parameters for the tile size selection slider
	private static final int TICS_PER_DECADE = 9;
	
	protected static final int EXPORT_DEBUG = 2;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * create the initial export selection dialog and register the listeners
	 * 
	 * @param format ... used to title the dialog
	 * @param map ... the map from which we are exporting
	 * @param minTile ... minimum tile size (meters)
	 * @param maxTile ... maximum tile size (meters)
	 * @param shape ... required export region shape
	 */
	public ExportBase(String format, Map map, int minTile, int maxTile, MapWindow.Selection shape)  {
		// pick up references
		this.map = map;
		this.window = map.window;
		this.parms = Parameters.getInstance();
		
		// sanity check the maximum and default tile sizes
		while(maxTile > parms.xy_range*100)
			maxTile /= 10;
		if (maxTile > 1 && parms.dTileSize >= maxTile)
			parms.dTileSize = maxTile/10;
			
		// create the dialog box
		int border = parms.dialogBorder;
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Export - " + format);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		sel_name = new JTextField();
		sel_name.setText(parms.map_name);
		JLabel nameLabel = new JLabel("Exported Map Name", JLabel.CENTER);
		nameLabel.setFont(fontLarge);
		
		accept = new JButton("EXPORT");
		cancel = new JButton("CANCEL");
		previewT = new JButton("PREVIEW TOPO");
		previewF = new JButton("PREVIEW FLORA");
	
		// the (optional loagrithmic) tile size slider is tricky
		JLabel resolutionLabel = null;
		if (maxTile > minTile) {
			resolution = new JSlider(JSlider.HORIZONTAL, meters_to_slider(minTile), meters_to_slider(maxTile), 
					meters_to_slider(parms.dTileSize));
			resolution.setMajorTickSpacing(TICS_PER_DECADE);
			resolution.setFont(fontSmall);
			resolution.setPaintTicks(true);
			Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for(int sz = 1; sz <= maxTile; sz *= 10)
				labels.put(meters_to_slider(sz),
						   new JLabel((sz < 1000) ? String.valueOf(sz) :
							   						String.valueOf(sz/1000) + "km"));
			resolution.setLabelTable(labels);
			resolution.setPaintLabels(true);
			
			resolutionLabel = new JLabel("Tile size(m)", JLabel.CENTER);
			resolutionLabel.setFont(fontLarge);
		}
		
		// create the region description fields
		sel_center = new JLabel();
		sel_km = new JLabel();
		sel_points = new JLabel("Select the area to be exported");
		
		// NORTH: 4x2 grid of descriptions
		JPanel descPanel = new JPanel(new GridLayout(4,2));
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
		descPanel.add(new JLabel("Center (lat, lon)"));
		descPanel.add(sel_center);
		descPanel.add(new JLabel("Selected area (km)"));
		descPanel.add(sel_km);
		descPanel.add(new JLabel("Selected area (tiles)"));
		descPanel.add(sel_points);
		sel_t_size = new JTextField();
		descPanel.add(new JLabel("Tile Size (m)"));
		descPanel.add(sel_t_size);
		mainPane.add(descPanel, BorderLayout.NORTH);

		// SOUTH: 3 buttons
		JPanel buttons = new JPanel();
		buttons.add(cancel);
		buttons.add(previewT);
		buttons.add(previewF);
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		// CENTER: rows, start w/tile size slider, map name
		JPanel namePanel = new JPanel(new GridLayout(2,1));
		namePanel.add(nameLabel);
		namePanel.add(sel_name);
		namePanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		if (resolution != null) {
			JPanel resPanel = new JPanel();
			resPanel.setLayout(new BoxLayout(resPanel, BoxLayout.PAGE_AXIS));
			resPanel.add(resolutionLabel);
			resPanel.add(resolution);
			resPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			sliders.add(resPanel);
		}
		
		// tile size may be input or display only
		if (resolution != null)
			sel_t_size.setText(Integer.toString(parms.dTileSize));
		else
			sel_t_size.setEditable(false);
		
		controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.PAGE_AXIS));
		controls.add(sliders);
		controls.add(namePanel);
		mainPane.add(controls, BorderLayout.CENTER);
		
		// add the super-class action listeners
		// (in-place to prevent shadowing by sub-class listeners)
		window.addMapListener(this);
		
		// tile size may or may not be changeable
		if (resolution != null) {
			sel_t_size.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					int meters = Integer.parseInt(sel_t_size.getText());
					resolution.setValue(meters_to_slider(meters));
					sel_t_size.setText(Integer.toString(meters));
					tile_size(meters);
					newSelection = true;
				}
			});

			resolution.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					int meters = slider_to_meters(resolution.getValue());
					sel_t_size.setText(Integer.toString(meters));
					tile_size(meters);
					newSelection = true;
				}
			});
		}
		
		window.selectMode(shape);
		selected = window.checkSelection(shape);
		newSelection = false;
	}
	
	/**
	 * create a full set of per-tile maps and pass them to the Exporter
	 * @param export - Exporter for the selected format
	 * 
	 * We need to re-call export whenever the Cartesian changes
	 *  - selected region changes
	 *  - tile size (number of tiles) changes
	 */
	protected void export(Exporter exporter) {
		
		// instantiate an Export Engine (to translate MeshPoints into tiles)
		ExportEngine e = new ExportEngine(map, box_x, box_y, box_width, box_height);
		
		// set (and remember) the tile size
		int meters = Integer.parseInt(sel_t_size.getText());
		e.tile_size(meters);
		parms.dTileSize = meters;
		
		// do the Cartesian conversions and pass them to the exporter
		e.export(exporter);
		
		// the write operation will be initiated in the sub-class
	}
	

	/**
	 * MapListener for region selection changes
	 * @param mx0	left most point (map coordinate)
	 * @param my0	upper most point (map coordinate)
	 * @param dx	width (in map units)
	 * @param dy	height (in map units)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(double mx0, double my0, 
			  double dx, double dy, boolean complete) {

		// update the export box location
		box_x = mx0;
		box_y = my0;
		box_width = dx;
		box_height = dy;
		sel_center.setText(String.format("%.6f, %.6f", 
				parms.latitude(box_y + box_height/2),  
				parms.longitude(box_x + box_width/2)));
		
		// update the selected area size
		double x_km = parms.km(box_width);
		double y_km = parms.km(box_height);
		sel_km.setText(String.format("%.1fx%.1f", x_km, y_km));

		if (resolution != null)
			// x_points/y_points is a function of tile size
			tile_size(Integer.parseInt(sel_t_size.getText()));
		else if (x_points > 0 && y_points > 0) {
			// tile size is a function of x_points/y_points
			sel_points.setText(x_points + "x" + y_points);
			int tile_size = (int) (x_km * 1000 / x_points);
			sel_t_size.setText(String.valueOf(tile_size));
		}
		
		selected = true;
		newSelection = true;
		return true;
	}
	
	/**
	 * update the expected export size based on a new tile size
	 * 
	 * @param meters
	 */
	private void tile_size(int meters) {
		x_points = (int) (parms.km(box_width) * 1000 / meters);
		y_points = (int) (parms.km(box_height) * 1000 / meters);
		int tiles = x_points * y_points;
		sel_points.setText(x_points + "x" + y_points);
		sel_points.setForeground( tiles > parms.tiles_max ? Color.RED : Color.BLACK);
	}
	
	/**
	 * convert a tile-size slider position into meters-per-tile
	 * @return the number of meters associated with a slider value
	 * @param slider
	 */
	private int slider_to_meters(int slider) {
		// get the power of ten
		int meters = 1;
		while(slider > TICS_PER_DECADE) {
			meters *= 10;
			slider -= TICS_PER_DECADE;
		}
		
		return(meters * (1 + slider));
	}
	
	/**
	 * convert a meters-per-tile value into a slider position
	 * @return the slider value associated with a number of meters
	 * @param meters
	 */
	private int meters_to_slider(int meters) {
		int slider = 0;
		while(slider_to_meters(slider+1) <= meters)
			slider++;
		return(slider);
	}

	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		window.selectMode(MapWindow.Selection.ANY);
		window.removeMapListener(this);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	/** (perfunctory) */ public boolean pointSelected(double x, double y) { return false; }
	/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
	
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	
	/** (perfunctory) */ public void mouseClicked(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseMoved(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseEntered(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseExited(MouseEvent arg0) {}
}
