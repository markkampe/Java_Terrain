package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class MeshDialog extends JFrame implements ActionListener, WindowListener {	
		private Parameters parms;
		private Map map;
	
		// control widgets
		private JSlider diameter;
		private JButton accept;
		private JButton cancel;
		private JComboBox<Integer> pointsChooser;
		private JComboBox<Integer> improveChooser;
		private JComboBox<String> profileChooser;
		
		private static final int profileSizes[] = {
				50, 100, 250, 500, 1000
		};
		private static int profilePoints[] = {
				512, 1024, 4096, 8192, 16384
		};
		private static final int DEFAULT_PROFILE = 1;
		
		private static final int DIALOG_OFFSET = 2;
		
		private static final long serialVersionUID = 1L;
		
		public MeshDialog(Map map)  {
			// pick up references
			this.map = map;
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			int border = parms.dialogBorder;
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
			setTitle("New World");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			// create the basic widgets
			Font fontSmall = new Font("Serif", Font.ITALIC, 10);
			Font fontLarge = new Font("Serif", Font.ITALIC, 15);
			pointsChooser = new JComboBox<Integer>();
			JLabel pointsLabel = new JLabel("Points", JLabel.CENTER);
			pointsLabel.setFont(fontLarge);
			for(int i = 256; i < 16536; i *= 2) {
				pointsChooser.addItem(i);
			}
			pointsChooser.setSelectedItem(profilePoints[DEFAULT_PROFILE]);
			
			int max = parms.diameter_max/parms.diameter_scale;
			int dflt = profileSizes[DEFAULT_PROFILE]/parms.diameter_scale;
			diameter = new JSlider(JSlider.HORIZONTAL, 0, max, dflt);
			diameter.setMajorTickSpacing(Parameters.niceTics(0, max, true));
			diameter.setMinorTickSpacing(Parameters.niceTics(0, max, false));
			diameter.setFont(fontSmall);
			diameter.setPaintTicks(true);
			diameter.setPaintLabels(true);
			String label = "Map Height/Width (" + Parameters.unit_xy + " x " + parms.diameter_scale + ")";
			JLabel diameterLabel = new JLabel(label, JLabel.CENTER);
			diameterLabel.setFont(fontLarge);
			
			profileChooser = new JComboBox<String>();
			JLabel profileLabel = new JLabel("Profile", JLabel.CENTER);
			profileLabel.setFont(fontLarge);
			for(int i = 0; i < profileSizes.length; i++) {
				profileChooser.addItem(String.format("%d%s", profileSizes[i], Parameters.unit_xy));
			}
			profileChooser.setSelectedIndex(DEFAULT_PROFILE);
			
			improveChooser = new JComboBox<Integer>();
			JLabel improveLabel = new JLabel("Improvements", JLabel.CENTER);
			improveLabel.setFont(fontLarge);
			for(int i = 0; i <= 4; i++) {
				improveChooser.addItem(i);
			}
			improveChooser.setSelectedItem(parms.improvements);
			
			accept = new JButton("ACCEPT");
			cancel = new JButton("CANCEL");
			
			/*
			 * Pack them into:
			 * 		a vertical Box layout containing comboboxes and buttons
			 * 		combos are a 1x2 grid layout
			 * 			each being a vertical Box w/label and combox
			 * 		buttons a horizontal Box layout
			 */
			JPanel p_panel = new JPanel();
			p_panel.setLayout(new BoxLayout(p_panel, BoxLayout.PAGE_AXIS));
			p_panel.add(pointsLabel);
			p_panel.add(pointsChooser);
			
			JPanel d_panel = new JPanel();
			d_panel.setLayout(new BoxLayout(d_panel, BoxLayout.PAGE_AXIS));
			d_panel.add(diameterLabel);
			d_panel.add(diameter);
			
			JPanel i_panel = new JPanel();
			i_panel.setLayout(new BoxLayout(i_panel, BoxLayout.PAGE_AXIS));
			i_panel.add(improveLabel);
			i_panel.add(improveChooser);
			
			JPanel prof = new JPanel();
			prof.setLayout(new BoxLayout(prof, BoxLayout.PAGE_AXIS));
			prof.add(profileLabel);
			prof.add(profileChooser);
			
			JPanel combos = new JPanel();
			combos.setLayout(new BoxLayout(combos, BoxLayout.LINE_AXIS));
			p_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			d_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			combos.add(prof);
			combos.add(d_panel);
		
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(p_panel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(i_panel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(cancel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(accept);
			buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

			mainPane.add(combos, BorderLayout.NORTH);
			mainPane.add(buttons, BorderLayout.SOUTH);
			
			pack();
			setLocation(parms.dialogDX + DIALOG_OFFSET * parms.dialogDelta, parms.dialogDY + DIALOG_OFFSET * parms.dialogDelta);
			setVisible(true);
			
			// add the action listeners
			accept.addActionListener(this);
			cancel.addActionListener(this);
			profileChooser.addActionListener(this);
		}
		
		/**
		 * Window Close event handler ... implicit CANCEL
		 */
		public void windowClosing(WindowEvent e) {
			this.dispose();
		}

		/**
		 * click events on ACCEPT/CANCEL buttons
		 * 
		 * 		on acceptance, update parameter values, create
		 * 		a new mesh, and install it in the map
		 */
		public void actionPerformed(ActionEvent e) {
			
			if (e.getSource() == profileChooser) {
				int chosen = profileChooser.getSelectedIndex();
				diameter.setValue(profileSizes[chosen]/parms.diameter_scale);
				pointsChooser.setSelectedItem(profilePoints[chosen]);
				return;
			}
			
			if (e.getSource() == accept) {
				parms.points = (Integer) pointsChooser.getSelectedItem();
				parms.dDiameter = diameter.getValue();
				parms.improvements = (Integer) improveChooser.getSelectedItem();
				Mesh m = new Mesh();
				m.create();
				map.setMesh(m);
				
				// give it a small slope, flat maps don't drain
				double[] heightMap = new double[m.vertices.length];
				double minX = -Parameters.x_extent/2;
				double maxX = Parameters.x_extent/2;
				// double minY = -Parameters.x_extent/2;
				double maxY = Parameters.x_extent/2;
				for(int i = 0; i < m.vertices.length; i++) {
					double d = m.vertices[i].distanceLine(minX, maxY, maxX, maxY);
					heightMap[i] =  d * parms.slope_init;
				}
				map.setHeightMap(heightMap);
				
				if (parms.debug_level > 0)
					System.out.println("Reinitialized map w/" + parms.dDiameter * parms.diameter_scale + Parameters.unit_xy + " diameter");
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