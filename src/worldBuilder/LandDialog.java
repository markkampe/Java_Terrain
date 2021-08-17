package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to adjust height, erosion and deposition
 */
public class LandDialog extends JFrame implements ActionListener, ChangeListener, MapListener, KeyListener, WindowListener {	
	
	private Map map;
	private MapWindow window;
	private TerrainEngine t;
	private Parameters parms;
	
	private JSlider altitude;
	private JSlider flatness;
	private JSlider erosion;
	private JSlider deposition;
	private JButton accept;
	private JButton cancel;
	
	private boolean have_selection;		// there is a selected point set
	private boolean changes_made;		// we have uncommitted changes
	private boolean[] selected_points;	// which points are selected
	
	//private static final int LAND_DEBUG = 2;
	//private static final int ERODE_DEBUG = 3;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public LandDialog(Map map)  {
		this.parms = Parameters.getInstance();
		
		// pick up references current maps
		this.map = map;
		this.window = map.window;
		this.t = new TerrainEngine(map);
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Edit height, erosion/deposition");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the buttons and sliders
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT (Enter)");
		cancel = new JButton("CANCEL (Esc)");
		
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
		JPanel b1 = new JPanel();
		b1.add(cancel);
		b1.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		JPanel b2 = new JPanel();
		b2.add(accept);
		b2.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		
		// and put them in a 1x3 grid
		JPanel buttons = new JPanel();
		buttons.setLayout(new GridLayout(1,2));
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
		window.addMapListener(this);
		window.addKeyListener(this);
		addKeyListener(this);
		window.requestFocus();
		
		// set us up for point-group selection
		window.selectMode(MapWindow.Selection.POINTS);
		window.checkSelection(MapWindow.Selection.POINTS);
	}

	/**
	 * update the WIP topology
	 */
	void update() {
		// TerrainEngine can raise/lower or exaggerate, but not both
		double z_mult = multiplier(flatness.getValue());
		double delta_z = parms.z(altitude.getValue());
		if (delta_z != 0) {
			t.raise(selected_points, delta_z);
			changes_made = true;
			altitude.setValue(0);	// reset slider after the change
		} else if (z_mult != 1.0) {
			t.exaggerate(selected_points, z_mult);
			changes_made = true;
			flatness.setValue(0);	// reset slider after the change
		}
		
		// erosion and sedimentation sliders
		double e_factor = multiplier(erosion.getValue());
		if (e_factor != 1.0) {
			t.erosion(selected_points, e_factor);
			changes_made = true;
			erosion.setValue(0);
		}
		double s_factor = multiplier(deposition.getValue());
		if (s_factor != 1.0) {
			t.sedimentation(selected_points, s_factor);
			changes_made = true;
			deposition.setValue(0);
		}
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
		update();
		return true;
	}

	/**
	 * restore previous height map and exit dialog
	 */
	private void cancelDialog() {
		// abort any in-progress changes
		t.abort();
		
		// disable any in-progress selection
		window.selectMode(MapWindow.Selection.NONE);
		
		// delete listeners and lose the dialog
		window.removeMapListener(this);
		window.removeKeyListener(this);
		window.selectMode(MapWindow.Selection.ANY);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	/**
	 * make the most recently created changes official
	 */
	private void acceptChanges() {
		// make the current maps official
		if (changes_made) {
			t.commit();
			map.region_stats();
		}
		
		/*
		 * Reset all controls to neutral values.  Otherwise
		 * accidentally compounded updates get out of hand
		 */
		altitude.setValue(0);
		flatness.setValue(0);
		erosion.setValue(0);
		deposition.setValue(0);
		
		// un-do the current selection
		window.selectMode(MapWindow.Selection.NONE);
		window.selectMode(MapWindow.Selection.POINTS);
		have_selection = false;
		
		// there are no uncommitted changes
		changes_made = false;
	}
				
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		t.abort();
		cancelDialog();
	}
	
	/**
	 * updates to the sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (have_selection) {
			update();
			window.requestFocus();
		}
	}
	
	/*
	 * key events: ENTER, ESCAPE, ^A (select all)
	 */
	private static final int CTRL_A = 1;

	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER && changes_made) {
			acceptChanges();
			changes_made = false;
		} else if (key == KeyEvent.VK_ESCAPE) {
			t.abort();
			window.selectMode(MapWindow.Selection.NONE);	// undo current selection
			window.selectMode(MapWindow.Selection.POINTS);
			changes_made = false;
		}  else if (key == CTRL_A) {
			window.selectAll(true);
		}
	}

	/**
	 * click events on ACCEPT/CANCEL buttons or form selector
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			t.abort();
			cancelDialog();
		} else if (e.getSource() == accept && changes_made) {
			acceptChanges();
		}
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
