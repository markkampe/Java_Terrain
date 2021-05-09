package worldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to enable the setting of MeshPoint attributes (flora, minerals, etc)
 */
public class ResourceDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener, KeyListener {	
	// flora types (used for quotas)
	private static final int MAX_CLASSES = 4;
	private String[] rsrcClasses = {"NONE", "FIRST", "SECOND", "THIRD"};
	private String[] rsrcNames;		// map resource type into a name
	
	private Map map;
	private Parameters parms;
	private AttributeEngine a;		// attribute placement engine
	private boolean rules_loaded;	// auto-placement rules have been loaded
	
	// placement information
	private int chosen_type;		// resource type being placed
	private static final int AUTOMATIC = -1;
	
	// widgets
	private JButton accept;			// accept these updates
	private JButton cancel;			// cancel dialog, no updates
	protected JTextField rsrc_rules;// resource placement rules file
	private JButton chooseRsrc;		// browse for resource placement rules
	protected JSlider rsrc_pct;		// fraction of area to be covered
	protected JLabel title_pct;		// title of total coverage slider
	protected RangeSlider rsrc_3;	// distribution among the three classes
	protected JLabel title_3;		// title of distribution slider
	private JLabel mode;			// auto/manual mode indication
	private JMenu modeMenu;			// mode selection menu
	private JMenuItem ruleMode;		// automatic (rule-based) selection
	
	// selected region info
	private boolean changes_made;	// we have displayed uncommitted updates
	private boolean[] whichPoints;	// which points have been selected
	private AttributeEngine.WhichMap whichMap;	// type of resource
	
	private static final String AUTO_NAME = "Rule Based";
	
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public ResourceDialog(Map map, AttributeEngine.WhichMap maptype, String[] classNames, String[] rsrcNames)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		this.whichMap = maptype;
		this.rsrcClasses = classNames;
		this.rsrcNames = rsrcNames;
		this.a = new AttributeEngine(map);
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Resource Placement");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create a panel for resource type selection widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		// manual vs rule-based assignment
		JPanel mPanel = new JPanel(new GridLayout(1,3));
		modeMenu = new JMenu("Resource Selection");
		ruleMode = new JMenuItem(AUTO_NAME);
		modeMenu.add(ruleMode);
		ruleMode.addActionListener(this);
		modeMenu.addSeparator();
		
		// build up a menu of the supported resource sub-types
		for(int i = 0; i < rsrcNames.length; i++) 
			if (rsrcNames[i] != null) {
				JMenuItem item = new JMenuItem(rsrcNames[i]);
				modeMenu.add(item);
				item.addActionListener(this);
			}
		JMenuBar bar = new JMenuBar();
		bar.add(modeMenu);
		mPanel.add(bar);
		mode = new JLabel("");
		mPanel.add(mode);
		mPanel.add(new JLabel(" "));
		locals.add(mPanel);

		// Resource rules file selection
		rsrc_rules = new JTextField("");
		JLabel fTitle = new JLabel("Placement Rules", JLabel.CENTER);
		chooseRsrc = new JButton("Browse");
		fTitle.setFont(fontLarge);
		JPanel fPanel = new JPanel(new GridLayout(2, 1));
		fPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		fPanel.add(fTitle);
		JPanel f1_panel = new JPanel();
		f1_panel.setLayout(new BoxLayout(f1_panel, BoxLayout.LINE_AXIS));
		f1_panel.add(rsrc_rules);
		f1_panel.add(Box.createRigidArea(new Dimension(40, 0)));
		f1_panel.add(chooseRsrc);
		fPanel.add(f1_panel);
		locals.add(fPanel);
		chooseRsrc.addActionListener(this);
	
		// coverage slider
		rsrc_pct = new JSlider(JSlider.HORIZONTAL, 0, 100, 50); // sub-class will override
		rsrc_pct.setMajorTickSpacing(10);
		rsrc_pct.setMinorTickSpacing(5);
		rsrc_pct.setFont(fontSmall);
		rsrc_pct.setPaintTicks(true);
		rsrc_pct.setPaintLabels(true);
		title_pct = new JLabel("Overall Density (percentage)");
		title_pct.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(rsrc_pct);
		locals.add(title_pct);
		rsrc_pct.addChangeListener(this);

		// distribution slider
		fPanel = new JPanel(new GridLayout(1, 3));
		JLabel fT1 = new JLabel(rsrcClasses[1]);
		fT1.setFont(fontLarge);
		fPanel.add(fT1);
		JLabel fT2 = new JLabel(rsrcClasses[2], JLabel.CENTER);
		fT2.setFont(fontLarge);
		fPanel.add(fT2);
		JLabel fT3 = new JLabel(rsrcClasses[3], JLabel.RIGHT);
		fT3.setFont(fontLarge);
		fPanel.add(fT3);
		rsrc_3 = new RangeSlider(0, 100);
		rsrc_3.setValue(40);		// sub-class will override
		rsrc_3.setUpperValue(80);	// sub-class will override
		rsrc_3.setMajorTickSpacing(10);
		rsrc_3.setMinorTickSpacing(5);
		rsrc_3.setFont(fontSmall);
		rsrc_3.setPaintTicks(true);
		rsrc_3.setPaintLabels(true);

		title_3 = new JLabel("Distribution (percentile)");
		title_3.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(fPanel);
		locals.add(rsrc_3);
		locals.add(title_3);
		rsrc_3.addChangeListener(this);
		
		// put all the sliders in the middle of the pane
		mainPane.add(locals);
		
