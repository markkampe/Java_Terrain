package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import java.util.Iterator;
import java.util.LinkedList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

/**
 * a Dialog to collect information about a Point of Interest
 */
public class POIDialog extends JFrame implements WindowListener, MapListener, ActionListener, ListSelectionListener {
		private Map map;
		private MapWindow window;
		private Parameters parms;
			
		private static final int BORDER_WIDTH = 5;
		
		private static final long serialVersionUID = 1L;
		
		private static final int[] column_widths = new int[] { 100, 180, 60, 60 };
		private static final String[] column_names = new String[] { "Type", "Name", "Lat", "Lon" };
		private static final int COL_TYPE = 0;
		private static final int COL_NAME = 1;
		private static final int COL_LAT = 2;
		private static final int COL_LON = 3;
		
		Object[][] data;		// the data to be displayed
		double x[], y[];		// corresponding X/Y coordinates
		
		private JTable table;
		private JButton accept;
		private JButton cancel;
		
		private static final int MAX_POIS = 10;
		
		/** instantiate the POI information collection widgets */
		public POIDialog(Map map)  {
			// pick up references
			this.map = map;
			this.window = map.window;
			this.parms = Parameters.getInstance();
			
			// import the current PoI list into a data table
			LinkedList<POI> poi_list = map.getPOI();
			int len = poi_list.size();
			if (len < MAX_POIS)
				len = MAX_POIS;
			data = new Object[len][4];
			x = new double[len];
			y = new double[len];
			int numPoints = 0;
			for(Iterator<POI> it = poi_list.iterator(); it.hasNext();) {
				POI p = it.next();
				data[numPoints][COL_TYPE] = p.type;
				data[numPoints][COL_NAME] = p.name;
				data[numPoints][COL_LAT] = parms.latitude(p.y);
				data[numPoints][COL_LON] = parms.longitude(p.x);
				y[numPoints] = p.y;
				x[numPoints] = p.x;
				numPoints++;
			}
				
			// label the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Points of Interest");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	
			// create a table of points of interest
			DefaultTableModel model = new DefaultTableModel(data, column_names) {
				@Override
				public boolean isCellEditable(int row, int col) {
					return (col < COL_LAT);
				}
				@Override
				public Class<?> getColumnClass(int col) {
					return (col < COL_LAT) ? String.class : Double.class;
				}
				private static final long serialVersionUID = 1L;
			};
			table = new JTable(model);
			TableColumnModel tcm = table.getColumnModel();
			for(int i = 0; i < column_widths.length; i++)
				tcm.getColumn(i).setPreferredWidth(column_widths[i]);
			mainPane.add(new JScrollPane(table),  BorderLayout.CENTER);
			
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
			table.getSelectionModel().addListSelectionListener(this);
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
			
			// update the selected (or first empty) row
			int row = table.getSelectedRow();
			if (row < 0) {
				for(int i = 0; i < data.length; i++)
					if (data[i][0] == null || data[i][0].equals("")) {
						row = i;
						break;
					}
			}
			if (row >= 0) {
				x[row] = map_x;
				y[row] = map_y;
				table.setValueAt(parms.latitude(map_y), row, COL_LAT);
				table.setValueAt(parms.longitude(map_y), row, COL_LON);
			}
			
			return true;
		}
		
		public void valueChanged(ListSelectionEvent e) {
			int row = table.getSelectedRow();
			String type = (String) table.getValueAt(row, COL_TYPE);
			if (type != null && !type.equals("")) {
				window.highlight(x[row],  y[row]);	
				window.repaint();
			}
		}
		
		/**
		 * commit our changes to the points of interest map
		 */
		private void updatePOIs() {
			// empty the existing list
			LinkedList<POI> poi_list = map.getPOI();
			while(poi_list.size() > 0)
				poi_list.remove();
			
			// copy data from our table back into the list
			for(int i = 0; i < data.length; i++) {
				String type = (String) table.getValueAt(i, 0);
				// PoI must have a name
				if (type == null || type.equals(""))
					continue;
				// PoI must have a position
				if (x[i] == 0 && y[i] == 0)
					continue;
				String name = (String) table.getValueAt(i, COL_NAME);
				map.addPOI(new POI(type, name, x[i], y[i]));
			}
		}
		
		/**
		 * unregister our listeners and exit
		 */
		private void cancelDialog() {
			window.highlight(-1,  null);			// clear highlights
			window.selectMode(MapWindow.Selection.ANY); 	// clear selections
			window.removeMapListener(this);
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
				updatePOIs();
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
