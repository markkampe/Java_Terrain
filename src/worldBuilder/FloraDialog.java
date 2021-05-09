package worldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to enable the creation a consistent map of a sub-region of the current world.
 */
public class FloraDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener, KeyListener {	
	// flora types (used for quotas)
	private static final int FLORA_NONE = 0;
	private static final int FLORA_GRASS = 1;
	private static final int FLORA_BRUSH = 2;
	private static final int FLORA_TREE = 3;
	private static final int MAX_CLASSES = 4;
	private static final String[] floraClasses = {"Barren", "Grass", "Brush", "Tree"};
	
	private Map map;
	private Parameters parms;
	private AttributeEngine a;		// attribute placement engine
	private boolean rules_loaded;	// auto-placement rules have been loaded
	
	// placement information
	private int chosen_flora;		// flora type being placed
	private static final int AUTOMATIC = -1;
	
	// widgets
	private JButton accept;			// accept these updates
	private JButton cancel;			// cancel dialog, no updates
	private JTextField flora_palette;	// flora placement rules
	private JButton chooseFlora;	// browse for flora placement trulesnewColors
	private JSlider flora_pct;		// fraction of area to be covered
	private RangeSlider flora_3;	// grass/brush/tree distribution
	JLabel mode;					// auto/manual mode indication
	JMenu modeMenu;					// mode selection menu
	JMenuItem ruleMode;				// automatic (rule-based) selection
	
	// selected region info
	private boolean changes_made;	// we have displayed uncommitted updates
	private boolean[] whichPoints;	// which points have been selected
	
