package worldBuilder;


import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * a Dialog to control the direction and amount of rainfall on the world map.
 */
public class RainDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener {	
	private Map map;
	private double[] oldRain;	// per MeshPoint rainfall at entry
	private double[] newRain;	// edited per MeshPoint rainfall
	private boolean[] selected;	// which points are being edited
	
	private Parameters parms;
	
	private JSlider amount;
	private JButton accept;
	private JButton cancel;
	
	private static final long serialVersionUID = 1L;
	
	private static final int RAIN_DEBUG = 3;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
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
		
		amount = new JSlider(JSlider.HORIZONTAL, 0, parms.rain_max, parms.dAmount);
		amount.setMajorTickSpacing(Parameters.niceTics(0, parms.rain_max,true));
		amount.setMinorTickSpacing(Parameters.niceTics(0, parms.rain_max,false));
		amount.setFont(fontSmall);
		amount.setPaintTicks(true);
		amount.setPaintLabels(true);
		JLabel amtLabel = new JLabel("Annual Rainfall(cm/yr)", JLabel.CENTER);
		amtLabel.setFont(fontLarge);
		
		/*
		 * Pack them into:
		 * 		a vertical Box layout containing sliders and buttons
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel diaPanel = new JPanel();
		diaPanel.setLayout(new BoxLayout(diaPanel, BoxLayout.PAGE_AXIS));
		diaPanel.add(amtLabel);
		diaPanel.add(amount);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		diaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		sliders.add(diaPanel);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

		mainPane.add(sliders);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		amount.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMapListener(this);
		
		// set the map for point-group selection and get current selection
		map.selectMode(Map.Selection.POINTS);
		map.checkSelection(Map.Selection.POINTS);
		
		// initialize the rainfall to default values
		rainFall((double) amount.getValue());
	}

	/**
	 * calculate the rainfall received at each selected Mesh point
	 * @param incoming (rain density)
	 */
	private void rainFall(double incoming) {
		if (selected == null)
			return;
		
		// set this rainfall at every selected point
		double[] rainmap = map.getRainMap();
		for(int i = 0; i < selected.length; i++)
			if (selected[i])
				rainmap[i] = incoming;
		
		// tell the map about the update
		map.setRainMap(rainmap);
		if (parms.debug_level >= RAIN_DEBUG)
			System.out.println("Update rainfall: " + incoming + Parameters.unit_r);
	}

	/**
	 * @return mean rainfall over the entire map
	 */
	private double meanRain() {
		double mean = 0;
		for(int i = 0; i < newRain.length; i++) {
			mean += newRain[i];
		}
		mean /= newRain.length;
		return mean;
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		// clear selected points and updated rainfall
		map.selectMode(Map.Selection.NONE);
		if (oldRain != null) {
			map.setRainMap(oldRain);
			oldRain = null;
		}
		
		this.dispose();
		map.removeMapListener(this);
		map.repaint();
		map.selectMode(Map.Selection.ANY);
	}
	
	/**
	 * updates to the axis/inclination/profile sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == amount) {
			rainFall((double) amount.getValue());	
		} 
	}
	
	/**
	 * called when a group of points is selected on the map
	 * @param selected	array of per point booleans (true=selected)
	 * @param complete	mouse button has been released
	 * @return	boolean	(should selection continue)
	 */
	public boolean groupSelected(boolean[] selected, boolean complete) {
		this.selected = selected;
		rainFall((double) amount.getValue());
		return true;
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
			
			if (parms.debug_level > 0) {
				System.out.println("Mean rainfall: " + (int) meanRain() + Parameters.unit_r);
				System.out.println("Rivers:    " +
						"max flow " + String.format("%.2f", map.max_flux) + Parameters.unit_f +
						", max speed " + String.format("%.2f",  map.max_velocity) + Parameters.unit_v);
			}
			
			// we no longer need the old rain map
			oldRain = null;
		}
		
		// clean up the selection and graphics
		this.dispose();
		map.removeMapListener(this);
		map.selectMode(Map.Selection.NONE);
		map.repaint();
		map.selectMode(Map.Selection.ANY);
	}
	
	/** (perfunctory) */ public boolean pointSelected(double x, double y) {return false;}
	/** (perfunctory) */ public boolean regionSelected(double x, double y, double w, double h, boolean c) {return false;}
	/** (perfunctory) */ public void mouseMoved(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseEntered(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseExited(MouseEvent arg0) {}
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
}
