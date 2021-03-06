package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * a JFrame that displays all the infomration about a selected mesh-point
 */
public class PointDebug extends JFrame implements WindowListener, MapListener, ActionListener {
		private Map map;
		private MapWindow window;
		private Parameters parms;
			
		private static final int BORDER_WIDTH = 5;
		private static final Color SELECTED_COLOR = Color.MAGENTA;
		private static final Color NEIGHBOR_COLOR = Color.BLACK;
		private static final Color GRAYED = Color.LIGHT_GRAY;
		
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
		private JLabel infoWater;
		private JLabel infoFlora;
		private JLabel infoDownhill;
		private JLabel labelDownhill;
		private JLabel infoOutlet;
		private JLabel labelOutlet;
		private JLabel infoNeighbors;
		private JLabel labelNeighbors;
		private JLabel infoName;
		private JLabel labelName;
		private JButton selectMode;
		
		private boolean meshMode;
		
		/** instantiate the point information display, register for selection events */
		public PointDebug(Map map)  {
			// pick up references
			this.map = map;
			this.window = map.window;
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Point Details");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			meshMode = true;
			selectMode = new JButton("Mesh Points");
			
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
			infoWater = new JLabel();
			infoFlora = new JLabel();
			infoDownhill = new JLabel();
			infoOutlet = new JLabel();
			infoNeighbors = new JLabel();
			infoName = new JLabel();
			int fields = 17;
			
			JPanel info = new JPanel(new GridLayout(fields, 2));
			info.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
			info.add(new JLabel("Selection Mode:"));
			info.add(selectMode);
			labelIndex = new JLabel("Index:");
			info.add(labelIndex);
			info.add(infoIndex);
			
			// information available for all points
			info.add(new JLabel("Map Location:"));
			info.add(infoMap);
			info.add(new JLabel("Lat,Lon:"));
			info.add(infoWorld);
			info.add(new JLabel("Altitude:"));
			info.add(infoAlt);
			info.add(new JLabel("Water Level:"));
			info.add(infoWater);
			info.add(new JLabel("rainfall:"));
			info.add(infoRain);
			info.add(new JLabel("Flora type:"));
			info.add(infoFlora);
			info.add(new JLabel("Soil Type:"));
			info.add(infoSoil);			
			info.add(new JLabel("Erosion/(Deposition):"));
			info.add(infoErode);
			
			// information only available for Mesh Points
			labelDownhill = new JLabel("Downhill:");
			info.add(labelDownhill);
			info.add(infoDownhill);
			labelFlux = new JLabel("Water Flux:");
			info.add(labelFlux);
			info.add(infoFlux);
			labelVelocity = new JLabel("Water Velocity");
			info.add(labelVelocity);
			info.add(infoVelocity);
			labelOutlet = new JLabel("Outlet:");
			info.add(labelOutlet);
			info.add(infoOutlet);
			labelSuspended = new JLabel("Suspended:");
			info.add(labelSuspended);
			info.add(infoSuspended);
			labelName = new JLabel("Name:");
			info.add(labelName);
			info.add(infoName);
			
			labelNeighbors = new JLabel("Neighbors (mesh):");
			info.add(labelNeighbors);
			info.add(infoNeighbors);
		
			mainPane.add(info,  BorderLayout.CENTER);
			
			pack();
			setVisible(true);
			
			// add the action listeners
			selectMode.addActionListener(this);
			window.addMapListener(this);
			window.selectMode(MapWindow.Selection.POINT);
			window.checkSelection(MapWindow.Selection.POINT);
		}
		
