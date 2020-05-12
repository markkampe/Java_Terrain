package worldBuilder;

import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.*;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Dialog to collect information for a Raw (JSON) export
 */
public class FoundationExport extends ExportBase implements ActionListener, ChangeListener {
	private FoundExporter exporter = null;
	
	// flora distribution control widgets
	private JSlider flora_pct;		// percent plant cover
	private RangeSlider flora_3;	// types of plant cover
	private JSlider goose_temp;		// temperature up/down
	private JSlider goose_hydro1;	// hydration up/down
	private JSlider goose_hydro2;	// hydration multiplier
	
	// state variables to advice us of need to recompute
	boolean floraChanged = false;	// flora distribution has changed
	boolean hydroChanged = false;	// hydration has changed
	boolean tempChanged = false;	// temperatures have changed
	private boolean exported = false;

	private static final long serialVersionUID = 1L;

	/**
	 * Register our own action listeners (BaseExport dialog is fine)
	 * 
	 * @param map ... Map to be exported
	 */
	public FoundationExport(Map map) {
		super("Foundation", map, 1, Map.Selection.SQUARE);
		
		// 1024x1024 takes forever, so we do 256x256 and interpolate
		x_points = 256;
		y_points = 256;
		
		// add our controls to those in the base class
		create_GUI();
		setVisible(true);
		
		if (parms.debug_level >= EXPORT_DEBUG)
			System.out.println("new Foundation Export");
	}
	
	/**
	 * create the control widgets and register listeners for this export
	 */
	private void create_GUI() {
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);

		// create a local panel for the export-type controls
		JPanel locals = new JPanel();
		locals.setLayout(new BoxLayout(locals, BoxLayout.PAGE_AXIS));
		locals.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));		// start out with from-the-model hydration
		locals.add(new JLabel("     "));
		
		// create a plant percentage slider
		flora_pct = new JSlider(JSlider.HORIZONTAL, 0, 100, parms.dFloraPct);
		flora_pct.setMajorTickSpacing(10);
		flora_pct.setMinorTickSpacing(5);
		flora_pct.setFont(fontSmall);
		flora_pct.setPaintTicks(true);
		flora_pct.setPaintLabels(true);
		JLabel fTitle = new JLabel("Plant Cover (percentage)");
		fTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(flora_pct);
		locals.add(fTitle);
		flora_pct.addChangeListener(this);
		
		// create a plant break-down slider
		JPanel fPanel = new JPanel(new GridLayout(1, 3));
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
		JLabel l = new JLabel("Flora Distribution (percentile)");
		l.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(fPanel);
		locals.add(flora_3);
		locals.add(l);
		flora_3.addChangeListener(this);
		
		// create temperature adjustment slider
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
		
		// create hydration adjustment slider
		goose_hydro1 = new JSlider(JSlider.HORIZONTAL, -parms.delta_h_max, parms.delta_h_max, parms.dDeltaH);
		goose_hydro1.setMajorTickSpacing(10);
		goose_hydro1.setMinorTickSpacing(1);
		goose_hydro1.setFont(fontSmall);
		goose_hydro1.setPaintTicks(true);
		goose_hydro1.setPaintLabels(true);
		lTitle = new JLabel("Hydration plus/minus (percentage)");
		lTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(goose_hydro1);
		locals.add(lTitle);
		goose_hydro1.addChangeListener(this);

		// create hydration scaling slider
		goose_hydro2 = new JSlider(JSlider.HORIZONTAL, 0, 200, parms.dTimesH);
		goose_hydro2.setMajorTickSpacing(25);
		goose_hydro2.setMinorTickSpacing(5);
		goose_hydro2.setFont(fontSmall);
		goose_hydro2.setPaintTicks(true);
		goose_hydro2.setPaintLabels(true);
		lTitle = new JLabel("Hydration Scaling (x percentage)");
		lTitle.setFont(fontSmall);
		locals.add(new JLabel("    "));
		locals.add(goose_hydro2);
		locals.add(lTitle);
		goose_hydro2.addChangeListener(this);

		// add the local panel to the main panel
		controls.add(locals);
		pack();

		// we handle window and button events
		previewT.addActionListener(this);
		previewF.addActionListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addWindowListener(this);
	}

	/**
	 * process ACCEPT/CANCEL button events
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			windowClosing((WindowEvent) null);
			return;
		}
		
		// make sure we have an exporter ready
		if (exporter == null || newSelection) {
			exporter = new FoundExporter(x_points, y_points);
			exported = false;
			newSelection = false;
		}
		if (!exported) {
			export(exporter);
			exported = true;
		}
		
		if (e.getSource() == accept && selected) {
			// create files in a chosen directory
			JFileChooser d = new JFileChooser(parms.export_dir);
			d.setDialogTitle("Export Directory");
			d.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			d.setAcceptAllFileFilterUsed(false);
			if (d.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				String dir = d.getSelectedFile().getPath();
				parms.export_dir = dir;
				parms.map_name = sel_name.getText();
				exporter.writeFile(dir);
				
				// discard the window
				windowClosing((WindowEvent) null);
			}
		} else if (e.getSource() == previewT && selected) {
			exporter.preview(Exporter.WhichMap.HEIGHTMAP, null);
		} else if (e.getSource() == previewF && selected) {
			exporter.preview(Exporter.WhichMap.FLORAMAP, null);
		}
	}

	/**
	 * record slider changes and note a re-export will be required
	 */
	public void stateChanged(ChangeEvent e) {
		Object source = e.getSource();
	
		if (source == flora_pct || source == flora_3)
			floraChanged = true;
		else if (source == goose_hydro1 || source == goose_hydro2) {
			hydroChanged = true;
			floraChanged = true;
		} else if (source == goose_temp) {
			tempChanged = true;
			floraChanged = true;
		}
	}
}
