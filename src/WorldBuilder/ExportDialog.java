package WorldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * SlopeDialog allows the user to choose an axis and inclination to
 * cause a uniform slope to the entire map.  
 */
public class ExportDialog extends JFrame implements ActionListener, ChangeListener, MouseListener, MouseMotionListener, WindowListener {	
	private Map map;
	private Parameters parms;
	private String filename;
	
	private JLabel sel_file;
	private JLabel sel_pixels;
	private JLabel sel_meters;
	private JLabel sel_points;
	private JSlider resolution;
	private JButton accept;
	private JButton cancel;
	
	private boolean selecting;		// selection in progress
	private boolean selected;		// selection completed
	private int x_start, x_end, y_start, y_end;		// selection start/end coordinates
	
	private int res_min, res_max;	// supported export resolutions
	
	private static final int BORDER_WIDTH = 5;
	
	private static final long serialVersionUID = 1L;
	
	public ExportDialog(Map map, String filename)  {
		// pick up references
		this.filename = filename;
		this.map = map;
		this.parms = Parameters.getInstance();
		
		res_min = 0;	// finest resolution: 1m
		res_max = 0;	// coarsest resolution: 1% of map
		for (int res = 1; res < parms.x_range * 10; res *= 2)
			res_max++;	
		
		// create the dialog box
		Container mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
		setTitle("Export");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("EXPORT");
		cancel = new JButton("CANCEL");
		
		resolution = new JSlider(JSlider.HORIZONTAL, res_min, res_max, res_min + 1);
		resolution.setMajorTickSpacing(4);
		resolution.setMinorTickSpacing(1);
		resolution.setFont(fontSmall);
		resolution.setPaintTicks(true);
		resolution.setPaintLabels(true);
		JLabel resolutionLabel = new JLabel("Resolution (m*2^n)", JLabel.CENTER);
		resolutionLabel.setFont(fontLarge);
		
		sel_file = new JLabel("File: " + filename);
		sel_pixels = new JLabel();
		sel_meters = new JLabel();
		sel_points = new JLabel("Select the area to be exported");

		/*
		 * Pack them into:
		 * 		a vertical Box layout containing descriptions sliders and buttons
		 * 		descriptions are a 1x3 layout of Labels
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel descPanel = new JPanel(new GridLayout(4,1));
		descPanel.add(sel_file);
		descPanel.add(sel_pixels);
		descPanel.add(sel_meters);
		descPanel.add(sel_points);
		
		JPanel resPanel = new JPanel();
		resPanel.setLayout(new BoxLayout(resPanel, BoxLayout.PAGE_AXIS));
		resPanel.add(resolutionLabel);
		resPanel.add(resolution);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		resPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		sliders.add(resPanel);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

		mainPane.add(descPanel, BorderLayout.NORTH);
		mainPane.add(sliders);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		resolution.addChangeListener(this);
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
	private void select(int x0, int y0, int x1, int y1, int res) {
		int x_pixels = Math.abs(x1-x0);
		int y_pixels = Math.abs(y1-y0);
		int x_meters = x_pixels * parms.x_range * 1000 / map.getWidth();
		int y_meters = y_pixels * parms.y_range * 1000 / map.getHeight();
		int grain = 1;
		while(res > 0) {
			grain *=2;
			res--;
		}
		int x_points = x_meters/grain;
		int y_points = y_meters/grain;
		
		sel_pixels.setText("Pixels: " + x_pixels + "x" + y_pixels);
		sel_meters.setText("Meters: " + x_meters + "x" + y_meters);
		sel_points.setText("Output  " + x_points + "x" + y_points + "points");
	}
	
	/**
	 * start defining a mountain range
	 */
	public void mousePressed(MouseEvent e) {
		x_start = e.getX();
		y_start = e.getY();
		selecting = true;
	}

	/**
	 * finish defining a export region
	 */
	public void mouseReleased(MouseEvent e) {
		if (selecting) {
			x_end = e.getX();
			y_end = e.getY();
			selecting = false;
			selected = true;
			select(x_start, y_start, x_end, y_end, resolution.getValue());
		}
	}
	
	/**
	 * progress in region selection
	 */
	public void mouseDragged(MouseEvent e) {
		if (selecting) {
			map.select(x_start,  y_start,  e.getX()-x_start,  e.getY()-y_start, Map.SEL_RECTANGULAR);
			select(x_start, y_start, e.getX(), e.getY(), resolution.getValue());
		}	
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.select(0, 0, 0, 0, Map.SEL_NONE);
		this.dispose();
	}
	
	/**
	 * updates to the axis/inclination sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (selected && e.getSource() == resolution) {
				select(x_start, y_start, x_end, y_end, resolution.getValue());
		} 
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		// clear the selection
		map.select(0,0,0,0, Map.SEL_NONE);
		
		if (e.getSource() == accept && selected) {
			
			// figure out the selected region
			double x = (double) x_start/map.getWidth() - parms.x_extent/2;
			double y = (double) y_start/map.getHeight() - parms.y_extent/2;
			double dx = (double) (x_end - x_start)/map.getWidth();
			double dy = (double) (y_end - y_start)/map.getHeight();
			
			// figure out the selected granularity
			int grain = 1;
			int res = resolution.getValue();
			while(res > 0) {
				grain *=2;
				res--;
			}
			map.getMesh().export(filename, x, y, dx, dy, grain);
		}
		
		// discard the window
		this.dispose();
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