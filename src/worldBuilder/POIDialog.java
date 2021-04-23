package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * a Dialog to collect information about a Point of Interest
 */
public class POIDialog extends JFrame implements WindowListener, MapListener, ActionListener {
		private Map map;
		private Parameters parms;
			
		private static final int BORDER_WIDTH = 5;
		private static final Color NEIGHBOR_COLOR = Color.BLACK;
		
		private static final long serialVersionUID = 1L;
		
		private JLabel infoMap;
		private JLabel infoWorld;
		private JLabel infoAlt;
		private JLabel infoNeighbors;
		private JTextField poi_name;
		private JTextField poi_desc;
		private JButton accept;
		private JButton cancel;
		
		private final double UNKNOWN = 666.0;
		
		double point_x, point_y;
		
		/** instantiate the POI information collection widgets */
		public POIDialog(Map map)  {
			// pick up references
			this.map = map;
			this.parms = Parameters.getInstance();
			
			point_x = UNKNOWN;
			point_y = UNKNOWN;
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Points of Interest");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	
			infoMap = new JLabel();
			infoWorld = new JLabel();
			infoAlt = new JLabel();
			infoNeighbors = new JLabel();
			poi_name = new JTextField();
			poi_desc = new JTextField();
			int fields = 7;
			
			JPanel info = new JPanel(new GridLayout(fields, 2));
			info.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
			
			// information about selected point
			info.add(new JLabel("Map Location:"));
			info.add(infoMap);
			info.add(new JLabel("Lat,Lon:"));
			info.add(infoWorld);
			info.add(new JLabel("Altitude:"));
			info.add(infoAlt);
			info.add(new JLabel("Surrounding:"));
			info.add(infoNeighbors);
			info.add(new JLabel(""));
			info.add(new JLabel(""));
			info.add(new JLabel("Name:"));
			info.add(poi_name);
			info.add(new JLabel("Description:"));
			info.add(poi_desc);
			mainPane.add(info,  BorderLayout.CENTER);
			
			// accept/cancel button
			accept = new JButton("ACCEPT");
			cancel = new JButton("CANCEL");
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(cancel);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
			buttons.add(accept);
			buttons.setBorder(BorderFactory.createEmptyBorder(20, 100, 20, 10));
			mainPane.add(buttons, BorderLayout.SOUTH);
		
			pack();
			setVisible(true);
			
			// add the action listeners
			accept.addActionListener(this);
			cancel.addActionListener(this);
			map.addMapListener(this);
			map.selectMode(Map.Selection.POINT);
			map.checkSelection(Map.Selection.POINT);
		}
		
		/**
		 * called whenever a point is selected on the map
		 * 
		 * @param map_x	(map) x coordinate of click
		 * @param map_y	(map) y coordinate of click
		 */
		public boolean pointSelected(double map_x, double map_y) {
			// cancel all previous highlights
			map.highlight(-1,  null);
			
			// display the info for this point
			mapPointInfo(map_x, map_y);
			
			// update the point information pop-up and display the highlights
			pack();
			map.repaint();
			return true;
		}
		
		/**
		 * display information about a random map point
		 * @param map_x
		 * @param map_y
		 */
		void mapPointInfo(double map_x, double map_y) {
			
			infoMap.setText(String.format("<%.7f, %.7f>", map_x, map_y));
			infoWorld.setText(String.format("<%.6f, %.6f>", parms.latitude(map_y), parms.longitude(map_x)));
			
			// figure out where we are relative to MeshPoints
			Vicinity vicinity = new Polygon(map.mesh, map_x, map_y);
			
			// Cartesian interpolated height
			double h = parms.altitude(vicinity.interpolate(map.getHeightMap()));
			String desc = String.format("%.3f%s MSL", h, Parameters.unit_z);
			infoAlt.setText(desc);
			
			// find and highlight our polygon neighbors
			String neighbors = "";
			for(int i = 0; i < Vicinity.NUM_NEIGHBORS; i++)
				if (vicinity.neighbors[i] >= 0) {
					map.highlight(vicinity.neighbors[i], NEIGHBOR_COLOR);
					if (neighbors == "")
						neighbors = String.format("%d", vicinity.neighbors[i]);
					else
						neighbors += String.format(", %d", vicinity.neighbors[i]);
				}
			infoNeighbors.setText(neighbors);
			
			point_x = map_x;
			point_y = map_y;
		}
		
		/**
		 * unregister our listeners and exit
		 */
		private void cancelDialog() {
			map.highlight(-1,  null);			// clear highlights
			map.selectMode(Map.Selection.ANY); 	// clear selections
			map.removeMapListener(this);
			this.dispose();
			WorldBuilder.activeDialog = false;
		}
		/**
		 * Window Close event handler ... do nothing
		 */
		public void windowClosing(WindowEvent e) {
			cancelDialog();
		}
		
		/**
		 * respond to button pushes
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == accept) {
				String name = poi_name.getText();
				if (name.equals("")) {
					System.err.println("Every Point of Interest must have a name");
					return;
				}
				String desc = poi_desc.getText();
				if (desc.equals("")) {
					System.err.println("Every Point of Interest must have a description");
					return;
				}
				
				if (point_x == UNKNOWN || point_y == UNKNOWN) {
					System.err.println("A point on the map must be selected");
					return;
				}
				
				map.setPOI(name, desc, point_x, point_y);
				
				if (parms.debug_level > 0)
					System.out.println("Added " + desc + " " + name + " at " + infoWorld.getText());
				
				// clear name and description for new entrypoints
				poi_name.setText("");
				poi_desc.setText("");
				point_x = UNKNOWN;
				point_y = UNKNOWN;
			} else if (e.getSource() == cancel) {
				cancelDialog();
			}
		}
		
		/** (perfunctory) */ public boolean regionSelected(double x, double y, double w, double h, boolean f) {return false;}
		/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
		/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
		/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
		/** (perfunctory) */ public void mousePressed(MouseEvent arg0) {}
		/** (perfunctory) */ public void mouseReleased(MouseEvent arg0) {}
}
