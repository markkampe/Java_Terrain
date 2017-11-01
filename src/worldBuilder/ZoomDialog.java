package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ZoomDialog extends JFrame implements ActionListener, WindowListener, MouseListener, MouseMotionListener {
	private Map map;
	private Parameters parms;
		
	private JButton accept;
	private JLabel sel_pixels;
	private JLabel sel_km;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private boolean zoomed;
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
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
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		
		selecting = false;
		selected = false;
	}
	
	/**
	 * describe the selected area
	 */
	private void describe(int x0, int y0, int x1, int y1) {
		// selected area in pixels
		int x_pixels = Math.abs(x1-x0);
		int y_pixels = Math.abs(y1-y0);
		
		// selected area in map coordinates
		double X0 = map.map_x(x0);
		double X1 = map.map_x(x1);
		double Y0 = map.map_y(y0);
		double Y1 = map.map_y(y1);
		
		// selected area in world coordinates
		double x_km = parms.km(X1-X0);
		double y_km = parms.km(Y1-Y0);
		
		// update the selection legend
		sel_pixels.setText(x_pixels + "x" + y_pixels);
		sel_km.setText("" + (int) x_km + "x" + (int) y_km);
	}
	
	/**
	 * start defining a zoom window
	 */
	public void mousePressed(MouseEvent e) {
		if (!zoomed) {
			x_start = e.getX();
			y_start = e.getY();
			selecting = true;
		}
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.selectRect(x_start, y_start, e.getX()-x_start, e.getY()-y_start);
			describe(x_start, y_start, e.getX(), e.getY());
		}	
	}
	
	/**
	 * finish defining a zoom window
	 */
	public void mouseReleased(MouseEvent e) {
		if (selecting) {
			x_end = e.getX();
			y_end = e.getY();
			int dx = x_end - x_start;
			int dy = y_end - y_start;
			
			// deal with negatively defined regions
			if (dx < 0) {
				x_start = x_end;
				dx = -dx;
			}
			if (dy < 0) {
				y_start = y_end;
				dy = -dy;
			}
			
			// enforce the map aspect ratio
			int height = map.getHeight();
			int width = map.getWidth();
			double mapAspect = (double) width / height;
			double selAspect = (double) dx / dy;
			if (selAspect >= mapAspect)
				dy = (int) ((double) dx / mapAspect); 
			else
				dx = (int) ((double) dy * mapAspect);
			x_end = x_start + dx;
			y_end = y_start + dy;
			
			// indicate the selected region
			map.selectRect(x_start, y_start, dx, dy);
			describe(x_start, y_start, x_end, y_end);
			selecting = false;
			selected = true;
		}
	}

	/**
	 * Window Close event handler ... do nothing
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		map.setWindow(-Parameters.x_extent/2, -Parameters.y_extent/2, Parameters.x_extent/2, Parameters.y_extent/2);
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
		WorldBuilder.activeDialog = false;
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		// clear the selection
		map.selectNone();
		
		if (e.getSource() == accept && selected && !zoomed) {
			sel_pixels.setText("DISMISS DIALOG TO UNZOOM");
			map.setWindow(map.map_x(x_start), map.map_y(y_start), map.map_x(x_end), map.map_y(y_end));
			zoomed = true;
			
			// we accept no further input
			accept.setVisible(false);
			map.removeMouseListener(this);
			map.removeMouseMotionListener(this);
			WorldBuilder.activeDialog = false;
			accept.removeActionListener(this);
		}
	}

	public void mouseClicked(MouseEvent arg0) {}
	public void mouseMoved(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
	public void windowActivated(WindowEvent arg0) {}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
}
