package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Box;
import javax.swing.event.*;

public class WorldDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {	
		private Parameters parms;
		private Map map;
	
		// control widgets
		private JButton accept;
		private JButton cancel;
		private JSlider diameter;
		private JSlider altitude;
		
		// limits on world sizes
		private static final int WORLD_SCALE = 100;	// slider labeling unit
		private static final int WORLD_MIN = 0;		// min world diameter (km x100)
		private static final int WORLD_MAX = 50;	// max world diameter (km x100)
		private static final int WORLD_GRAIN = 500;	// multiple of 500 km
		private static final int ALT_SCALE = 1000;	// slider labeling unit
		private static final int ALT_MIN = 0;		// min altitude (m x 1000)
		private static final int ALT_MAX = 10;		// max altitude (m x 1000)
		
		private static final int BORDER_WIDTH = 5;
		
		private static final long serialVersionUID = 1L;
		
		public WorldDialog(Map map)  {
			// pick up references
			this.parms = Parameters.getInstance();
			this.map = map;
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("World Size");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			// create the basic widgets
			Font fontSmall = new Font("Serif", Font.ITALIC, 10);
			Font fontLarge = new Font("Serif", Font.ITALIC, 15);
			accept = new JButton("ACCEPT");
			cancel = new JButton("CANCEL");
			
			diameter = new JSlider(JSlider.HORIZONTAL, WORLD_MIN, WORLD_MAX, parms.xy_range/WORLD_SCALE);
			diameter.setMajorTickSpacing(parms.niceTics(WORLD_MIN, WORLD_MAX, true));
			diameter.setMinorTickSpacing(parms.niceTics(WORLD_MIN, WORLD_MAX, false));
			diameter.setFont(fontSmall);
			diameter.setPaintTicks(true);
			diameter.setPaintLabels(true);
			JLabel diameterLabel = new JLabel("Diameter (km x 100)", JLabel.CENTER);
			diameterLabel.setFont(fontLarge);
			
			altitude = new JSlider(JSlider.HORIZONTAL, ALT_MIN, ALT_MAX, parms.z_range/(2*ALT_SCALE));
			altitude.setMajorTickSpacing(parms.niceTics(ALT_MIN, ALT_MAX, true));
			altitude.setMinorTickSpacing(parms.niceTics(ALT_MIN, ALT_MAX, false));
			altitude.setFont(fontSmall);
			altitude.setPaintTicks(true);
			altitude.setPaintLabels(true);
			JLabel altLabel = new JLabel(" max Altitude (m x 1000)", JLabel.CENTER);
			altLabel.setFont(fontLarge);
			
			/*
			 * Pack them into:
			 * 		a vertical Box layout containing sliders and buttons
			 * 		sliders are a 1x2 grid layout
			 * 			each being a vertical Box w/label and slider
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
			
			JPanel sliders = new JPanel();
			sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
			d_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			a_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			sliders.add(d_panel);
			sliders.add(a_panel);
			
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(cancel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(accept);
			buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

			mainPane.add(sliders, BorderLayout.NORTH);
			mainPane.add(buttons, BorderLayout.SOUTH);
			
			pack();
			setLocation(parms.dialogDX, parms.dialogDY);
			setVisible(true);
			
			// add the action listeners
			diameter.addChangeListener(this);
			altitude.addChangeListener(this);
			accept.addActionListener(this);
			cancel.addActionListener(this);
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
					int v = altitude.getValue() * ALT_SCALE;
					parms.z_range = 2*v;		// from -v to +v
				}
			}
			// discard the window
			this.dispose();
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
