package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.ListIterator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to enable the creation a consistent map of a sub-region of the current world.
 */
public class FloraDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener {	
	// flora types and sub-types
	private static final int FLORA_NONE = 0;
	private static final int FLORA_GRASS = 1;
	private static final int FLORA_BRUSH = 2;
	private static final int FLORA_TREE = 3;
	private static final int MAX_TYPES = 4;		// number of flora classes
	private static final int MAX_SUBTYPES = 20;	// max number flora sub-classes
	private boolean haveFloraTypes = false;
	
	private Map map;
	private Parameters parms;
	
	// attribute maps
	private double floraMap[];		// per mesh-point plant types
	private double prevFlora[];		// saved flora Map
	private Color floraColors[];	// type to preview color map	private static final int MAX_SUBTYPES = 20;	// max number of flora subtypes
	private Color prevColors[];		// saved type to preview color map
	private double hydroMap[];		// per mesh-point hydration
	private double prevHydro[];		// saved hydro map
	private double heightMap[];		// per mesh-point altitude
	private double erodeMap[];		// per mesh-point erosion
	private double soilMap[];		// per mesh-point soil map
	
	// widgets
	private JButton accept;			// accept these updates
	private JButton cancel;			// cancel dialog, no updates
	private JTextField flora_palette;	// flora placement rules
	private JButton chooseFlora;	// browse for flora placemen trulesnewColors
	private JSlider flora_pct;		// fraction of area to be covered
	private RangeSlider flora_3;	// grass/brush/tree distribution
	private JSlider goose_temp;		// goose temperature
	private JSlider goose_hydro1;	// goose hydration adder
	private JSlider goose_hydro2;	// goose hydration multiplier
	
	// selected region info
	private boolean selected;		// a region has been selected
	private double x0, y0;			// upper left hand corner
	private double width, height;	// selected area size (in pixels)
	private int placed[];			// count of per-type placements

	// enablers for esoteric WIP attribute tweaks
	private final boolean goose_t = false;
	private final boolean goose_h1 = false;
	private final boolean goose_h2 = false;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public FloraDialog(Map map)  {			PointBid thisBid;
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// get incoming maps, and copy the hydration/flora maps
		heightMap = map.getHeightMap();
		erodeMap = map.getErodeMap();
		soilMap = map.getSoilMap();
		prevFlora = map.getFloraMap();
		prevHydro = map.getHydrationMap();
		floraMap = new double[prevFlora.length];
		hydroMap = new double[prevHydro.length];
		for(int i = 0; i < floraMap.length; i++) {
			floraMap[i] = prevFlora[i];
			hydroMap[i] = prevHydro[i];
		}
		
		// get and copy the type to preview color map
		floraColors = new Color[MAX_SUBTYPES];
		prevColors = map.getFloraColors();
		for(int i = 0; i < floraColors.length; i++)
			floraColors[i] = (prevColors == null) ? Color.BLACK : prevColors[i];
		
		// initial placement counts	private static final int MAX_SUBTYPES = 20;	// max number of flora subtypes
		placed = new int[MAX_SUBTYPES];
		
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
		
		// flora rules selection field
		flora_palette = new JTextField(parms.flora_config);
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
		JLabel fT1 = new JLabel("Tall Grass");
		fT1.setFont(fontLarge);
		fPanel.add(fT1);
		JLabel fT2 = new JLabel("Brush", JLabel.CENTER);
		fT2.setFont(fontLarge);
		fPanel.add(fT2);
		JLabel fT3 = new JLabel("Trees", JLabel.RIGHT);
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
		
		if (goose_t) {
			// temperature goosing slider
			locals.add(new JLabel("    "));
			goose_temp = new JSlider(JSlider.HORIZONTAL, -parms.delta_t_max, parms.delta_t_max, parms.dDeltaT);
			goose_temp.setMajorTickSpacing(5);
			goose_temp.setMinorTickSpacing(1);
			goose_temp.setFont(fontSmall);
			goose_temp.setPaintTicks(true);
			goose_temp.setPaintLabels(true);
			JLabel lTitle = new JLabel("Temperature Adjustment (deg C)");
			lTitle.setFont(fontSmall);
			locals.add(new JLabel("    "));
			locals.add(goose_temp);
			locals.add(lTitle);
			goose_temp.addChangeListener(this);
		}
		
		if (goose_h1) {
			// hydration delta slider
			goose_hydro1 = new JSlider(JSlider.HORIZONTAL, -parms.delta_h_max, parms.delta_h_max, parms.dDeltaH);
			goose_hydro1.setMajorTickSpacing(25);
			goose_hydro1.setMinorTickSpacing(5);
			goose_hydro1.setFont(fontSmall);
			goose_hydro1.setPaintTicks(true);
			goose_hydro1.setPaintLabels(true);
			fTitle = new JLabel("Hydration plus/minus (percentage)");
			fTitle.setFont(fontSmall);
	
			locals.add(new JLabel("    "));
			locals.add(goose_hydro1);
			locals.add(fTitle);
			goose_hydro1.addChangeListener(this);
		}

		if (goose_h2) {
			// hydration scaling slider
			goose_hydro2 = new JSlider(JSlider.HORIZONTAL, 0, 200, parms.dTimesH);
			goose_hydro2.setMajorTickSpacing(25);
			goose_hydro2.setMinorTickSpacing(5);
			goose_hydro2.setFont(fontSmall);
			goose_hydro2.setPaintTicks(true);
			goose_hydro2.setPaintLabels(true);
			fTitle = new JLabel("Hydration Scaling (x percentage)");
			fTitle.setFont(fontSmall);
	
			locals.add(new JLabel("    "));
			locals.add(goose_hydro2);
			locals.add(fTitle);
			goose_hydro2.addChangeListener(this);
		}
		
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
		map.addMapListener(this);	
		map.selectMode(Map.Selection.SQUARE);
		selected = map.checkSelection(Map.Selection.SQUARE);
	}
	
