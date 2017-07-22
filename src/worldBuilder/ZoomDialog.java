package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ZoomDialog extends JFrame implements ActionListener, WindowListener, MouseListener, MouseMotionListener {
	private Map map;
	private Parameters parms;
		
	private JButton accept;
	private JButton cancel;
	private JLabel sel_pixels;
	private JLabel sel_km;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public ZoomDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// create the dialog box
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
		setTitle("Export");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		accept = new JButton("ZOOM");
		cancel = new JButton("CANCEL");
		sel_pixels = new JLabel("SELECT THE AREA TO ZOOM INTO");
		sel_km = new JLabel("CANCEL TO RETURN TO FULL SCALE");

		/*
		 * Pack them into:
		 * 		a vertical Box layout containing descriptions and buttons
		 * 		descriptions are a 1x2 layout of Labels
		 * 		buttons a horizontal Box layout
		 */
		JPanel descPanel = new JPanel(new GridLayout(2,1));
		descPanel.add(sel_pixels);
		descPanel.add(sel_km);
		descPanel.setBorder(BorderFactory.createEmptyBorder(20,100,20,10));

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,20,20,10));

		mainPane.add(descPanel);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		accept.addActionListener(this);
		cancel.addActionListener(this);
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
		
		// selected area in km
		double scale = parms.xy_range;
		scale /= map.getWidth() > map.getHeight() ? map.getWidth() : map.getHeight();
		double x_km = x_pixels * scale;
		double y_km = y_pixels * scale;
		
		sel_pixels.setText("Pixels:    " + x_pixels + "x" + y_pixels);
		sel_km.setText("Kilometers:" + (int) x_km + "x" + (int) y_km);
	}
	
	/**
	 * start defining a zoom window
	 */
	public void mousePressed(MouseEvent e) {
		x_start = e.getX();
		y_start = e.getY();
		selecting = true;
	}

	/**
	 * finish defining a zoom window
	 */
	public void mouseReleased(MouseEvent e) {
		if (selecting) {
			x_end = e.getX();
			y_end = e.getY();
			map.selectRect(x_start, y_start, e.getX()-x_start, e.getY()-y_start);
			selecting = false;
			selected = true;
			describe(x_start, y_start, x_end, y_end);
		}
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.selectRect(x_start, y_start, e.getX()-x_start, e.getY()-y_start);
		}	
	}
	
	/**
	 * Window Close event handler ... do nothing
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		// clear the selection
		map.selectNone();
		
		if (e.getSource() == accept && selected) {
			System.out.println("IMPLEMENT ZOOM");
		} else if (e.getSource() == cancel) {
			System.out.println("IMPLEMENT UNZOOM");
		}
		
		// discard the window
		this.dispose();
		map.removeMouseListener(this);
		map.removeMouseMotionListener(this);
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
