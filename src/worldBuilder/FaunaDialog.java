package worldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to enable the distribution of Fauna across MeshPoints
 */
public class FaunaDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener {	
	
	private Map map;
	private Parameters parms;
	private Placement placer;		// placement engine
	
	// placement information
	private double faunaMap[];		// per mesh-point fauna types
	private double prevFauna[];		// saved fauna Map
	private Color prevColors[];		// saved type to preview color map
	private String prevNames[];		// saved type to name map
	private int classCounts[];		// returned placements per class
	private int chosen_type;		// fauna type being placed
	
	// widgets
	private JLabel mode;			// auto/manual mode
	private JMenu modeMenu;			// mode selection menu
	private JMenuItem ruleMode;		// automatic (rule based) selection
	private JButton accept;			// accept these updates
	private JButton cancel;			// cancel dialog, no updates
	private JTextField fauna_palette;	// mineral placement rules
	private JButton chooseFauna;	// browse for flora placement trulesnewColors
	private JSlider fauna_pct;		// fraction of area to be covered
	private RangeSlider fauna_3;	// relative mineral distribution

	// selected region info
	private boolean selected;		// a region has been selected
	private double x0, y0;			// upper left hand corner
	private double width, height;	// selected area size (in pixels)
	private boolean changes_made;	// we have displayed updates

	private static final int FAUNA_NONE = 0;
	private static final int FAUNA_BIRDS = 1;
	private static final int FAUNA_SMALL = 2;
	private static final int FAUNA_LARGE = 3;
	private static final int MAX_TYPES = 4;
	private static final String[] faunaClasses = {"None", "Birds", "Small Game", "Large Game" };
	
	private static final String AUTO_NAME = "Rule Based";
	private static final int AUTOMATIC = -1;
	
	// multiple selections are additive (vs replacement)
	private boolean progressive = false;

	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public FaunaDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// get incoming Flora Map and its preview colors
		prevFauna = map.getFaunaMap();
		prevColors = map.getFaunaColors();
		prevNames = map.getFaunaNames();
		faunaMap = new double[prevFauna.length];
		for(int i = 0; i < prevFauna.length; i++)
			faunaMap[i] = prevFauna[i];
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Fauna Placement");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create a panel for mineral selection widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		// manual vs rule-based assignment
		JPanel mPanel = new JPanel(new GridLayout(1,3));
		modeMenu = new JMenu("Fauna Selection");
		ruleMode = new JMenuItem(AUTO_NAME);
		modeMenu.add(ruleMode);
		ruleMode.addActionListener(this);
		modeMenu.addSeparator();
		String[] choices = map.getFaunaNames();
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
		fauna_palette = new JTextField(parms.fauna_rules);
		JLabel fTitle = new JLabel("Fauna Palette", JLabel.CENTER);
		chooseFauna = new JButton("Browse");
		fTitle.setFont(fontLarge);
		JPanel fPanel = new JPanel(new GridLayout(2, 1));
		fPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		fPanel.add(fTitle);
		JPanel f1_panel = new JPanel();
		f1_panel.setLayout(new BoxLayout(f1_panel, BoxLayout.LINE_AXIS));
		f1_panel.add(fauna_palette);
		f1_panel.add(Box.createRigidArea(new Dimension(40, 0)));
		f1_panel.add(chooseFauna);
		fPanel.add(f1_panel);
		locals.add(fPanel);
		chooseFauna.addActionListener(this);
	
