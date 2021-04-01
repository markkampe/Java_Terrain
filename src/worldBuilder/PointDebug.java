package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * a JFrame that displays all the infomration about a selected mesh-point
 */
public class PointDebug extends JFrame implements WindowListener, MapListener, ActionListener {
		private Map map;
		private Parameters parms;
			
		private static final int BORDER_WIDTH = 5;
		private static final Color SELECTED_COLOR = Color.MAGENTA;
		private static final Color NEIGHBOR_COLOR = Color.BLACK;
		
		private static final long serialVersionUID = 1L;
		
		private JLabel infoIndex;
		private JLabel labelIndex;
		private JLabel infoMap;
		private JLabel infoWorld;
		private JLabel infoAlt;
		private JLabel infoFlux;
		private JLabel labelFlux;
		private JLabel infoVelocity;
		private JLabel labelVelocity;
		private JLabel infoRain;
		private JLabel infoErode;
		private JLabel infoSuspended;
		private JLabel labelSuspended;
		private JLabel infoSoil;
		private JLabel infoHydro;
		private JLabel infoFlora;
		private JLabel infoDownhill;
		private JLabel labelDownhill;
		private JLabel infoOutlet;
		private JLabel labelOutlet;
		private JLabel infoNeighbors;
		private JButton selectMode;
		
		private boolean meshMode = true;
		private String modes[] = { "Map Point", "Mesh Point" };
		
		/** instantiate the point information display, register for selection events */
		public PointDebug(Map map)  {
			// pick up references
			this.map = map;
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Point Details");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			selectMode = new JButton(modes[1]);
			infoIndex = new JLabel("Select a point");
			infoMap = new JLabel();
			infoWorld = new JLabel();
			infoAlt = new JLabel();
			infoFlux = new JLabel();
			infoVelocity = new JLabel();
			infoRain = new JLabel();
			infoErode = new JLabel();
			infoSuspended = new JLabel();
			infoSoil = new JLabel();
			infoHydro = new JLabel();
			infoFlora = new JLabel();
			infoDownhill = new JLabel();
			infoOutlet = new JLabel();
			infoNeighbors = new JLabel();
			int fields = 16;
			
			JPanel info = new JPanel(new GridLayout(fields, 2));
			info.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
			info.add(new JLabel("Selection Mode:"));
			info.add(selectMode);
			labelIndex = new JLabel("Index:");
			info.add(labelIndex);
			info.add(infoIndex);
			info.add(new JLabel("Map Location:"));
			info.add(infoMap);
			info.add(new JLabel("Lat,Lon:"));
			info.add(infoWorld);
			info.add(new JLabel("Altitude:"));
			info.add(infoAlt);
			info.add(new JLabel("rainfall:"));
			info.add(infoRain);
			labelFlux = new JLabel("Water Flux:");
			info.add(labelFlux);
			info.add(infoFlux);
			labelVelocity = new JLabel("Water Velocity");
			info.add(labelVelocity);
			info.add(infoVelocity);
			info.add(new JLabel("Erosion/(Deposition):"));
			info.add(infoErode);
			labelSuspended = new JLabel("Suspended:");
			info.add(labelSuspended);
			info.add(infoSuspended);
			info.add(new JLabel("Flora type:"));
			info.add(infoFlora);
			info.add(new JLabel("Soil Type:"));
			info.add(infoSoil);
			info.add(new JLabel("Depth:"));
			info.add(infoHydro);
			labelDownhill = new JLabel("Downhill:");
			info.add(labelDownhill);
			info.add(infoDownhill);
			labelOutlet = new JLabel("Outlet:");
			info.add(labelOutlet);
			info.add(infoOutlet);
			info.add(new JLabel("Neighbors: "));
			info.add(infoNeighbors);

			mainPane.add(info,  BorderLayout.CENTER);
			
			pack();
			setVisible(true);
			
			// add the action listeners
			selectMode.addActionListener(this);
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
			if (meshMode) {
				MeshPoint point = map.mesh.choosePoint(map_x, map_y);
				meshPointInfo(point);
				
				// highlight selected point
				map.highlight(point.index, SELECTED_COLOR);
			} else {
				mapPointInfo(map_x, map_y);
			}
			
			// find and highlight our neighbors
			Vicinity vicinity = new Polygon(map.mesh, map_x, map_y);
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
			
			// update the point information pop-up and display the highlights
			pack();
			map.repaint();
			return true;
		}
		