	private static final String AUTO_NAME = "Rule Based";
	
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public FloraDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		this.a = new AttributeEngine(map);
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Flora Placement");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create a panel for flora selection widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		// manual vs rule-based assignment
		JPanel mPanel = new JPanel(new GridLayout(1,3));
		modeMenu = new JMenu("Flora Selection");
		ruleMode = new JMenuItem(AUTO_NAME);
		modeMenu.add(ruleMode);
		ruleMode.addActionListener(this);
		modeMenu.addSeparator();
		String[] choices = map.floraNames;
		for(int i = 0; i < choices.length; i++) 
			if (choices[i] != null) {
				JMenuItem item = new JMenuItem(choices[i]);
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

		// flora rules selection field
		flora_palette = new JTextField(parms.flora_rules);
		JLabel fTitle = new JLabel("Flora Palette", JLabel.CENTER);
		chooseFlora = new JButton("Browse");
		fTitle.setFont(fontLarge);
		JPanel fPanel = new JPanel(new GridLayout(2, 1));
		fPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		fPanel.add(fTitle);
		JPanel f1_panel = new JPanel();
		f1_panel.setLayout(new BoxLayout(f1_panel, BoxLayout.LINE_AXIS));
		f1_panel.add(flora_palette);
		f1_panel.add(Box.createRigidArea(new Dimension(40, 0)));
		f1_panel.add(chooseFlora);
		fPanel.add(f1_panel);
		locals.add(fPanel);
		chooseFlora.addActionListener(this);
	
		// plant coverage slider
		flora_pct = new JSlider(JSlider.HORIZONTAL, 0, 100, parms.dFloraPct);
		flora_pct.setMajorTickSpacing(10);
		flora_pct.setMinorTickSpacing(5);
		flora_pct.setFont(fontSmall);
		flora_pct.setPaintTicks(true);
		flora_pct.setPaintLabels(true);
		fTitle = new JLabel("Plant Cover (percentage)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(flora_pct);
		locals.add(fTitle);
		flora_pct.addChangeListener(this);

		// flora type slider
		fPanel = new JPanel(new GridLayout(1, 3));
		JLabel fT1 = new JLabel(floraClasses[1]);
		fT1.setFont(fontLarge);
		fPanel.add(fT1);
		JLabel fT2 = new JLabel(floraClasses[2], JLabel.CENTER);
		fT2.setFont(fontLarge);
		fPanel.add(fT2);
		JLabel fT3 = new JLabel(floraClasses[3], JLabel.RIGHT);
		fT3.setFont(fontLarge);
		fPanel.add(fT3);
		flora_3 = new RangeSlider(0, 100);
		flora_3.setValue(parms.dFloraMin);
		flora_3.setUpperValue(parms.dFloraMax);
		flora_3.setMajorTickSpacing(10);
		flora_3.setMinorTickSpacing(5);
		flora_3.setFont(fontSmall);
		flora_3.setPaintTicks(true);
		flora_3.setPaintLabels(true);

		fTitle = new JLabel("Flora Distribution (percentile)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(fPanel);
		locals.add(flora_3);
		locals.add(fTitle);
		flora_3.addChangeListener(this);
		
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
		chosen_flora = AUTOMATIC;
		mode.setText(AUTO_NAME);
	}
	
	/**
	 * (re-)determine the plant coverage of each selected mesh point
	 */
	private void placeFlora() {
		// compute the per-class fractional quotas
		double[] quotas = new double[MAX_CLASSES];
		quotas[FLORA_NONE] = 1.0;
		double density = flora_pct.getValue()/100.0;
		quotas[FLORA_GRASS] = density * flora_3.getValue() / 100.0;
		quotas[FLORA_TREE] = density * (1.0 - (flora_3.getUpperValue()/100.0));
		quotas[FLORA_BRUSH] = density * (flora_3.getUpperValue() - flora_3.getValue())/100.0;
		
		// let the attribute auto-placement engine do the work
		if (!rules_loaded)
			rules_loaded = a.placementRules(flora_palette.getText(), floraClasses, AttributeEngine.WhichMap.FLORA);
		a.autoPlacement(whichPoints,  quotas, AttributeEngine.WhichMap.FLORA);
	}


	/**
	 * called when map points are selected
	 * @param selected boolean per MeshPoint selected or not
	 * @param selection complete (mouse button no longer down)
	 */
	public boolean groupSelected(boolean[] selected, boolean complete) {
		whichPoints = selected;
		if (complete) {
			if (chosen_flora == AUTOMATIC)
				placeFlora();
			else
				a.placement(whichPoints, AttributeEngine.WhichMap.FLORA, chosen_flora);
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
			if (e.getSource() == flora_pct) {
				placeFlora();
			} else if (e.getSource() == flora_3) {
				placeFlora();
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
	private void accept() {
		// tell attribute engine to make it so
		a.commit();
		changes_made = false;
		
		// remember the chosen palette and percentages
		parms.flora_rules = flora_palette.getText();
		parms.dFloraPct = flora_pct.getValue();
		parms.dFloraMin = flora_3.getValue();
		parms.dFloraMax = flora_3.getUpperValue();
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
		} else if (e.getSource() == chooseFlora) {
			FileDialog d = new FileDialog(this, "Floral Palette", FileDialog.LOAD);
			d.setFile(flora_palette.getText());
			d.setVisible(true);
			String palette_file = d.getFile();
			if (palette_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					palette_file = dir + palette_file;
				flora_palette.setText(palette_file);
				rules_loaded = false;	// this will need to be re-loaded
			}
		} else if (e.getSource() == cancel) {
			cancelDialog();
		} else {	// most likely a menu item
			JMenuItem item = (JMenuItem) e.getSource();
			String chosen = item.getText();
			if (item == ruleMode) {
				// go back to rule-based selection
				chooseFlora.setEnabled(true);
				flora_palette.setEnabled(true);
				flora_pct.setEnabled(true);
				flora_3.setEnabled(true);
				chosen_flora = AUTOMATIC;
				if (whichPoints != null)
					placeFlora();
			} else {
				// manual choice and placement
				chooseFlora.setEnabled(false);
				flora_palette.setEnabled(false);
				flora_pct.setEnabled(false);
				flora_3.setEnabled(false);
				chosen_flora = map.getFloraType(chosen);
				if (whichPoints != null) {
					a.placement(whichPoints, AttributeEngine.WhichMap.FLORA, chosen_flora);
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