	/**
	 * update the soil hydration for each selected mesh point
	 */
	void updateHydro() {
		double plus = goose_hydro1.getValue();	// percentage
		double scale = goose_hydro2.getValue();	// percentage
		
		MeshPoint[] points = map.mesh.vertices;
		for(int i = 0; i < hydroMap.length; i++) {
			// make sure it is in selected area
			if (points[i].x < x0 || points[i].x >= x0+width ||
				points[i].y < y0 || points[i].y >= y0+height)
				continue;
			
			// make sure it is a land point
			double h = prevHydro[i];
			if (h < 0)
				continue;
			
			// compute the updated hydration for this point
			h *= scale/100.0;
			h += plus/100.0;
			if (h < 0)
				h = 0;
			if (h > 0.99)
				h = 0.99;
			
			hydroMap[i] = h;
		}
	}
	
	/**
	 * record of winning bids
	 */
	private class PointBid {
		int	rule;		// number of the winning rule
		int classNum;	// of the winning bid
		int index;		// index of the point for which this bid was made
		double bid;		// amount of this bid
		PointBid next;	// next bid in the list
		
		PointBid(int index, int ruleNum, double bid, int classNum) {
			this.index = index;
			this.rule = ruleNum;
			this.bid = bid;
			this.classNum = classNum;
			this.next = null;
		}
	}
	
	/**
	 * re-determine the plant coverage of each selected mesh point
	 */
	private int[] placeFlora() {
		// allocate an array for the sub-type colors
		floraColors = new Color[MAX_SUBTYPES];
		floraColors[0] = Color.BLACK;
		
		// make sure we have rules
		if (!haveFloraTypes) {
			ResourceRule.loadRules(flora_palette.getText());
			haveFloraTypes = true;
		}	
		
		// count and initialize the points to be populated
		int point_count = 0;
		MeshPoint[] points = map.mesh.vertices;
		for(int i = 0; i < floraMap.length; i++)
			if (points[i].x >= x0 && points[i].x < x0+width &&
				points[i].y >= y0 && points[i].y < y0+height) {
				floraMap[i] = FLORA_NONE;
				point_count++;
			}

		// figure out the per-class quotas (in MeshPoints)
		int quotas[] = new int[MAX_TYPES];
		point_count = (point_count * flora_pct.getValue())/100;
		quotas[FLORA_GRASS] = (point_count * (100 - flora_3.getUpperValue()))/100;
		quotas[FLORA_TREE] = (point_count * flora_3.getValue())/100;
		quotas[FLORA_BRUSH] = point_count = (quotas[0] + quotas[2]);
		int counts[] = new int[MAX_TYPES];
		
		// get the list of the bidding sub-types
		ResourceRule bidders[] = new ResourceRule[MAX_SUBTYPES];
		int numRules = 0;
		int first = 666;
		int last = -666;
		for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
			ResourceRule r = it.next();
			bidders[numRules++] = r;
			floraColors[numRules] = r.previewColor;
			if (r.order < first)
				first = r.order;
			if (r.order > last)
				last = r.order;
		}
		