		// mineral coverage slider
		fauna_pct = new JSlider(JSlider.HORIZONTAL, 0, 100, parms.dFaunaPct);
		fauna_pct.setMajorTickSpacing(10);
		fauna_pct.setMinorTickSpacing(5);
		fauna_pct.setFont(fontSmall);
		fauna_pct.setPaintTicks(true);
		fauna_pct.setPaintLabels(true);
		fTitle = new JLabel("Fauna Density (percentage)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(fauna_pct);
		locals.add(fTitle);
		fauna_pct.addChangeListener(this);

		// flora type slider
		fPanel = new JPanel(new GridLayout(1, 3));
		JLabel fT1 = new JLabel(faunaClasses[1]);
		fT1.setFont(fontLarge);
		fPanel.add(fT1);
		JLabel fT2 = new JLabel(faunaClasses[2], JLabel.CENTER);
		fT2.setFont(fontLarge);
		fPanel.add(fT2);
		JLabel fT3 = new JLabel(faunaClasses[3], JLabel.RIGHT);
		fT3.setFont(fontLarge);
		fPanel.add(fT3);
		fauna_3 = new RangeSlider(0, 100);
		fauna_3.setValue(parms.dFaunaMin);
		fauna_3.setUpperValue(parms.dFaunaMax);
		fauna_3.setMajorTickSpacing(10);
		fauna_3.setMinorTickSpacing(5);
		fauna_3.setFont(fontSmall);
		fauna_3.setPaintTicks(true);
		fauna_3.setPaintLabels(true);

		fTitle = new JLabel("Relative Abundance (percentile)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(fPanel);
		locals.add(fauna_3);
		locals.add(fTitle);
		fauna_3.addChangeListener(this);
		
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
		
		// get region selection input
		changes_made = false;
		map.addMapListener(this);	
		map.selectMode(Map.Selection.RECTANGLE);
		selected = map.checkSelection(Map.Selection.RECTANGLE);
		
		// we start out with rule-based placement
		chosen_type = AUTOMATIC;
		mode.setText(AUTO_NAME);
	}
	
	/**
	 * (re-)determine the mineral types of each selected mesh point
	 */
	private void rulePlacement() {
		if (placer == null)
			placer = new Placement(fauna_palette.getText(), map, faunaMap);
		
		// count and initialize the points to be populated
		double[] heightMap = map.getHeightMap();
		double[] erodeMap = map.getErodeMap();
		double[] waterLevel = map.getWaterLevel();
		int land_points = 0, water_points = 0;
		MeshPoint[] points = map.mesh.vertices;
		for(int i = 0; i < faunaMap.length; i++)
			if (points[i].x >= x0 && points[i].x < x0+width &&
				points[i].y >= y0 && points[i].y < y0+height) {
				faunaMap[i] = FAUNA_NONE;
				if (waterLevel[i] < heightMap[i] - erodeMap[i])
					land_points++;
				else
					water_points++;
			}

		// figure out the per-class quotas (in MeshPoints)
		int quotas[] = new int[MAX_TYPES];
		land_points = (land_points * fauna_pct.getValue())/100;
		quotas[FAUNA_LARGE] = (land_points * (100 - fauna_3.getUpperValue()))/100;
		quotas[FAUNA_BIRDS] = (land_points * fauna_3.getValue())/100;
		quotas[FAUNA_SMALL] = land_points - (quotas[FAUNA_LARGE] + quotas[FAUNA_BIRDS]);
		quotas[FAUNA_NONE] = (water_points * fauna_pct.getValue())/100;
		
		// assign flora types for each MeshPoint
		classCounts = placer.update(x0, y0, height, width, quotas, faunaClasses);
	
		// instantiate (and display) the updated flora map
		map.setFaunaColors(placer.previewColors());
		map.setFaunaNames(placer.resourceNames());
		map.setFaunaMap(faunaMap);
		map.repaint();
		changes_made = true;
	}
	
	private void manualPlacement() {
		MeshPoint[] points = map.mesh.vertices;
		int count = 0;
		for(int i = 0; i < faunaMap.length; i++)
			if (points[i].x >= x0 && points[i].x < x0+width &&
				points[i].y >= y0 && points[i].y < y0+height) {
				faunaMap[i] = chosen_type;
				count++;
			}
		
		map.setFaunaMap(faunaMap);
		map.repaint();
		if (parms.debug_level > 0)
			System.out.println("Fauna Placement: " + prevNames[chosen_type] + 
								"(" + chosen_type + ") = " + count);
	}

	/**
	 * called whenever a region selection changes
	 * @param mx0	left most point (map coordinate)
	 * @param my0	upper most point (map coordinate)
	 * @param dx	width (in map units)
	 * @param dy	height (in map units)
	 * @param complete	boolean, has selection completed
	 * 
	 * @return	boolean	(should selection continue)
	 */
	public boolean regionSelected(double mx0, double my0, 
								  double dx, double dy, boolean complete) {		
		if (changes_made && !progressive) {
			for(int i = 0; i < faunaMap.length; i++)
				faunaMap[i] = prevFauna[i];
			changes_made = false;
		}
		selected = complete;
		x0 = mx0;
		y0 = my0;
		width = dx;
		height = dy;
		if (complete)
			if (chosen_type == AUTOMATIC)
				rulePlacement();
			else
				manualPlacement();
		return true;
	}

	/**
	 * Slider changes
	 */
	public void stateChanged(ChangeEvent e) {
		if (selected)
			rulePlacement();
	}
	
	/**
	 * unregister our map listener and close the dialog
	 */
	private void cancelDialog() {
		// back out any in progress changes
		map.setFaunaColors(prevColors);
		map.setFaunaNames(prevNames);
		map.setFaunaMap(prevFauna);
		map.repaint();
		
		// release our hold on selection
		map.selectMode(Map.Selection.ANY);
		map.removeMapListener(this);
		
		// and close the window
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
	 * click events on one of the buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == accept && selected) {
			// remember the chosen fauna palette/distribution
			parms.fauna_rules = fauna_palette.getText();
			parms.dFaunaPct = fauna_pct.getValue();
			parms.dFaunaMin = fauna_3.getValue();
			parms.dFaunaMax = fauna_3.getUpperValue();
			
			// check-point these updates
			for(int i = 0; i < faunaMap.length; i++)
				prevFauna[i] = faunaMap[i];
			
			if (chosen_type == AUTOMATIC) {
				prevColors = placer.previewColors();
				prevNames = placer.resourceNames();
				
				// report the changes
				if (parms.debug_level > 0) {
					System.out.println("Fauna Placement (" + ResourceRule.ruleset + 
									   "): Birds/Small/Large = " + classCounts[FAUNA_BIRDS] + 
									   "/" + classCounts[FAUNA_SMALL] +
									   "/" + classCounts[FAUNA_LARGE]);
				}
			}
		} else if (e.getSource() == chooseFauna) {
			FileDialog d = new FileDialog(this, "Mineral Palette", FileDialog.LOAD);
			d.setFile(fauna_palette.getText());
			d.setVisible(true);
			String palette_file = d.getFile();
			if (palette_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					palette_file = dir + palette_file;
				fauna_palette.setText(palette_file);
				placer = null;
			}
			placer = null;
		} else if (e.getSource() == cancel) {
			cancelDialog();
		} else {	// most likely a menu item
			JMenuItem item = (JMenuItem) e.getSource();
			String chosen = item.getText();
			if (item == ruleMode) {
				// go back to rule-based selection
				chooseFauna.setEnabled(true);
				fauna_palette.setEnabled(true);
				fauna_pct.setEnabled(true);
				fauna_3.setEnabled(true);
				chosen_type = AUTOMATIC;
				progressive = false;
			} else {
				// manual choice and placement
				chooseFauna.setEnabled(false);
				fauna_palette.setEnabled(false);
				fauna_pct.setEnabled(false);
				fauna_3.setEnabled(false);
				chosen_type = map.getFaunaType(chosen);
				progressive = true;
			}
			mode.setText(chosen);
		}
	}

	/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
	/** (perfunctory) */ public boolean pointSelected(double x, double y) {return false;}
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
}
