package worldBuilder;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * SlopeDialog allows the user to choose an axis and inclination to
 * cause a uniform slope to the entire map.  
 */
public class RainDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {	
	private Map map;
	private double[] oldRain;	// per MeshPoint rainfall at entry
	private double[] newRain;	// edited per MeshPoint raifall
	private Parameters parms;
	
	private JSlider direction;
	private JSlider amount;
	private JSlider altitude;
	private JButton accept;
	private JButton cancel;
	
	private static final int DIALOG_OFFSET = 3;
	
	private static final long serialVersionUID = 1L;
	
	public RainDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldRain = map.getRainMap();
		this.parms = Parameters.getInstance();
		
		// copy current rain map
		this.newRain = new double[oldRain.length];
		for(int i = 0; i < oldRain.length; i++)
			newRain[i] = oldRain[i];
		map.setRainMap(newRain);

		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Rainfall");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");
		
		direction = new JSlider(JSlider.HORIZONTAL, -180, 180, parms.dDirection);
		direction.setMajorTickSpacing(90);
		direction.setMinorTickSpacing(30);
		direction.setFont(fontSmall);
		direction.setPaintTicks(true);
		direction.setPaintLabels(true);
		JLabel dirLabel = new JLabel("Dominant Direction", JLabel.CENTER);
		dirLabel.setFont(fontLarge);

		amount = new JSlider(JSlider.HORIZONTAL, 0, parms.rain_max, parms.dAmount);
		amount.setMajorTickSpacing(Parameters.niceTics(0, parms.rain_max,true));
		amount.setMinorTickSpacing(Parameters.niceTics(0, parms.rain_max,false));
		amount.setFont(fontSmall);
		amount.setPaintTicks(true);
		amount.setPaintLabels(true);
		JLabel amtLabel = new JLabel("Annual Rainfall(cm/yr)", JLabel.CENTER);
		amtLabel.setFont(fontLarge);
		
		altitude = new JSlider(JSlider.HORIZONTAL, 0, parms.alt_maxrain, parms.dRainHeight);
		altitude.setMajorTickSpacing(Parameters.niceTics(0, parms.alt_maxrain, true));
		altitude.setMinorTickSpacing(Parameters.niceTics(0, parms.alt_maxrain, false));
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altLabel = new JLabel("Cloud Bottoms(m x 1000)", JLabel.CENTER);
		altLabel.setFont(fontLarge);

		
		/*
		 * Pack them into:
		 * 		a vertical Box layout containing sliders and buttons
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel altPanel = new JPanel();
		altPanel.setLayout(new BoxLayout(altPanel, BoxLayout.PAGE_AXIS));
		altPanel.add(dirLabel);
		altPanel.add(direction);
		
		JPanel diaPanel = new JPanel();
		diaPanel.setLayout(new BoxLayout(diaPanel, BoxLayout.PAGE_AXIS));
		diaPanel.add(amtLabel);
		diaPanel.add(amount);
		
		JPanel rndPanel = new JPanel();
		rndPanel.setLayout(new BoxLayout(rndPanel, BoxLayout.PAGE_AXIS));
		rndPanel.add(altLabel);
		rndPanel.add(altitude);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		diaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		rndPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		sliders.add(altPanel);
		sliders.add(diaPanel);
		sliders.add(rndPanel);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

		mainPane.add(sliders);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setLocation(parms.dialogDX + DIALOG_OFFSET * parms.dialogDelta, parms.dialogDY + DIALOG_OFFSET * parms.dialogDelta);
		setVisible(true);
		
		// add the action listeners
		direction.addChangeListener(this);
		amount.addChangeListener(this);
		altitude.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		
		// initialize the direction indicator
		setDirection(direction.getValue());
		
		// initialize the rainfall to default values
		rainFall();
	}

	/**
	 * calculate the rainfall received at each Mesh point
	 */
	private void rainFall() {
		int dir = direction.getValue();
		int amt = amount.getValue();
		int alt = altitude.getValue();
		
		// TODO trace source path back to each MeshPoint
		// TODO topographically forced rain
		// TODO reduce available moisture by what falls
		// TODO rain on MeshPoint = f(available)
		
		// compute the rain at every point
		for(int i = 0; i < newRain.length; i++) {
			// start out with even rain distribution
			newRain[i] = amt;
		}
		// tell the map about the update
		map.setRainMap(newRain);
	}
	
	/**
	 * display weather direction as a select line
	 * 
	 * @param angle (0 = north)
	 */
	private void setDirection(int degrees) {
		
		// figure out line center and length
		int x_center = map.getWidth()/2;
		int x_len = 3 * x_center / 2;
		int y_center = map.getHeight()/2;
		int y_len = 3 * y_center / 2;
		
		int x0, y0, x1, y1;
		
		// vertical lines are a special case
		if (degrees == -90 || degrees == 90) {
			x0 = x_center;
			x1 = x_center;
			y0 = map.getHeight()/8;
			y1 = map.getHeight()*7/8;
		} else {
			double radians = Math.PI * ((double) degrees)/180;
			double sin = Math.sin(radians);
			double cos = Math.cos(radians);
			double dy = sin * y_len / 2;
			double dx = cos * x_len / 2;
			x0 = x_center - (int) dx;
			y0 = y_center - (int) dy;
			x1 = x_center + (int) dx;
			y1 = y_center + (int) dy;
		}
		
		// display the slope axis
		map.selectLine(x0, y0, x1, y1);
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		if (oldRain != null) {
			map.setRainMap(oldRain);
			map.repaint();
			oldRain = null;
		}
		this.dispose();
	}
	
	/**
	 * updates to the axis/inclination/profile sliders
	 */
	public void stateChanged(ChangeEvent e) {
			if (e.getSource() == direction) {
				setDirection(direction.getValue());
				rainFall();
			} else if (e.getSource() == amount) {
				rainFall();
			} else if (e.getSource() == altitude) {
				rainFall();
			}
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			// revert to previous rain map
			map.setRainMap(oldRain);
			map.repaint();
			oldRain = null;
		} else if (e.getSource() == accept) {
			// make the new parameters official
			parms.dAmount = amount.getValue();
			parms.dDirection = direction.getValue();
			parms.dRainHeight = altitude.getValue();
			
			if (parms.debug_level > 0)
				System.out.println("Rainfall: " + parms.dAmount + Parameters.unit_r + ", from " + 
						parms.dDirection + ", cloud bottoms at " + parms.dRainHeight + Parameters.unit_z);
			// we no longer need the old rain map
			oldRain = null;
		}
		
		// clean up the graphics
		map.selectNone();
		this.dispose();
	}
	
	// perfunctory methods
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
