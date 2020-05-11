package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to gather information about the size and locaion of this map on the world.
 */
public class WorldDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {	
		private Parameters parms;
	
		// control widgets
		private JButton accept;
		private JButton cancel;
		private JSlider diameter;
		private JSlider altitude;
		private JSlider topo_minor;
		private JSlider topo_major;
		private JTextField latitude;
		private JTextField longitude;
		private JTextArea description;
		private JTextField region_name;
		private JTextField author_name;
		
		private static final int minor_choices[] = {1, 5, 10, 50, 100, 500, 1000};
		private static final int major_choices[] = {5, 10, 20};
		
		private static final long serialVersionUID = 1L;
		
		/**
		 * instantiate the dialog widgets and register the listeners
		 * 
		 * @param readOnly ... sub-region w/fixed world parameters
		 *			disabling these updates makes it more difficult
		 *			to make changes to a sub-region map that are 
		 *			inconsistent with the world-map from which it came.
		 */
		public WorldDialog(boolean readOnly)  {
			// pick up references
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(parms.border, parms.border, parms.border, parms.border, Color.LIGHT_GRAY));
			setTitle("Map Scale/Location");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			// create the basic widgets
			Font fontSmall = new Font("Serif", Font.ITALIC, 10);
			Font fontLarge = new Font("Serif", Font.ITALIC, 15);
			accept = new JButton("ACCEPT");
			cancel = new JButton("CANCEL");
			
			latitude = new JTextField(Double.toString(parms.latitude));
			JLabel latLabel = new JLabel("latitude (center of map, " + Parameters.unit_d + "North)");
			latLabel.setFont(fontLarge);
			longitude = new JTextField(Double.toString(parms.longitude));
			JLabel lonLabel = new JLabel("longitude (center of map, " + Parameters.unit_d + "East)");
			lonLabel.setFont(fontLarge);
			latitude.setEditable(!readOnly);
			longitude.setEditable(!readOnly);
			
			region_name = new JTextField(parms.region_name); 
			JLabel nameLabel = new JLabel("Region Name");
			nameLabel.setFont(fontLarge);
			author_name = new JTextField(parms.author_name);
			JLabel authLabel = new JLabel("Author's Name");
			authLabel.setFont(fontLarge);
		
			description = new JTextArea(parms.getDescription());
			description.setRows(parms.descr_height);
			JLabel dscLabel = new JLabel("Description");
			dscLabel.setFont(fontLarge);
			JPanel dsc_panel = new JPanel();
			dsc_panel.setLayout(new BoxLayout(dsc_panel, BoxLayout.LINE_AXIS));
			dsc_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			dsc_panel.add(description);
			
			int max = parms.diameter_max/parms.diam_scale;
			int dflt = parms.xy_range/parms.diam_scale;
			diameter = new JSlider(JSlider.HORIZONTAL, 0, max, dflt);
			diameter.setMajorTickSpacing(Parameters.niceTics(0, max, true));
			diameter.setMinorTickSpacing(Parameters.niceTics(0, max, false));
			diameter.setFont(fontSmall);
			diameter.setPaintTicks(true);
			diameter.setPaintLabels(true);
			String label = "Height/Width (" + Parameters.unit_xy + " x " + parms.diam_scale + ")";
			JLabel diameterLabel = new JLabel(label, JLabel.CENTER);
			diameterLabel.setFont(fontLarge);
			diameter.setEnabled(!readOnly);
			
			max = parms.alt_max / parms.alt_scale;
			dflt = parms.z_range / (2 * parms.alt_scale);
			altitude = new JSlider(JSlider.HORIZONTAL, 0, max, dflt);
			altitude.setMajorTickSpacing(Parameters.niceTics(0, max, true));
			altitude.setMinorTickSpacing(Parameters.niceTics(0, max, false));
			altitude.setFont(fontSmall);
			altitude.setPaintTicks(true);
			altitude.setPaintLabels(true);
			label = "max Altitude (" + Parameters.unit_z + " x " + parms.alt_scale + ")";
			JLabel altLabel = new JLabel(label, JLabel.CENTER);
			altLabel.setFont(fontLarge);
			altitude.setEnabled(true);
			
