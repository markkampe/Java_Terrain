package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.Box;
import javax.swing.event.*;

public class MeshDialog extends JFrame implements ActionListener, WindowListener {	
		private Parameters parms;
		private Map map;
	
		// control widgets
		private JButton accept;
		private JButton cancel;
		private JComboBox<Integer> pointsChooser;
		private JComboBox<Integer> improveChooser;
				
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
			setTitle("Create Mesh");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			// create the basic widgets
			// Font fontSmall = new Font("Serif", Font.ITALIC, 10);
			Font fontLarge = new Font("Serif", Font.ITALIC, 15);
			pointsChooser = new JComboBox<Integer>();
			JLabel pointsLabel = new JLabel("Points", JLabel.CENTER);
			pointsLabel.setFont(fontLarge);
			for(int i = 256; i < 16536; i *= 2) {
				pointsChooser.addItem(i);
			}
			pointsChooser.setSelectedItem(parms.points);
			
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
			
			JPanel i_panel = new JPanel();
			i_panel.setLayout(new BoxLayout(i_panel, BoxLayout.PAGE_AXIS));
			i_panel.add(improveLabel);
			i_panel.add(improveChooser);
			
			JPanel combos = new JPanel();
			combos.setLayout(new BoxLayout(combos, BoxLayout.LINE_AXIS));
			p_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 15));
			i_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
			combos.add(p_panel);
			combos.add(i_panel);
			
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
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
			if (e.getSource() == accept) {
				parms.points = (Integer) pointsChooser.getSelectedItem();
				parms.improvements = (Integer) improveChooser.getSelectedItem();
				Mesh m = new Mesh();
				m.create();
				map.setMesh(m);
				
				// make it a very short cone, flat maps don't drain
				double[] heightMap = new double[m.vertices.length];
				MeshPoint centre = new MeshPoint(0, 0);
				MeshPoint corner = new MeshPoint(Parameters.x_extent/2, Parameters.y_extent/2);
				double dMax = centre.distance(corner);
				for(int i = 0; i < m.vertices.length; i++) {
					double d = centre.distance(m.vertices[i]);
					heightMap[i] = (dMax - d) * parms.slope_init;
				}
				map.setHeightMap(heightMap);
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