		// sub-types bid for mesh points in specified order
		for(int order = first; order <= last; order++) {
			// collect bids for every point in the range
			PointBid thisBid;
			PointBid winners = null;
			for(int i = 0; i < floraMap.length; i++) {
				// make sure it is in selected area
				if (points[i].x < x0 || points[i].x >= x0+width ||
					points[i].y < y0 || points[i].y >= y0+height)
					continue;
				
				// make sure it is not yet occupied
				if (floraMap[i] != FLORA_NONE)
					continue;
				
				// make sure it is above water
				if (hydroMap[i] < 0)
					continue;
				
				// gather bidding attributes for this point
				int alt = (int) parms.altitude(heightMap[i] - erodeMap[i]);
				double lapse = alt * parms.lapse_rate;
				double soil = soilMap[i];
				
				// figure out the (potentially goosed) temperature
				double Twinter = parms.meanWinter();
				double Tsummer = parms.meanSummer();
				if (goose_t) {
					Twinter += goose_temp.getValue();
					Tsummer += goose_temp.getValue();
				}
				
				// figure out the (potentially goosed) hydration
				double plus = goose_h1 ? goose_hydro1.getValue() : 0.0;
				double scale = goose_h2 ? goose_hydro2.getValue() : 100.0;
				double hydro = ((hydroMap[i] * scale)/100) + plus;
				
				// collect all bids (within this order) for this point
				double high_bid = -666.0;
				int winner = -1;
				for(int r = 0; r < numRules; r++) {
					if (bidders[r].order != order)
						continue;	// not eligible to bid this round
					double bid = bidders[r].bid(alt, hydro, Twinter - lapse, Tsummer - lapse, soil);
					if (bid <= 0)
						continue;	// doesn't want this point
					if (bid > high_bid) {
						high_bid = bid;
						winner = r;
					}
				}
				
				// add the winner to our list of winning bids
				if (winner >= 0) {
					int floraClass = FLORA_NONE;
					if (bidders[winner].className.equals("Tree")) floraClass = FLORA_TREE;
					if (bidders[winner].className.equals("Brush")) floraClass = FLORA_BRUSH;
					if (bidders[winner].className.equals("Grass")) floraClass = FLORA_GRASS;
					thisBid = new PointBid(i, winner+1, high_bid, floraClass);
					
					if (winners == null)
						winners = thisBid;		// first bid
					else if (winners.bid < thisBid.bid) {
						thisBid.next = winners;	// highest bid
						winners = thisBid;
					} else {					// insert it after the last bigger-than-this
						PointBid prev = winners;
						while(prev.next != null && prev.next.bid > thisBid.bid)
							prev = prev.next;
						thisBid.next = prev.next;
						prev.next = thisBid;
					}
				}
			}
			
			// award tiles in bid-order
			thisBid = winners;
			while(thisBid != null) {
				// point must not yet be awarded, bidder must be under quota
				if (floraMap[thisBid.index] == 0 &&
					counts[thisBid.classNum] < quotas[thisBid.classNum]) {
					floraMap[thisBid.index] = thisBid.rule;
					counts[thisBid.classNum] += 1;
				}
				thisBid = thisBid.next;
			}
		}
	
		// instantiate (and display) the updated flora map
		map.setFloraColors(floraColors);
		map.setFloraMap(floraMap);
		map.repaint();
		
		return placed;
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
		selected = complete;
		x0 = mx0;
		y0 = my0;
		width = dx;
		height = dy;
		if (complete)
			placeFlora();
		return true;
	}

	/**
	 * Slider changes
	 */
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
		
		if ((source == flora_pct || source == flora_3) && selected)
			placeFlora();
		else if ((source == goose_hydro1 || source == goose_hydro2) && selected) {
			updateHydro();
			placeFlora();
		} else if ((source == goose_temp) && selected)
			placeFlora();
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
			parms.flora_config = flora_palette.getText();
			
			// make these updates official
			for(int i = 0; i < floraMap.length; i++)
				prevFlora[i] = floraMap[i];
			if (prevColors == null)
				prevColors = new Color[MAX_SUBTYPES];
			for(int i = 0; i < floraColors.length; i++)
				prevColors[i] = floraColors[i];
			
			// report the changes
			if (parms.debug_level > 0) {
				System.out.println("Flora Placement (" + ResourceRule.ruleset + "):");
				int number = 1;
				for( ListIterator<ResourceRule> it = ResourceRule.iterator(); it.hasNext();) {
					ResourceRule r = it.next();
					System.out.println("    " + r.className + "." + r.ruleName + ":\t" + placed[number]);
					number++;
				}
			}
			
			// re-zero the placement counts
			for(int i = 0; i < placed.length; i++)
				placed[i] = 0;
			
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
			}
			haveFloraTypes = false;
		} else if (e.getSource() == cancel) {
			map.setFloraColors(prevColors);
			map.setFloraMap(prevFlora);
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
