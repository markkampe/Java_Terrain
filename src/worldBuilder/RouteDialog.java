package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to lay out trade routes
 */
public class RouteDialog extends JFrame implements ActionListener, ChangeListener, MapListener, KeyListener, WindowListener {	
	
	private Map map;
	private MapWindow window;
	private TerritoryEngine te;
	private Parameters parms;
	
	private JSlider x_cost;
	private JSlider z_cost;
	private JSlider w_cost;
	private JSlider day_len;
	private JSlider max_days;
	private JTextField travel_time;
	private JButton automatic;
	private JButton accept;
	private JButton cancel;
	
	private double x_start, y_start;// currently selected start coordinates
	private double x_end, y_end;	// currently selected end coordinates
	private boolean auto_changes;	// there are uncommited automatic changes
	private boolean manual_changes;	// there are uncommited manual changes
	private int last_start;			// point index of last manual route start
	private int last_end;			// point index of last manual route end
	
	// slider ranges
	private static final int KM_MIN = 10;
	private static final int KM_MAX = 60;
	private static final int M1K_MIN = 60;
	private static final int M1K_MAX = 300;
	private static final int FORD_MIN = 10;
	private static final int FORD_MAX = 100;
	private static final int MIN_DAY = 300;
	private static final int MAX_DAY = 600;
	private static final int MIN_DAYS = 2;
	private static final int MAX_DAYS = 20;
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public RouteDialog(Map map)  {
		// pick up references
		this.map = map;
		this.window = map.window;
		te = new TerritoryEngine(map);
		this.parms = Parameters.getInstance();
	
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Trade Routes");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		automatic = new JButton("Automatic");
		accept = new JButton("ACCEPT (Enter)");
		cancel = new JButton("CANCEL (Esc)");
		
		x_cost = new JSlider(JSlider.HORIZONTAL, KM_MIN, KM_MAX, (int) parms.dTimeTravel);
		x_cost.setMajorTickSpacing(Parameters.niceTics(KM_MIN, KM_MAX, true));
		x_cost.setFont(fontSmall);
		x_cost.setPaintTicks(true);
		x_cost.setPaintLabels(true);
		
		z_cost = new JSlider(JSlider.HORIZONTAL, M1K_MIN, M1K_MAX, (int) parms.dTimeClimb);
		z_cost.setMajorTickSpacing(Parameters.niceTics(M1K_MIN, M1K_MAX, true));
		z_cost.setFont(fontSmall);
		z_cost.setPaintTicks(true);
		z_cost.setPaintLabels(true);
		
		w_cost = new JSlider(JSlider.HORIZONTAL, FORD_MIN, FORD_MAX, (int) parms.dTimeCross);
		w_cost.setMajorTickSpacing(Parameters.niceTics(FORD_MIN, FORD_MAX, true));
		w_cost.setFont(fontSmall);
		w_cost.setPaintTicks(true);
		w_cost.setPaintLabels(true);
		
		day_len = new JSlider(JSlider.HORIZONTAL, MIN_DAY, MAX_DAY, (int) parms.dTravelDay);
		day_len.setMajorTickSpacing(Parameters.niceTics(MIN_DAY, MAX_DAY, true));
		day_len.setFont(fontSmall);
		day_len.setPaintTicks(true);
		day_len.setPaintLabels(true);
		
		max_days = new JSlider(JSlider.HORIZONTAL, MIN_DAYS, MAX_DAYS, (int) parms.dTravelMax);
		max_days.setMajorTickSpacing(Parameters.niceTics(MIN_DAYS, MAX_DAYS, true));
		max_days.setFont(fontSmall);
		max_days.setPaintTicks(true);
		max_days.setPaintLabels(true);
		
		travel_time = new JTextField("");
		travel_time.setEditable(false);
		
		/*
		 * 
		 * Then pack all the controls into a 3x3 grid
		 */
		JPanel xPanel = new JPanel();
		xPanel.setLayout(new BoxLayout(xPanel, BoxLayout.PAGE_AXIS));
		JLabel top = new JLabel("Time to travel 1" + Parameters.unit_xy, SwingConstants.CENTER);
		top.setFont(fontLarge);
		xPanel.add(top);
		xPanel.add(x_cost);
		JLabel min = new JLabel("minutes", SwingConstants.CENTER);
		min.setFont(fontSmall);
		xPanel.add(min);
		xPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel zPanel = new JPanel();
		zPanel.setLayout(new BoxLayout(zPanel, BoxLayout.PAGE_AXIS));
		top = new JLabel("Time to gain 1000" + Parameters.unit_z, SwingConstants.CENTER);
		top.setFont(fontLarge);
		zPanel.add(top);
		zPanel.add(z_cost);
		min = new JLabel("minutes", SwingConstants.CENTER);
		min.setFont(fontSmall);
		zPanel.add(min);
		zPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel wPanel = new JPanel();
		wPanel.setLayout(new BoxLayout(wPanel, BoxLayout.PAGE_AXIS));
		top = new JLabel("Time to cross 1" + Parameters.unit_f, SwingConstants.CENTER);
		top.setFont(fontLarge);
		wPanel.add(top);
		wPanel.add(w_cost);
		min = new JLabel("minutes", SwingConstants.CENTER);
		min.setFont(fontSmall);
		wPanel.add(min);
		wPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel tPanel = new JPanel();
		tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.PAGE_AXIS));
		top = new JLabel("New route travel time", SwingConstants.CENTER);
		top.setFont(fontLarge);
		tPanel.add(top);
		tPanel.add(travel_time);
		min = new JLabel("days", SwingConstants.CENTER);
		min.setFont(fontSmall);
		tPanel.add(min);
		tPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel dPanel = new JPanel();
		dPanel.setLayout(new BoxLayout(dPanel, BoxLayout.PAGE_AXIS));
		top = new JLabel("Travel time per day", SwingConstants.CENTER);
		top.setFont(fontLarge);
		dPanel.add(top);
		dPanel.add(day_len);
		min = new JLabel("minutes", SwingConstants.CENTER);
		min.setFont(fontSmall);
		dPanel.add(min);
		dPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel mPanel = new JPanel();
		mPanel.setLayout(new BoxLayout(mPanel, BoxLayout.PAGE_AXIS));
		top = new JLabel("Max journey between cities", SwingConstants.CENTER);
		top.setFont(fontLarge);
		mPanel.add(top);
		mPanel.add(max_days);
		min = new JLabel("days", SwingConstants.CENTER);
		min.setFont(fontSmall);
		mPanel.add(min);
		mPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		JPanel p7 = new JPanel();
		p7.add(automatic);
		p7.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		JPanel p8 = new JPanel();
		p8.add(cancel);
		p8.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));
		JPanel p9 = new JPanel();
		p9.add(accept);
		p9.setBorder(BorderFactory.createEmptyBorder(20, 15, 20, 15));

		// pack them all together into a 3x3 box
		JPanel controls = new JPanel();
		controls.setLayout(new GridLayout(3,3));
		controls.add(xPanel);
		controls.add(zPanel);
		controls.add(wPanel);
		controls.add(tPanel);
		controls.add(dPanel);
		controls.add(mPanel);
		
		controls.add(p7);
		controls.add(p8);
		controls.add(p9);
		
		mainPane.add(controls);
		pack();
		setVisible(true);
		manual_changes = false;
		auto_changes = false;
		
		// add the other widget action listeners
		x_cost.addChangeListener(this);
		z_cost.addChangeListener(this);
		w_cost.addChangeListener(this);
		day_len.addChangeListener(this);
		max_days.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		automatic.addActionListener(this);
		window.addMapListener(this);
		window.addKeyListener(this);
		addKeyListener(this);

		window.requestFocus();
		
		// set us up for line selection
		window.selectMode(MapWindow.Selection.LINE);
		window.checkSelection(MapWindow.Selection.LINE);
	}
	
	/**
	 * called whenever a (Map) region selection changes
	 * @param map_x		left most point (map coordinate)
	 * @param map_y		upper most point (map coordinate)
	 * @param width		(in map units, can be negative)
	 * @param height	(in map units, can be negative)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(
			double map_x, double map_y, 
			double width, double height,
			boolean complete) {
		
		this.x_start = map_x;
		this.y_start = map_y;
		this.x_end = map_x + width;
		this.y_end = map_y + height;
		if (complete) {
			last_start = map.mesh.choosePoint(x_start, y_start).index;
			last_end = map.mesh.choosePoint(x_end, y_end).index;
			manual_route();
		} else {
			last_start = -1;
			last_end = -1;
		}
		window.requestFocus();
		return true;
	}

	/**
	 * (re) draw the last-selected manual route
	 */
	private void manual_route() {
		int valid = 0;
		if (te.startFrom(last_start))
			valid++;
		if (te.startFrom(last_end))
			valid++;
		if (valid > 0) {
			te.reset();
			set_parameters();
			TradeRoute added = te.outwards(1, valid == 1);
			if (added != null)
				travel_time.setText(String.format("%.1f days", added.cost));
		}
		window.repaint();
		manual_changes = true;
	}
	
	/**
	 * (re)perform automatic route selection
	 */
	private void auto_route() {
		te.reset();	// start from scratch
		set_parameters();
		te.allCities();
		auto_changes = true;
		window.repaint();
	}
	
	/**
	 * load the current slider values into the TerritoryEngine
	 */
	private void set_parameters() {
		te.set_parms(x_cost.getValue(), z_cost.getValue(), w_cost.getValue(),
					 day_len.getValue(), max_days.getValue());
	}
	
	/**
	 * updates to the parameter sliders
	 */
	public void stateChanged(ChangeEvent e) {
			te.abort();		// everything changes
			if (auto_changes)
				auto_route();
			if (manual_changes)
				manual_route();
			window.requestFocus();
	}
	
	/**
	 * restore previous height map and exit dialog
	 */
	private void cancelDialog() {
		// back out any uncommitted updates
		if (manual_changes || auto_changes)
			te.abort();
		
		// un-register our listeners and end the dialog
		window.selectMode(MapWindow.Selection.NONE);
		window.removeMapListener(this);
		window.removeKeyListener(this);
		window.selectMode(MapWindow.Selection.ANY);
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		cancelDialog();
	}
	
	/**
	 * look for ENTER or ESC
	 */
	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER && (manual_changes || auto_changes)) {
			te.commit();
			manual_changes = false;
			auto_changes = false;
		} else if (key == KeyEvent.VK_ESCAPE) {
			// cancel the last updates
			if (manual_changes || auto_changes) {
				te.abort();
				window.repaint();
				manual_changes = false;
				auto_changes = false;
			}
			
			// undo the last region selection
			window.selectMode(MapWindow.Selection.NONE);
			window.selectMode(MapWindow.Selection.LINE);
			last_start = -1;
			last_end = -1;
		}
		window.requestFocus();
	}

	/**
	 * click events on ACCEPT/CANCEL buttons or form selector
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			cancelDialog();
		} else if (e.getSource() == accept && (auto_changes || manual_changes)) {
			te.commit();
			auto_changes = false;
			manual_changes = false;
			window.requestFocus();
		} else if (e.getSource() == automatic) {
			te.abort();
			auto_route();
			window.requestFocus();
		}
	}
	
	/** (perfunctory) */ public boolean pointSelected(double map_x, double map_y) { return false; }
	/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	/** (perfunctory) */ public void keyPressed(KeyEvent arg0) {}
	/** (perfunctory) */ public void keyReleased(KeyEvent arg0) {}
}