		/**
		 * called whenever a point is selected on the map
		 * 
		 * @param map_x	(map) x coordinate of click
		 * @param map_y	(map) y coordinate of click
		 */
		public boolean pointSelected(double map_x, double map_y) {
			// cancel all previous highlights
			window.highlight(-1,  null);
			
			// display the info for this point
			if (meshMode) {
				MeshPoint point = map.mesh.choosePoint(map_x, map_y);
				meshPointInfo(point);
				
				// highlight selected point
				window.highlight(point.index, SELECTED_COLOR);
			} else {
				mapPointInfo(map_x, map_y);
			}
			
			
			
			// update the point information pop-up and display the highlights
			pack();
			window.repaint();
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
			infoWorld.setText(String.format("<%.5f, %.5f>", parms.latitude(point.y), parms.longitude(point.x)));
			
			double heightMap[] = map.getHeightMap();
			double v = parms.altitude(heightMap[point.index]);
			String desc = String.format("%.3f%s MSL", v, Parameters.unit_z);
			infoAlt.setText(desc);
			
			double fluxMap[] = map.getFluxMap();
			infoFlux.setText(String.format("%.3f%s", fluxMap[point.index], Parameters.unit_f));
			pack();
			
			double rainMap[] = map.getRainMap();
			infoRain.setText(String.format("%.1f%s", rainMap[point.index], Parameters.unit_r));
			pack();
			
			double erodeMap[] = map.getErodeMap();
			v = parms.height(erodeMap[point.index]);
			if (v < 0)
				desc = String.format("(%.3f%s)", -v, Parameters.unit_z);
			else
				desc = String.format("%.3f%s", v, Parameters.unit_z);
			infoErode.setText(desc);
			
			double speed = map.waterflow.velocityMap[point.index];
			infoVelocity.setText(String.format("%.3f%s", speed, Parameters.unit_v));
			
			double susp = map.waterflow.suspended[point.index];
			infoSuspended.setText(String.format("%f%s", susp, Parameters.unit_f));

			double floraMap[] = map.getFloraMap();
			if (floraMap != null && floraMap[point.index] > 0)
				infoFlora.setText(map.floraNames[(int) floraMap[point.index]]);
			else
				infoFlora.setText("None");
			
			double soilMap[] = map.getSoilMap();
			desc = erodeMap[point.index] < 0 ? map.getSoilType("Alluvial") + "/" : "";
			infoSoil.setText(desc + map.rockNames[(int) soilMap[point.index]]);
			
			double waterLevel[] = map.getWaterLevel();
			v = waterLevel[point.index];
			if (v > WaterFlow.UNKNOWN)
				desc = String.format("%.3f%s MSL", parms.altitude(v), Parameters.unit_z);
			else
				desc = "NONE NEARBY";
			infoWater.setText(desc);
			
			int downHill[] = map.getDrainage().downHill;
			infoDownhill.setText(String.format("%d", downHill[point.index]));
			
			double outlet = map.drainage.outlet[point.index];
			infoOutlet.setText(outlet == Drainage.UNKNOWN ? "NONE" :
								String.format("%.3fMSL", parms.altitude(outlet)));
			
			String[] names = map.getNameMap();
			infoName.setText((names[point.index] == null) ? "NONE" : names[point.index]);
			
			// find and highlight our Mesh neighbors
			String neighbors = "";
			for(int i = 0; i < point.neighbors; i++) {
				window.highlight(point.neighbor[i].index, NEIGHBOR_COLOR);
				if (neighbors == "")
					neighbors = String.format("%d", point.neighbor[i].index);
				else
					neighbors += String.format(", %d", point.neighbor[i].index);
			}
			infoNeighbors.setText(neighbors);
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
			
			// nearest valid water depth
			double v = vicinity.nearestValid(map.getWaterLevel(), WaterFlow.UNKNOWN);
			if (v != WaterFlow.UNKNOWN)
				desc = String.format("%.3f%s MSL", parms.altitude(v), Parameters.unit_z);
			else
				desc = "NONE NEARBY";
			infoWater.setText(desc);
			
			// Cartesian interpolated rainfall
			v = vicinity.interpolate(map.getRainMap());
			infoRain.setText(String.format("%.1f%s", v, Parameters.unit_r));

			// Cartesian interpolated erosion
			v = vicinity.interpolate(map.getErodeMap());
			desc = (v < 0) ? String.format("(%.3f%s)", -v, Parameters.unit_z)
							: String.format("%.3f%s", v, Parameters.unit_z);
			infoErode.setText(desc);
			
						
			// soil from nearest MeshPoint
			double m[] = map.getSoilMap();
			if (m != null) {
				v = vicinity.nearest(m);
				infoSoil.setText(map.rockNames[(int) v]);
			} else
				infoSoil.setText("");
			
			// flora from nearest MeshPoint
			m = map.getFloraMap();
			if (m != null) {
				v = vicinity.nearest(m);
				infoFlora.setText(map.floraNames[(int) v]);
			} else
				infoFlora.setText("");
			
			// find and highlight our polygon neighbors
			
			String neighbors = "";
			for(int i = 0; i < Vicinity.NUM_NEIGHBORS; i++)
				if (vicinity.neighbors[i] >= 0) {
					window.highlight(vicinity.neighbors[i], NEIGHBOR_COLOR);
					if (neighbors == "")
						neighbors = String.format("%d", vicinity.neighbors[i]);
					else
						neighbors += String.format(", %d", vicinity.neighbors[i]);
				}
			infoNeighbors.setText(neighbors);
		}
		
		/**
		 * respond to changes in the selectMode button
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == selectMode) {
				if (meshMode) {
					meshMode = false;
					// gray out the MeshPoint-only fields
					infoIndex.setText("");
					labelIndex.setForeground(GRAYED);
					infoFlux.setText("");
					labelFlux.setForeground(GRAYED);
					infoVelocity.setText("");
					labelVelocity.setForeground(GRAYED);
					infoSuspended.setText("");
					labelSuspended.setForeground(GRAYED);
					infoDownhill.setText("");
					labelDownhill.setForeground(GRAYED);
					infoOutlet.setText("");
					labelOutlet.setForeground(GRAYED);
					infoName.setText("");
					labelName.setForeground(GRAYED);
					labelNeighbors.setText("Neghbors (polygon):");
				} else {
					meshMode = true;
					// re-enable the MeshPoint only fields
					labelIndex.setForeground(Color.BLACK);
					labelFlux.setForeground(Color.BLACK);
					labelVelocity.setForeground(Color.BLACK);
					labelSuspended.setForeground(Color.BLACK);
					labelDownhill.setForeground(Color.BLACK);
					labelOutlet.setForeground(Color.BLACK);
					labelName.setForeground(Color.BLACK);
					labelNeighbors.setText("Neighbors (mesh):");
				}
				selectMode.setText((meshMode ? "Mesh" : "Map") + " Points");
			}
		}
		/**
		 * Window Close event handler ... do nothing
		 */
		public void windowClosing(WindowEvent e) {
			window.highlight(-1,  null);			// clear highlights
			window.selectMode(MapWindow.Selection.ANY); 	// clear selections
			window.removeMapListener(this);
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