		/**
		 * display info for a MeshPoint
		 */
		private void meshPointInfo(MeshPoint point) {
			
			if (point.immutable)
				infoIndex.setText(String.format("%d (immutable)", point.index));
			else
				infoIndex.setText(String.format("%d", point.index));
			infoMap.setText(String.format("<%.7f, %.7f>", point.x, point.y));
			infoWorld.setText(String.format("<%.6f, %.6f>", parms.latitude(point.y), parms.longitude(point.x)));
			
			double heightMap[] = map.getHeightMap();
			infoAlt.setText(String.format("%.1f%s MSL", parms.altitude(heightMap[point.index]), Parameters.unit_z));
			
			double fluxMap[] = map.getFluxMap();
			infoFlux.setText(String.format("%.3f%s", fluxMap[point.index], Parameters.unit_f));
			pack();
			
			double rainMap[] = map.getRainMap();
			infoRain.setText(String.format("%.1f%s", rainMap[point.index], Parameters.unit_r));
			pack();
			
			double erodeMap[] = map.getErodeMap();
			double h = parms.height(erodeMap[point.index]);
			String desc;
			if (h < 0)
				desc = String.format("(%.3f%s)",- h, Parameters.unit_z);
			else
				desc = String.format("%.3f%s", h, Parameters.unit_z);
			infoErode.setText(desc);
			
			double speed = map.hydro.velocityMap[point.index];
			infoVelocity.setText(String.format("%.3f%s", speed, Parameters.unit_v));
			
			double susp = map.hydro.suspended[point.index];
			infoSuspended.setText(String.format("%f%s", susp, Parameters.unit_f));

			double floraMap[] = map.getFloraMap();
			if (floraMap != null && floraMap[point.index] > 0)
				infoFlora.setText(map.floraNames[(int) floraMap[point.index]]);
			else
				infoFlora.setText("None");
			
			double soilMap[] = map.getSoilMap();
			desc = erodeMap[point.index] < 0 ? map.getSoilType("Alluvial") + "/" : "";
			infoSoil.setText(desc + map.rockNames[(int) soilMap[point.index]]);
			
			double depthMap[] = map.getDepthMap();
			h = depthMap[point.index];
			if (h > 0)
				desc = String.format((h >= 10.0) ? "%.1f" : "%.2f", h) +
						Parameters.unit_z + " above outlet";
			else
				desc = String.format((h > -10.0) ? "%.2f" : "%.1f", -h) +
						Parameters.unit_z + " below water";
			infoHydro.setText(desc);
			
			int downHill[] = map.getDownHill();
			infoDownhill.setText(String.format("%d", downHill[point.index]));
			
			double outlet = map.hydro.outlet[point.index];
			infoOutlet.setText(outlet == Hydrology.UNKNOWN ? "NONE" :
								String.format("%.1fMSL", parms.altitude(outlet)));
		}
		
		/**
		 * display information about a random map point
		 * @param map_x
		 * @param map_y
		 */
		void mapPointInfo(double map_x, double map_y) {
			
			infoMap.setText(String.format("<%.7f, %.7f>", map_x, map_y));
			infoWorld.setText(String.format("<%.6f, %.6f>", parms.latitude(map_y), parms.longitude(map_x)));
			
			double h = 0; // = parms.height(erodeMap[point.index]);
			String desc = "TBD";
			
			double heightMap[] = map.getHeightMap();
			//infoAlt.setText(String.format("%.1f%s MSL", parms.altitude(heightMap[point.index]), Parameters.unit_z));
			infoAlt.setText(desc);
			
			double rainMap[] = map.getRainMap();
			//infoRain.setText(String.format("%.1f%s", rainMap[point.index], Parameters.unit_r));
			infoRain.setText(desc);
			
			double erodeMap[] = map.getErodeMap();

			if (h < 0)
				desc = String.format("(%.3f%s)",- h, Parameters.unit_z);
			else
				desc = String.format("%.3f%s", h, Parameters.unit_z);
			infoErode.setText(desc);
			
			double floraMap[] = map.getFloraMap();
			//if (floraMap != null && floraMap[point.index] > 0)
			//	infoFlora.setText(map.floraNames[(int) floraMap[point.index]]);
			//else
			// infoFlora.setText("None");
			infoFlora.setText(desc);
			
			double soilMap[] = map.getSoilMap();
			//desc = erodeMap[point.index] < 0 ? map.getSoilType("Alluvial") + "/" : "";
			//infoSoil.setText(desc + map.rockNames[(int) soilMap[point.index]]);
			infoSoil.setText(desc);
			
			double depthMap[] = map.getDepthMap();
			//h = depthMap[point.index];
			if (h > 0)
				desc = String.format((h >= 10.0) ? "%.1f" : "%.2f", h) +
						Parameters.unit_z + " above outlet";
			else
				desc = String.format((h > -10.0) ? "%.2f" : "%.1f", -h) +
						Parameters.unit_z + " below water";
			infoHydro.setText(desc);
		}
		
		/**
		 * respond to changes in the selectMode button
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == selectMode) {
				if (meshMode) {
					infoIndex.setText("");
					labelIndex.setForeground(Color.GRAY);
					infoFlux.setText("");
					labelFlux.setForeground(Color.GRAY);
					infoVelocity.setText("");
					labelVelocity.setForeground(Color.GRAY);
					infoSuspended.setText("");
					labelSuspended.setForeground(Color.GRAY);
					infoDownhill.setText("");
					labelDownhill.setForeground(Color.GRAY);
					infoOutlet.setText("");
					labelOutlet.setForeground(Color.GRAY);
					selectMode.setText(modes[0]);
					meshMode = false;
				} else {
					labelIndex.setForeground(Color.BLACK);
					labelFlux.setForeground(Color.BLACK);
					labelVelocity.setForeground(Color.BLACK);
					labelSuspended.setForeground(Color.BLACK);
					labelDownhill.setForeground(Color.BLACK);
					labelOutlet.setForeground(Color.BLACK);
					selectMode.setText(modes[1]);
					meshMode = true;
				}
			}
		}
		/**
		 * Window Close event handler ... do nothing
		 */
		public void windowClosing(WindowEvent e) {
			map.highlight(-1,  null);			// clear highlights
			map.selectMode(Map.Selection.ANY); 	// clear selections
			map.removeMapListener(this);
			this.dispose();
			WorldBuilder.activeDialog = false;
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
