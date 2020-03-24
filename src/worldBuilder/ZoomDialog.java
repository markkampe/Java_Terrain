package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ZoomDialog extends JFrame implements ActionListener, WindowListener, MapListener {
	private Map map;
	private Parameters parms;
		
	private JButton accept;
	private JLabel sel_pixels;
	private JLabel sel_km;
	
	private double new_x, new_y;	// start of zoom window
	private double new_height;		// height of new zoom
	private double new_width;		// width of new zoom
	private boolean selected;		// selection completed
	private boolean zoomed;
	
	private static final long serialVersionUID = 1L;
	
	public ZoomDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Zoom");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		accept = new JButton("ZOOM");
		sel_pixels = new JLabel("SELECT THE AREA TO ZOOM INTO");
		sel_km = new JLabel("");

		/*
		 * Pack them into:
		 * 		a vertical Box layout containing descriptions and buttons
		 * 		descriptions are a 1x2 layout of Labels
		 * 		buttons a horizontal Box layout
		 */
		JPanel descPanel = new JPanel(new GridLayout(2,2));
		descPanel.add(new JLabel("pixels"));
		descPanel.add(sel_pixels);
		descPanel.add(new JLabel(Parameters.unit_xy));
		descPanel.add(sel_km);
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,10));

		JPanel buttons = new JPanel();
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,20,20,10));

		mainPane.add(descPanel);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		accept.addActionListener(this);
		map.addMapListener(this);
		
		selected = map.checkSelection(Map.Selection.RECTANGLE);
	}
	
	/**
	 * describe the selected area
	 * @param width (in map units)
	 * @param height (in map units)
	 */
	private void describe(double width, double height) {
		// selected area in pixels
		int x_pixels = map.screen_x(width) - map.screen_x(0);
		int y_pixels = map.screen_y(height) - map.screen_y(0);
		
		// selected area in world coordinates
		double x_km = parms.km(width);
		double y_km = parms.km(height);
		
		// update the selection legend
		sel_pixels.setText(x_pixels + "x" + y_pixels);
		sel_km.setText("" + (int) x_km + "x" + (int) y_km);
	}
	
	/**
	 * called whenever a region selection changes
	 * @param map_x0	left most point (map coordinate)
	 * @param map_y0	upper most point (map coordinate)
	 * @param width		(in map units)
	 * @param height	(in map units)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(
			double x0, double y0, 
			double width, double height,
			boolean complete) {
		
		// record the area to zoom
		new_x = x0;
		new_y = y0;
		
		if (complete) {
			// ensure new map has correct aspect ration
			double mapAspect = (double) map.getWidth() / map.getHeight();
			double selAspect = (double) width / height;
			if (selAspect >= mapAspect)
				height = width * mapAspect;
			else
				width = height / mapAspect;
			
			// and push this correction back to the display
			map.selectRect(map.screen_x(x0), map.screen_y(y0),
						   map.screen_x(x0+width), map.screen_y(y0+height));
		}
		new_height = height;
		new_width = width;
		describe(width, height);
		
		selected = complete;
		return true;
	}

	/**
	 * Window Close event handler ... do nothing
	 */
	public void windowClosing(WindowEvent e) {
		map.selectMode(Map.Selection.ANY);
		map.setWindow(-Parameters.x_extent/2, -Parameters.y_extent/2, Parameters.x_extent/2, Parameters.y_extent/2);
		map.removeMapListener(this);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		// clear the selection
		map.selectMode(Map.Selection.ANY);
		
		if (e.getSource() == accept && selected && !zoomed) {
			sel_pixels.setText("DISMISS DIALOG TO UNZOOM");
			map.setWindow(new_x, new_y, new_x + new_width, new_y + new_height);
			zoomed = true;
			
			// we accept no further input, just wait for the close
			map.removeMapListener(this);
			accept.setVisible(false);
			WorldBuilder.activeDialog = false;
			accept.removeActionListener(this);
		}
	}

	public boolean pointSelected(double x, double y) { return false; }
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}
