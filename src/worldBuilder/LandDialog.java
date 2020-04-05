package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to adjust height, erosion and deposition
 */
public class LandDialog extends JFrame implements ActionListener, ChangeListener, ItemListener, MapListener, KeyListener, WindowListener {	
	
	private Map map;
	private Parameters parms;
	
	/*
	 * The old maps are the last accepted versions.
	 * The new maps start out as copies of the old maps, but are updated
	 * to reflect the requested changes.  During the editing process, it
	 * is these new maps that are loaded into the displayed Map.
	 *
	 * If changes are rejected, the old maps are reinstantiated.
	 * When changes are accepted, the new map values are copied to
	 * the old maps.
	 */
	private double[] old_height, new_height;
	private double[] old_erosion, erodeMap;
	private double[] old_soil, new_soil;
	
	private JCheckBox igneous;
	private JCheckBox metamorphic;
	private JCheckBox sedimentary;
	private JCheckBox alluvial;
	private JSlider altitude;
	private JSlider flatness;
	private JSlider erosion;
	private JSlider deposition;
	private JButton accept;
	private JButton cancel;
	
	private boolean have_selection;		// there is a selected point set
	private boolean[] selected_points;	// which points are selected
	
	private static final int LAND_DEBUG = 2;
	private static final int ERODE_DEBUG = 3;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public LandDialog(Map map)  {
		this.parms = Parameters.getInstance();
		
		// pick up references current maps
		this.map = map;
		this.old_height = map.getHeightMap();
		this.old_soil = map.getSoilMap();
		this.erodeMap = map.getErodeMap();	// erodeMap is edit-in-place
		
		// make new (WIP) copies of each
		int points = old_height.length;
		new_height = new double[points];
		old_erosion = new double[points];
		new_soil = new double[points];
		for(int i = 0; i < points; i++) {
			new_height[i] = old_height[i];
			new_soil[i] = old_soil[i];
			old_erosion[i] = erodeMap[i];
		}
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Edit soil, height, erosion/deposition");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the buttons and sliders
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT (Enter)");
		cancel = new JButton("CANCEL (Esc)");
		
		igneous = new JCheckBox("Igneous");
		metamorphic = new JCheckBox("Metamorphic");
		sedimentary = new JCheckBox("Sedimentary");
		alluvial = new JCheckBox("Alluvial");
		
		altitude = new JSlider(JSlider.HORIZONTAL, -parms.delta_z_max, parms.delta_z_max, 0);
		altitude.setMajorTickSpacing(Parameters.niceTics(-parms.delta_z_max, parms.delta_z_max, true));
		if (parms.delta_z_max > 10)
			altitude.setMinorTickSpacing(Parameters.niceTics(-parms.delta_z_max, parms.delta_z_max, false));
		altitude.setFont(fontSmall);
		altitude.setPaintTicks(true);
		altitude.setPaintLabels(true);
		JLabel altitudeLabel = new JLabel("Lower/Raise Height(m)", JLabel.CENTER);
		altitudeLabel.setFont(fontLarge);

		// labels for the exaggeration sliders
		Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
		JLabel low = new JLabel("/10");
		low.setFont(fontSmall);
		labels.put(-10,  low);
		JLabel mid = new JLabel("x1");
		mid.setFont(fontSmall);
		labels.put(0,  mid);
		JLabel high = new JLabel("x10");
		high.setFont(fontSmall);
		labels.put(10, high);
		
		flatness = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
		flatness.setMajorTickSpacing(Parameters.niceTics(-10, 10, true));
		flatness.setFont(fontSmall);
		flatness.setPaintTicks(true);
		flatness.setLabelTable(labels);
		flatness.setPaintLabels(true);
		JLabel flatnessLabel = new JLabel("Height Range", JLabel.CENTER);
		flatnessLabel.setFont(fontLarge);
		
		erosion = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
		erosion.setMajorTickSpacing(Parameters.niceTics(-10, 10, true));
		erosion.setFont(fontSmall);
		erosion.setPaintTicks(true);
		erosion.setLabelTable(labels);
		erosion.setPaintLabels(true);
		JLabel erosionLabel = new JLabel("High Flow Erosion", JLabel.CENTER);
		erosionLabel.setFont(fontLarge);
		
		deposition = new JSlider(JSlider.HORIZONTAL, -10, 10, 0);
		deposition.setMajorTickSpacing(Parameters.niceTics(-10, 10, true));
		deposition.setFont(fontSmall);
		deposition.setPaintTicks(true);
		deposition.setLabelTable(labels);
		deposition.setPaintLabels(true);
		JLabel depositionLabel = new JLabel("Low Flow Deposition", JLabel.CENTER);
		depositionLabel.setFont(fontLarge);
		
		// create a panel for each slider and its label
		JPanel altPanel = new JPanel();	// altitude adjustment
		altPanel.setLayout(new BoxLayout(altPanel, BoxLayout.PAGE_AXIS));
		altPanel.add(altitudeLabel);
		altPanel.add(altitude);
		altPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel flatPanel = new JPanel();	// flatness/exaggeration
		flatPanel.setLayout(new BoxLayout(flatPanel, BoxLayout.PAGE_AXIS));
		flatPanel.add(flatnessLabel);
		flatPanel.add(flatness);
		flatPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
	
		JPanel erodPanel = new JPanel();	// degree of erosion
		erodPanel.setLayout(new BoxLayout(erodPanel, BoxLayout.PAGE_AXIS));
		erodPanel.add(erosionLabel);
		erodPanel.add(erosion);
		erodPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
		
		JPanel depoPanel = new JPanel();	// degree of deposition
		depoPanel.setLayout(new BoxLayout(depoPanel, BoxLayout.PAGE_AXIS));
		depoPanel.add(depositionLabel);
		depoPanel.add(deposition);
		depoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		
		// put all the sliders in a 2x2 grid
		JPanel sliders = new JPanel();
		sliders.setLayout(new GridLayout(2,2));
		sliders.add(altPanel);
		sliders.add(erodPanel);
		sliders.add(flatPanel);
		sliders.add(depoPanel);
		
		// create a panel for each button (or set of radio buttons)
		JPanel soil = new JPanel();
		soil.setLayout(new GridLayout(2,2));
		soil.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		soil.add(igneous);
		soil.add(metamorphic);
		soil.add(sedimentary);
		soil.add(alluvial);
		JPanel b1 = new JPanel();
		b1.add(cancel);
		b1.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		JPanel b2 = new JPanel();
		b2.add(accept);
		b2.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		
		// and put them in a 1x3 grid
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(1,3));
		buttons.add(soil);
		buttons.add(b1);
		buttons.add(b2);