		// put the control buttons at the bottom
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");
		JPanel buttons = new JPanel(new GridLayout(1, 5));
		buttons.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 15));
		buttons.add(new JLabel(" "));
		buttons.add(cancel);
		buttons.add(new JLabel(" "));
		buttons.add(accept);
		buttons.add(new JLabel(" "));
		accept.addActionListener(this);
		cancel.addActionListener(this);
		mainPane.add(buttons, BorderLayout.SOUTH);

		// put it up
		pack();
		setVisible(true);
		
		// get keyboard input
		addKeyListener(this);
		map.addKeyListener(this);
		map.requestFocus();

		// get region selection input
		changes_made = false;
		map.addMapListener(this);	
		map.selectMode(Map.Selection.POINTS);
		map.checkSelection(Map.Selection.POINTS);
		
		// we start out with rule-based placement
		chosen_type = AUTOMATIC;
		mode.setText(AUTO_NAME);
	}
	
	/**
	 * (re-)determine the coverage of each selected mesh point
	 */
	private void autoPlace() {
		// compute the per-class fractional quotas
		double[] quotas = new double[MAX_CLASSES];
		quotas[0] = 1.0;
		double density = rsrc_pct.getValue()/100.0;
		quotas[1] = density * rsrc_3.getValue() / 100.0;
		quotas[3] = density * (1.0 - (rsrc_3.getUpperValue()/100.0));
		quotas[2] = density * (rsrc_3.getUpperValue() - rsrc_3.getValue())/100.0;
		
		// let the attribute auto-placement engine do the work
		if (!rules_loaded)
			rules_loaded = a.placementRules(rsrc_rules.getText(), rsrcClasses, whichMap);
		a.autoPlacement(whichPoints,  quotas, whichMap);
	}


	/**
	 * called when map points are selected
	 * @param selected boolean per MeshPoint selected or not
	 * @param selection complete (mouse button no longer down)
	 */
	public boolean groupSelected(boolean[] selected, boolean complete) {
		whichPoints = selected;
		if (complete) {
			if (chosen_type == AUTOMATIC)
				autoPlace();
			else
				a.placement(whichPoints, whichMap, chosen_type);
			changes_made = true;
		}
		return true;
	}
	
	/**
	 * Slider changes
	 */
	public void stateChanged(ChangeEvent e) {
		// if we have selected points, make it so
		if (whichPoints != null) {
			if (e.getSource() == rsrc_pct) {
				autoPlace();
			} else if (e.getSource() == rsrc_3) {
				autoPlace();
			}
		}
		map.requestFocus();
	}
	
	/**
	 * unregister our map listener and close the dialog
	 */
	private void cancelDialog() {
		if (changes_made) {
			undo();
		}

		// cease to listen to selection events
		map.selectMode(Map.Selection.ANY);
		map.removeMapListener(this);
		map.removeKeyListener(this);

		// close the window
		this.dispose();
		WorldBuilder.activeDialog = false;
	}
	
	/**
	 * make pending changes official
	 */
	protected void accept() {
		// tell attribute engine to make it so
		a.commit();
		changes_made = false;
	}

	/**
	 * back out any uncommitted updates
	 */
	public void undo() {
		// return to last committed flora map
		a.abort();
	}
	
	/**
	 * ENTER/ESC for accept or undo
	 */
	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER)
			accept();
		else if (key == KeyEvent.VK_ESCAPE)
			undo();
		
		// clear the (just committed) selection
		map.selectMode(Map.Selection.NONE);
		whichPoints = null;
		map.selectMode(Map.Selection.POINTS);
	}

	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		cancelDialog();
	}

	/**
	 * click events on one of the buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == accept) {
			if (changes_made)
				accept();
		} else if (e.getSource() == chooseRsrc) {
			FileDialog d = new FileDialog(this, "Resource Rules", FileDialog.LOAD);
			d.setFile(rsrc_rules.getText());
			d.setVisible(true);
			String palette_file = d.getFile();
			if (palette_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					palette_file = dir + palette_file;
				rsrc_rules.setText(palette_file);
				rules_loaded = false;	// this will need to be re-loaded
			}
		} else if (e.getSource() == cancel) {
			cancelDialog();
		} else {	// most likely a menu item
			JMenuItem item = (JMenuItem) e.getSource();
			String chosen = item.getText();
			if (item == ruleMode) {
				// go back to rule-based selection
				chooseRsrc.setEnabled(true);
				rsrc_rules.setEnabled(true);
				rsrc_pct.setEnabled(true);
				rsrc_3.setEnabled(true);
				chosen_type = AUTOMATIC;
				if (whichPoints != null)
					autoPlace();
			} else {
				// manual choice and placement
				chooseRsrc.setEnabled(false);
				rsrc_rules.setEnabled(false);
				rsrc_pct.setEnabled(false);
				rsrc_3.setEnabled(false);
				for(chosen_type = 0; chosen_type < rsrcNames.length; chosen_type++)
					if (chosen.equals(rsrcNames[chosen_type]))
						break;
				if (whichPoints != null) {
					a.placement(whichPoints, whichMap, chosen_type);
					changes_made = true;
				}
			}
			mode.setText(chosen);
		}
		map.requestFocus();
	}

	/** (perfunctory)*/ public boolean regionSelected(double mx0, double my0, double dx, double dy, boolean complete) {	return false;}
	/** (perfunctory) */ public boolean pointSelected(double x, double y) {return false;}
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	/** (perfunctory) */ public void keyPressed(KeyEvent arg0) {}
	/** (perfunctory) */ public void keyReleased(KeyEvent arg0) {}
}
