package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class PointDebug extends JFrame implements WindowListener, MouseListener {
		private Map map;
		private Parameters parms;
			
		private static final int BORDER_WIDTH = 5;
		private static final int DIALOG_OFFSET = 7;
		
		private static final long serialVersionUID = 1L;
		
		private JLabel infoIndex;
		private JLabel infoMap;
		private JLabel infoWorld;
		private JLabel infoAlt;
		private JLabel infoFlux;
		private JLabel infoErode;
		private JLabel infoSoil;
		private JLabel infoHydro;
		private JLabel infoDownhill;
		
		public PointDebug(Map map)  {
			// pick up references
			this.map = map;
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Point Info");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			infoIndex = new JLabel("Select a point");
			infoMap = new JLabel();
			infoWorld = new JLabel();
			infoAlt = new JLabel();
			infoFlux = new JLabel();
			infoErode = new JLabel();
			infoSoil = new JLabel();
			infoHydro = new JLabel();
			infoDownhill = new JLabel();
			
			JPanel info = new JPanel(new GridLayout(9,2));
			info.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
			info.add(new JLabel("Index:"));
			info.add(infoIndex);
			info.add(new JLabel("Map:"));
			info.add(infoMap);
			info.add(new JLabel("Lat,Lon"));
			info.add(infoWorld);
			info.add(new JLabel("Altitude:"));
			info.add(infoAlt);
			info.add(new JLabel("Water Flux:"));
			info.add(infoFlux);
			info.add(new JLabel("Erosion/Deposition:"));
			info.add(infoErode);
			info.add(new JLabel("Soil Type"));
			info.add(infoSoil);
			info.add(new JLabel("Hydration:"));
			info.add(infoHydro);
			info.add(new JLabel("Downhill"));
			info.add(infoDownhill);

			mainPane.add(info,  BorderLayout.CENTER);
			
			pack();
			setLocation(parms.dialogDX + DIALOG_OFFSET * parms.dialogDelta, parms.dialogDY + DIALOG_OFFSET * parms.dialogDelta);
			setVisible(true);
			
			// add the action listeners
			map.addMouseListener(this);
		}
		
		/**
		 * select a point
		 */
		public void mouseClicked(MouseEvent e) {
			int x = e.getX();
			int y = e.getY();
			MeshPoint point = choosePoint(x, y);
			infoIndex.setText(String.format("%d", point.index));
			infoMap.setText(String.format("x=%.7f, y=%.7f", point.x, point.y));
			infoWorld.setText(String.format("<%.6f,%.6f>", parms.latitude(point.y), parms.longitude(point.x)));
			
			double heightMap[] = map.getHeightMap();
			infoAlt.setText(String.format("%.1f%s MSL", parms.altitude(heightMap[point.index]), Parameters.unit_z));
			
			double fluxMap[] = map.getFluxMap();
			infoFlux.setText(String.format("%.1f%s", fluxMap[point.index], Parameters.unit_f));
			pack();
			
			double erodeMap[] = map.getErodeMap();
			infoErode.setText(String.format("%.2f%s", parms.altitude(-erodeMap[point.index]), Parameters.unit_z));
			
			double soilMap[] = map.getSoilMap();
			infoSoil.setText(Map.soil_names[(int) soilMap[point.index]]);
			
			double hydroMap[] = map.getHydrationMap();
			double h = hydroMap[point.index];
			infoHydro.setText(h < 1.0 ? String.format("%.0f%%",h) : 
										String.format("%d%s under water", (int) h - 1, Parameters.unit_z));		
			
			int downHill[] = map.getDownHill();
			infoDownhill.setText(String.format("%d", downHill[point.index]));
		}

		public MeshPoint choosePoint(int screen_x, int screen_y) {
			
			double x = map.x(screen_x);
			double y = map.y(screen_y);
			MeshPoint spot = new MeshPoint(x, y);
			MeshPoint closest = null;
			double distance = 2 * Parameters.x_extent;
			
			Mesh m = map.getMesh();
			for(int i = 0; i < m.vertices.length; i++) {
				MeshPoint point = m.vertices[i];
				double d = spot.distance(point);
				if (d < distance) {
					closest = point;
					distance = d;
				}
			}
			
			return closest;
		}
		
		/**
		 * Window Close event handler ... do nothing
		 */
		public void windowClosing(WindowEvent e) {
			this.dispose();
			map.removeMouseListener(this);
		}

		public void mouseMoved(MouseEvent arg0) {}
		public void mouseEntered(MouseEvent arg0) {}
		public void mouseExited(MouseEvent arg0) {}
		public void windowActivated(WindowEvent arg0) {}
		public void windowClosed(WindowEvent arg0) {}
		public void windowDeactivated(WindowEvent arg0) {}
		public void windowDeiconified(WindowEvent arg0) {}
		public void windowIconified(WindowEvent arg0) {}
		public void windowOpened(WindowEvent arg0) {}
		public void mousePressed(MouseEvent arg0) {}
		public void mouseReleased(MouseEvent arg0) {}
}
