package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class WorldDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {	
		private Parameters parms;
	
		// control widgets
		private JButton accept;
		private JButton cancel;
		private JSlider diameter;
		private JSlider altitude;
		private JTextField latitude;
		private JTextField longitude;
		
		// limits on world sizes
		private static final int WORLD_SCALE = 100;	// slider labeling unit
		private static final int WORLD_MIN = 0;		// min world diameter (km x100)
		private static final int WORLD_MAX = 50;	// max world diameter (km x100)
		private static final int WORLD_GRAIN = 500;	// multiple of 500 km
		
		private static final int BORDER_WIDTH = 5;
		
		private static final long serialVersionUID = 1L;
		
		public WorldDialog()  {
			// pick up references
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Map Size/Location");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			// create the basic widgets
			Font fontSmall = new Font("Serif", Font.ITALIC, 10);
			Font fontLarge = new Font("Serif", Font.ITALIC, 15);
			accept = new JButton("ACCEPT");
			cancel = new JButton("CANCEL");
			
			latitude = new JTextField("0.0");
			JLabel latLabel = new JLabel("latitude (center of map)");
			latLabel.setFont(fontLarge);
			longitude = new JTextField("0.0");
			JLabel lonLabel = new JLabel("longitude (center of map");
			lonLabel.setFont(fontLarge);
			
			diameter = new JSlider(JSlider.HORIZONTAL, WORLD_MIN, WORLD_MAX, parms.xy_range/WORLD_SCALE);
			diameter.setMajorTickSpacing(Parameters.niceTics(WORLD_MIN, WORLD_MAX, true));
			diameter.setMinorTickSpacing(Parameters.niceTics(WORLD_MIN, WORLD_MAX, false));
			diameter.setFont(fontSmall);
			diameter.setPaintTicks(true);
			diameter.setPaintLabels(true);
			JLabel diameterLabel = new JLabel("Diameter (km x 100)", JLabel.CENTER);
			diameterLabel.setFont(fontLarge);
			
			altitude = new JSlider(JSlider.HORIZONTAL, Parameters.ALT_MIN, Parameters.ALT_MAX, parms.z_range/(2*Parameters.ALT_SCALE));
			altitude.setMajorTickSpacing(Parameters.niceTics(Parameters.ALT_MIN, Parameters.ALT_MAX, true));
			altitude.setMinorTickSpacing(Parameters.niceTics(Parameters.ALT_MIN, Parameters.ALT_MAX, false));
			altitude.setFont(fontSmall);
			altitude.setPaintTicks(true);
			altitude.setPaintLabels(true);
			JLabel altLabel = new JLabel(" max Altitude (m x 1000)", JLabel.CENTER);
			altLabel.setFont(fontLarge);
			
			/*
			 * Pack them into:
			 * 		vertical Box layout w/ sliders, input fields, and buttons
			 * 		sliders are a 1x2 grid layout
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
			
			JPanel lt_panel = new JPanel();
			lt_panel.setLayout(new BoxLayout(lt_panel, BoxLayout.PAGE_AXIS));
			lt_panel.add(latLabel);
			lt_panel.add(latitude);
			
			JPanel ln_panel = new JPanel();
			ln_panel.setLayout(new BoxLayout(ln_panel, BoxLayout.PAGE_AXIS));
			ln_panel.add(lonLabel);
			ln_panel.add(longitude);
			
			
			JPanel sliders = new JPanel();
			sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
			d_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			a_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			sliders.add(d_panel);
			sliders.add(a_panel);
			
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
			setLocation(parms.dialogDX, parms.dialogDY);
			setVisible(true);
			
			// add the action listeners
			diameter.addChangeListener(this);
			altitude.addChangeListener(this);
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
					int v = diameter.getValue() * WORLD_SCALE;
					if (v < WORLD_GRAIN)	// minimum legal value
						v = WORLD_GRAIN;
					else					// force it to a round number
						v = ((v + WORLD_GRAIN-1) / WORLD_GRAIN) * WORLD_GRAIN;
					parms.xy_range = v;
				}
				if (altitude.getValue() > 0) {
					int v = altitude.getValue() * Parameters.ALT_SCALE;
					parms.z_range = 2*v;		// from -v to +v
				}
				parms.latitude = Double.parseDouble(latitude.getText());
				parms.longitude = Double.parseDouble(longitude.getText());
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