		// sliders above, buttons below
		JPanel controls = new JPanel();
		controls.setLayout(new GridLayout(2,1));
		controls.add(sliders);
		controls.add(buttons);
		mainPane.add(controls);
		pack();
		setVisible(true);
		
		// add the action listeners
		altitude.addChangeListener(this);
		flatness.addChangeListener(this);
		erosion.addChangeListener(this);
		deposition.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		map.addMapListener(this);
		map.addKeyListener(this);
		addKeyListener(this);
		igneous.addItemListener(this);
		metamorphic.addItemListener(this);
		sedimentary.addItemListener(this);
		alluvial.addItemListener(this);
		map.requestFocus();
		
		// set us up for point-group selection
		map.selectMode(Map.Selection.POINTS);
		map.checkSelection(Map.Selection.POINTS);
	}

	/**
	 * update the WIP map display to reflect current slider settings
	 */
	void redraw() {
		// get the parameters
		int points = selected_points.length;
		double delta_z = parms.z(altitude.getValue());
		double soil = soilType();
		
		// figure out the vertical range of the selected points
		double z_min = 666.0, z_max = -666.0;
		for(int i = 0; i < points; i++)
			if (selected_points[i]) {
				if (old_height[i] < z_min)
					z_min = old_height[i];
				if (old_height[i] > z_max)
					z_max = old_height[i];
			}
		double z_mid = (z_min + z_max)/2;
		double z_radius = z_max - z_mid;
		double z_mult = multiplier(flatness.getValue());
		
		int v = erosion.getValue();
		double e_mult = (v == 0) ? 1.0 : multiplier(v);
		v = deposition.getValue();
		double d_mult = (v == 0) ? 1.0 : multiplier(v);
		
		// go through and update all of the selected points
		for(int i = 0; i < points; i++) {
			if (!selected_points[i])
				continue;
			/* FIX this is going bad
			// apply the vertical range exaggeration
			double off_middle = old_height[i] - z_mid; 
			new_height[i] = old_height[i] + (off_middle * z_mult / z_radius);
			*/
			new_height[i] = old_height[i];
		
			// apply the general altitude shift
			new_height[i] += delta_z;
			
			// update the soil type (if a type has been selected)
			if (soil >= 0)
				new_soil[i] = soil;
			
			// perform incremental erosion
			double e_meters = e_mult * map.hydro.erosion(i);
			if (e_meters > 0)
				erodeMap[i] = old_erosion[i] + parms.z(e_meters);
		}
		
		// instantiate these updates and redraw the map
		map.setHeightMap(new_height);
		map.setSoilMap(new_soil);
		// no-need to update erodeMap, that being edit-in-place
		map.repaint();
	}
	
	/**
	 * called when a group of points is selected on the map
	 * @param selected	array of per point booleans (true=selected)
	 * @param complete	mouse button has been released
	 * @return	boolean	(should selection continue)
	 */
	public boolean groupSelected(boolean[] selected, boolean complete) {
		selected_points = selected;
		have_selection = true;
		redraw();	// and update the display
		return true;
	}

	/**
	 * restore previous height map and exit dialog
	 */
	private void cancelDialog() {
		// disable any in-progress selection
		map.selectMode(Map.Selection.NONE);
		
		// restore the old height, soil and erosion maps
		map.setHeightMap(old_height);
		map.setSoilMap(old_soil);
		for(int i = 0; i < erodeMap.length; i++)
			erodeMap[i] = old_erosion[i];
		
		map.removeMapListener(this);
		map.removeKeyListener(this);
		map.selectMode(Map.Selection.ANY);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	/**
	 * make the most recently created changes official
	 */
	private void acceptChanges() {
		// make the current height, erosion, and soil-maps official
		int points = new_height.length;
		for(int i = 0; i < points; i++) {
			old_height[i] = new_height[i];
			old_soil[i] = new_soil[i];
			old_erosion[i] = erodeMap[i];
		}
		
		// describe what we have just done
		if (selected_points != null && parms.debug_level >= LAND_DEBUG) {
			points = 0;
			for(int i = 0; i < selected_points.length; i++)
				if (selected_points[i])
					points++;
			String descr = String.format("Updated %d points", points);
			
			double s = soilType();
			if (s >= 0)
				descr += ", soil=" + Map.soil_names[(int)s];
			
			int v = altitude.getValue();
			if (v != 0)
				descr += String.format(", deltaH=%d%s", v, Parameters.unit_z);
			
			v = flatness.getValue();
			if (v > 1)
				descr += String.format(", vertical=*%d", v);
			else if (v < -1)
				descr += String.format(", vertical=/%d", -v);
			
			v = erosion.getValue();
			if (v > 0)
				descr += String.format(", erosion=*%d", v);
			else if (v < 0)
				descr += String.format(", erosion=/%d", -v);
			
			v = deposition.getValue();
			if (v > 0)
				descr += String.format(", deposition=*%d", v);
			else if (v < 0)
				descr += String.format(", deposition=/%d", -v);
			System.out.println(descr);
		}
		
		/*
		 * Reset all controls to neutral values.  Otherwise
		 * accidentally compounded updates get out of hand
		 */
		altitude.setValue(0);
		flatness.setValue(0);
		erosion.setValue(0);
		deposition.setValue(0);
		igneous.setSelected(false);
		metamorphic.setSelected(false);
		sedimentary.setSelected(false);
		alluvial.setSelected(false);
		
		// un-do the current selection
		map.selectMode(Map.Selection.NONE);
		map.selectMode(Map.Selection.POINTS);
		have_selection = false;
	}
				
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		cancelDialog();
	}
	
	/**
	 * updates to the sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (have_selection)
			redraw();
	}
	
	/**
	 * look for ENTER or ESC
	 */
	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER && have_selection)
			acceptChanges();
		else if (key == KeyEvent.VK_ESCAPE)
			cancelDialog();	
	}

	/**
	 * click events on ACCEPT/CANCEL buttons or form selector
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			cancelDialog();
		} else if (e.getSource() == accept && have_selection) {
			acceptChanges();
		}
	}
	
	/**
	 * soil type has changed
	 * 
	 * @param e
	 */
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == igneous && igneous.isSelected()) {
			metamorphic.setSelected(false);
			sedimentary.setSelected(false);
			alluvial.setSelected(false);
		} else if (e.getSource() == metamorphic && metamorphic.isSelected()) {
			igneous.setSelected(false);
			sedimentary.setSelected(false);
			alluvial.setSelected(false);
		} else if (e.getSource() == sedimentary && sedimentary.isSelected()) {
			metamorphic.setSelected(false);
			igneous.setSelected(false);
			alluvial.setSelected(false);
		} else if (e.getSource() == alluvial && alluvial.isSelected()) {
			metamorphic.setSelected(false);
			igneous.setSelected(false);
			sedimentary.setSelected(false);
		}
		if (have_selection)
			redraw();
	}
	
	/**
	 * return the currently selected soil type
	 */
	private double soilType() {
		if (igneous.isSelected())
			return(Map.IGNEOUS);
		if (metamorphic.isSelected())
			return(Map.METAMORPHIC);
		if (alluvial.isSelected())
			return Map.ALLUVIAL;
		if (sedimentary.isSelected())
			return Map.SEDIMENTARY;
		return -1;
	}
	
	/**
	 * convert a (-10 to 10) slider value into a (.1 to 10) multiplier
	 */
	private double multiplier(int value) {
		if (value < -1)
			return -1.0/value;
		else if (value > 1)
			return (double) value;
		else
			return 1.0;
	}
	
	/** (perfunctory) */ public boolean regionSelected(double x, double y, double w, double h, boolean c) {return false;}
	/** (perfunctory) */ public boolean pointSelected(double map_x, double map_y) { return false; }
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	/** (perfunctory) */ public void keyPressed(KeyEvent arg0) {}
	/** (perfunctory) */ public void keyReleased(KeyEvent arg0) {}
}
