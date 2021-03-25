package worldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to enable the creation a consistent map of a sub-region of the current world.
 */
public class MineralDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener {	
	
	private Map map;
	private Parameters parms;
	private Placement placer;		// placement engine
	
	// placement information
	private double soilMap[];		// per mesh-point soil/mineral types
	private double prevSoil[];		// saved soil Map
	private Color prevColors[];		// saved type to preview color map
	private int classCounts[];		// returned placements per class
	
	// widgets
	private JButton accept;			// accept these updates
	private JButton cancel;			// cancel dialog, no updates
	private JTextField rock_palette;	// mineral placement rules
	private JButton chooseRocks;	// browse for flora placement trulesnewColors
	private JSlider mineral_pct;	// fraction of area to be covered
	private RangeSlider minerals_3;	// relative mineral distribution

	// selected region info
	private boolean selected;		// a region has been selected
	private double x0, y0;			// upper left hand corner
	private double width, height;	// selected area size (in pixels)
	private boolean changes_made;	// we have displayed updates

	private static final int MIN_NONE = 0;		// merely soil
	private static final int MIN_STONE = 1;		// sand-stone, granite, etc
	private static final int MIN_METAL = 2;		// copper, iron, etc
	private static final int MIN_PRECIOUS = 3;	// gold, silver, etc
	private static final int MAX_TYPES = 4;
	private static final String[] mineralClasses = {"None", "Stone", "Metal", "Precious" };

	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public MineralDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// get incoming Flora Map and its preview colors
		prevSoil = map.getSoilMap();
		prevColors = map.getRockColors();
		soilMap = new double[prevSoil.length];
		for(int i = 0; i < prevSoil.length; i++)
			soilMap[i] = prevSoil[i];
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Mineral Placement");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create a panel for flora selection widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
		
		// flora rules selection field
		rock_palette = new JTextField(parms.mineral_rules);
		JLabel fTitle = new JLabel("Mineral Palette", JLabel.CENTER);
		chooseRocks = new JButton("Browse");
		fTitle.setFont(fontLarge);
		JPanel fPanel = new JPanel(new GridLayout(2, 1));
		fPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		fPanel.add(fTitle);
		JPanel f1_panel = new JPanel();
		f1_panel.setLayout(new BoxLayout(f1_panel, BoxLayout.LINE_AXIS));
		f1_panel.add(rock_palette);
		f1_panel.add(Box.createRigidArea(new Dimension(40, 0)));
		f1_panel.add(chooseRocks);
		fPanel.add(f1_panel);
		locals.add(fPanel);
		chooseRocks.addActionListener(this);
	
		// mineral coverage slider
		mineral_pct = new JSlider(JSlider.HORIZONTAL, 0, 100, parms.dRockPct);
		mineral_pct.setMajorTickSpacing(10);
		mineral_pct.setMinorTickSpacing(5);
		mineral_pct.setFont(fontSmall);
		mineral_pct.setPaintTicks(true);
		mineral_pct.setPaintLabels(true);
		fTitle = new JLabel("Mineral Deposits (percentage)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(mineral_pct);
		locals.add(fTitle);
		mineral_pct.addChangeListener(this);

		// flora type slider
		fPanel = new JPanel(new GridLayout(1, 3));
		JLabel fT1 = new JLabel("Stone");
		fT1.setFont(fontLarge);
		fPanel.add(fT1);
		JLabel fT2 = new JLabel("Metals", JLabel.CENTER);
		fT2.setFont(fontLarge);
		fPanel.add(fT2);
		JLabel fT3 = new JLabel("Precious", JLabel.RIGHT);
		fT3.setFont(fontLarge);
		fPanel.add(fT3);
		minerals_3 = new RangeSlider(0, 100);
		minerals_3.setValue(parms.dRockMin);
		minerals_3.setUpperValue(parms.dRockMax);
		minerals_3.setMajorTickSpacing(10);
		minerals_3.setMinorTickSpacing(5);
		minerals_3.setFont(fontSmall);
		minerals_3.setPaintTicks(true);
		minerals_3.setPaintLabels(true);

		fTitle = new JLabel("Relative Abundance (percentile)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(fPanel);
		locals.add(minerals_3);
		locals.add(fTitle);
		minerals_3.addChangeListener(this);
		
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
	}
	
	/**
	 * (re-)determine the mineral types of each selected mesh point
	 */
	private void placeMinerals() {
		if (placer == null)
			placer = new Placement(rock_palette.getText(), map, soilMap);
		
		// count and initialize the points to be populated
		int point_count = 0;
		MeshPoint[] points = map.mesh.vertices;
		for(int i = 0; i < soilMap.length; i++)
			if (points[i].x >= x0 && points[i].x < x0+width &&
				points[i].y >= y0 && points[i].y < y0+height) {
				soilMap[i] = MIN_NONE;
				point_count++;
			}

		// figure out the per-class quotas (in MeshPoints)
		int quotas[] = new int[MAX_TYPES];
		point_count = (point_count * mineral_pct.getValue())/100;
		quotas[MIN_PRECIOUS] = (point_count * (100 - minerals_3.getUpperValue()))/100;
		quotas[MIN_STONE] = (point_count * minerals_3.getValue())/100;
		quotas[MIN_METAL] = point_count - (quotas[MIN_PRECIOUS] + quotas[MIN_STONE]);
		
		// assign flora types for each MeshPoint
		classCounts = placer.update(x0, y0, height, width, quotas, mineralClasses);
	
		// instantiate (and display) the updated flora map
		map.setRockColors(placer.previewColors());
		map.setSoilMap(soilMap);
		map.repaint();
		changes_made = true;
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
		if (changes_made) {
			// undo any uncommitted placements
			for(int i = 0; i < soilMap.length; i++)
				soilMap[i] = prevSoil[i];
			changes_made = false;
		}
		selected = complete;
		x0 = mx0;
		y0 = my0;
		width = dx;
		height = dy;
		if (complete)
			placeMinerals();
		return true;
	}

	/**
	 * Slider changes
	 */
	public void stateChanged(ChangeEvent e) {
		if (selected)
			placeMinerals();
	}
	
	/**
	 * unregister our map listener and close the dialog
	 */
	private void cancelDialog() {
		map.selectMode(Map.Selection.ANY);
		map.removeMapListener(this);
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
			// remember the chosen flora palette
			parms.flora_rules = rock_palette.getText();
			
			// check-point these updates
			for(int i = 0; i < soilMap.length; i++)
				prevSoil[i] = soilMap[i];
			prevColors = placer.previewColors();
			
			// report the changes
			if (parms.debug_level > 0) {
				System.out.println("Mineral Placement (" + ResourceRule.ruleset + 
								   "): Stone/Metal/Precious = " + classCounts[MIN_STONE] + 
								   "/" + classCounts[MIN_METAL] +
								   "/" + classCounts[MIN_PRECIOUS]);
			}
		} else if (e.getSource() == chooseRocks) {
			FileDialog d = new FileDialog(this, "Mineral Palette", FileDialog.LOAD);
			d.setFile(rock_palette.getText());
			d.setVisible(true);
			String palette_file = d.getFile();
			if (palette_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					palette_file = dir + palette_file;
				rock_palette.setText(palette_file);
				placer = null;
			}
			placer = null;
		} else if (e.getSource() == cancel) {
			map.setRockColors(prevColors);
			map.setSoilMap(prevSoil);
			map.repaint();
			cancelDialog();
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
