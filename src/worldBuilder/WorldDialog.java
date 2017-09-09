package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.*;

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
		
		private static final int minor_choices[] = {1, 5, 10, 50, 100, 500, 1000};
		private static final int major_choices[] = {5, 10, 20};
		
		private static final int DIALOG_OFFSET = 0;
		
		private static final long serialVersionUID = 1L;
		
		public WorldDialog()  {
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
			JLabel latLabel = new JLabel("latitude (center of map)");
			latLabel.setFont(fontLarge);
			longitude = new JTextField(Double.toString(parms.longitude));
			JLabel lonLabel = new JLabel("longitude (center of map)");
			lonLabel.setFont(fontLarge);
		
			int max = parms.diameter_max/parms.diameter_scale;
			int dflt = parms.xy_range/parms.diameter_scale;
			diameter = new JSlider(JSlider.HORIZONTAL, 0, max, dflt);
			diameter.setMajorTickSpacing(Parameters.niceTics(0, max, true));
			diameter.setMinorTickSpacing(Parameters.niceTics(0, max, false));
			diameter.setFont(fontSmall);
			diameter.setPaintTicks(true);
			diameter.setPaintLabels(true);
			String label = "Height/Width (" + Parameters.unit_xy + " x " + parms.diameter_scale + ")";
			JLabel diameterLabel = new JLabel(label, JLabel.CENTER);
			diameterLabel.setFont(fontLarge);
			
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
			 * 		text fiels are a 1x2 grid layout
			 * 			each being a vertical Box w/label and text field
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
			
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(cancel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(accept);
			buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

			mainPane.add(sliders, BorderLayout.NORTH);
			mainPane.add(inputs, BorderLayout.CENTER);
			mainPane.add(buttons, BorderLayout.SOUTH);
			
			pack();
			setLocation(parms.dialogDX + DIALOG_OFFSET*parms.dialogDelta, parms.dialogDY+ DIALOG_OFFSET*parms.dialogDelta);
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
					int v = diameter.getValue() * parms.diameter_scale;
					if (v < parms.diameter_grain)	// minimum legal value
						v = parms.diameter_grain;
					else					// force it to a round number
						v = ((v + parms.diameter_grain-1) /parms. diameter_grain) * parms.diameter_grain;
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

		// perfunctory methods
		public void stateChanged(ChangeEvent e) {}
		public void windowActivated(WindowEvent arg0) {}
		public void windowClosed(WindowEvent arg0) {}
		public void windowDeactivated(WindowEvent arg0) {}
		public void windowDeiconified(WindowEvent arg0) {}
		public void windowIconified(WindowEvent arg0) {}
		public void windowOpened(WindowEvent arg0) {}
	}