			for(dflt = 0; dflt < minor_choices.length; dflt++)
				if (minor_choices[dflt] >= parms.topo_minor)
					break;
			topo_minor = new JSlider(JSlider.HORIZONTAL, 0, minor_choices.length-1, dflt);
			topo_minor.setFont(fontSmall);;
			label = Parameters.unit_z + " per minor line";
			JLabel minorLabel = new JLabel(label, JLabel.CENTER);
			minorLabel.setFont(fontLarge);
			Hashtable<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
			for(int i = 0; i < minor_choices.length; i++)
				labels.put(i, new JLabel(String.format("%d", minor_choices[i])));
			topo_minor.setLabelTable(labels);
			topo_minor.setPaintLabels(true);
			
			for(dflt = 0; dflt < major_choices.length; dflt++)
				if (major_choices[dflt] >= parms.topo_major)
					break;
			topo_major = new JSlider(JSlider.HORIZONTAL, 0, major_choices.length-1, dflt);
			topo_major.setFont(fontSmall);
			label = "minor lines per major line";
			JLabel majorLabel = new JLabel(label, JLabel.CENTER);
			majorLabel.setFont(fontLarge);
			labels = new Hashtable<Integer, JLabel>();
			for(int i = 0; i < major_choices.length; i++)
				labels.put(i, new JLabel(String.format("%d", major_choices[i])));
			topo_major.setLabelTable(labels);
			topo_major.setPaintLabels(true);
			
			/*
			 * Pack them into:
			 * 		vertical Box layout w/ sliders, input fields, and buttons
			 * 		sliders are a 2x2 grid layout
			 * 			each being a vertical Box w/label and slider
			 * 		lat/lon fields are a 1x2 grid layout
			 * 			each being a vertical Box w/label and text field
			 * 		descr is a single text field w/label
			 * 		buttons a horizontal Box layout
			 */
			JPanel d_panel = new JPanel();
			d_panel.setLayout(new BoxLayout(d_panel, BoxLayout.PAGE_AXIS));
			d_panel.add(diameterLabel);
			d_panel.add(diameter);
			
			JPanel a_panel = new JPanel();
			a_panel.setLayout(new BoxLayout(a_panel, BoxLayout.PAGE_AXIS));
			a_panel.add(altLabel);
			a_panel.add(altitude);
			
			JPanel min_panel = new JPanel();
			min_panel.setLayout(new BoxLayout(min_panel, BoxLayout.PAGE_AXIS));
			min_panel.add(minorLabel);
			min_panel.add(topo_minor);
			
			JPanel maj_panel = new JPanel();
			maj_panel.setLayout(new BoxLayout(maj_panel, BoxLayout.PAGE_AXIS));
			maj_panel.add(majorLabel);
			maj_panel.add(topo_major);
				
			JPanel lt_panel = new JPanel();
			lt_panel.setLayout(new BoxLayout(lt_panel, BoxLayout.PAGE_AXIS));
			lt_panel.add(latLabel);
			lt_panel.add(latitude);
			
			JPanel ln_panel = new JPanel();
			ln_panel.setLayout(new BoxLayout(ln_panel, BoxLayout.PAGE_AXIS));
			ln_panel.add(lonLabel);
			ln_panel.add(longitude);
			
			JPanel n_panel = new JPanel();
			n_panel.setLayout(new BoxLayout(n_panel, BoxLayout.PAGE_AXIS));
			n_panel.add(nameLabel);
			n_panel.add(region_name);
			
			JPanel w_panel = new JPanel();
			w_panel.setLayout(new BoxLayout(w_panel, BoxLayout.PAGE_AXIS));
			w_panel.add(authLabel);
			w_panel.add(author_name);
			
			JPanel sliders = new JPanel();
			sliders.setLayout(new GridLayout(2,2));
			d_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			a_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			min_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			maj_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			sliders.add(d_panel);
			sliders.add(a_panel);
			sliders.add(min_panel);
			sliders.add(maj_panel);
			
			JPanel inputs = new JPanel();
			inputs.setLayout(new BoxLayout(inputs, BoxLayout.LINE_AXIS));
			lt_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			ln_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			inputs.add(lt_panel);
			inputs.add(ln_panel);
			
			JPanel names = new JPanel();
			names.setLayout(new BoxLayout(names, BoxLayout.LINE_AXIS));
			n_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			w_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			names.add(n_panel);
			names.add(w_panel);
			
			JPanel middle = new JPanel();
			middle.setLayout(new BoxLayout(middle, BoxLayout.PAGE_AXIS));
			middle.add(inputs);
			middle.add(Box.createRigidArea(new Dimension(0,20)));
			middle.add(names);
			middle.add(Box.createRigidArea(new Dimension(0,20)));
			middle.add(dscLabel);
			middle.add(dsc_panel);
			
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(cancel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(accept);
			buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

			mainPane.add(sliders, BorderLayout.NORTH);
			mainPane.add(middle, BorderLayout.CENTER);
			mainPane.add(buttons, BorderLayout.SOUTH);
			
			pack();
			setVisible(true);
			
			// add the action listeners
			diameter.addChangeListener(this);
			altitude.addChangeListener(this);
			topo_minor.addChangeListener(this);
			topo_major.addChangeListener(this);
			accept.addActionListener(this);
			cancel.addActionListener(this);
			latitude.addActionListener(this);
			longitude.addActionListener(this);
		}
		
		/**
		 * Window Close event handler ... implicit CANCEL
		 */
		public void windowClosing(WindowEvent e) {
			this.dispose();
		}

		/**
		 * click events on ACCEPT/CANCEL buttons
		 */
		public void actionPerformed(ActionEvent e) {
			// on acceptance, copy values into parameters
			if (e.getSource() == accept) {
				if (diameter.getValue() > 0) {
					int v = diameter.getValue() * parms.diam_scale;
					if (v < parms.diam_grain)	// minimum legal value
						v = parms.diam_grain;
					else					// force it to a round number
						v = ((v + parms.diam_grain-1) /parms.diam_grain) * parms.diam_grain;
					parms.xy_range = v;
				}
				if (altitude.getValue() > 0) {
					int v = altitude.getValue() * parms.alt_scale;
					parms.z_range = 2*v;		// from -v to +v
				}
				parms.latitude = Double.parseDouble(latitude.getText());
				parms.longitude = Double.parseDouble(longitude.getText());
				parms.topo_minor = minor_choices[topo_minor.getValue()];
				parms.topo_major = major_choices[topo_major.getValue()];
				parms.setDescription(description.getText());
				parms.author_name = author_name.getText();
				parms.region_name = region_name.getText();
				parms.checkDefaults();	// make sure defaults consistent w/new world size
				
				if (parms.debug_level > 0)
					parms.worldParms();
				
				this.dispose();
			} else if (e.getSource() == cancel) {
				this.dispose();
			} else if (e.getSource() == latitude) {
				try {
					double d = Double.parseDouble(latitude.getText());
					if (d > 90 || d < -90)
						throw new NumberFormatException("latitude must be between 90 and -90");
				} catch(NumberFormatException x) {
					JOptionPane.showMessageDialog(new JFrame(), x.getMessage(), "INVALID LATITUDE", JOptionPane.ERROR_MESSAGE);
					longitude.setText("0.0");
				};
			} else if (e.getSource() == longitude) {
				try {
					double d = Double.parseDouble(longitude.getText());
					if (d > 180 || d < -180) {
						throw new NumberFormatException("longitude must be between 180.0 and -180.0");
					}
				} catch(NumberFormatException x) {
					JOptionPane.showMessageDialog(new JFrame(), x.getMessage(), "INVALID LONGITUDE", JOptionPane.ERROR_MESSAGE);
					longitude.setText("0.0");
				};
			}
		}

		/** (perfunctory) */ public void stateChanged(ChangeEvent e) {}
		/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	}
