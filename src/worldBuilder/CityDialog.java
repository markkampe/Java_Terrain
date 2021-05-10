package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * a Dialog to collect information about City Locations
 */
public class CityDialog extends JFrame implements WindowListener, MapListener, ActionListener {
		private Map map;
		private Parameters parms;

		private String[] nameMap;
		private int chosenPoint;
		
		private static final int BORDER_WIDTH = 5;
		private static final int NAME_WIDTH = 20;
		private static final Color SELECTED_COLOR = Color.MAGENTA;

		private JMenu typeMenu;
		private JMenuItem none, capitol, city, town, village;
		private JLabel type;
		private JTextField name;
		private JLabel lat;
		private JLabel lon;
		private JTextField descr;
		private JButton accept;
		private JButton delete;
		private JButton cancel;
		
		private static final long serialVersionUID = 1L;
		
		/** instantiate the City info collection widgets */
		public CityDialog(Map map)  {
			// pick up references
			this.map = map;
			this.parms = Parameters.getInstance();
			
			nameMap = map.getNameMap();
				
			// label the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Cities & Villages");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			Font fontSmall = new Font("Serif", Font.ITALIC, 10);
			
			// type selection menu
			JMenuBar bar = new JMenuBar();
			typeMenu = new JMenu("type");
			none = new JMenuItem("none");
			typeMenu.add(none);
			none.addActionListener(this);
			capitol = new JMenuItem("capitol");
			typeMenu.add(capitol);
			capitol.addActionListener(this);
			city = new JMenuItem("city");
			typeMenu.add(city);
			city.addActionListener(this);
			town = new JMenuItem("town");
			typeMenu.add(town);
			town.addActionListener(this);
			village = new JMenuItem("village");
			typeMenu.add(village);
			village.addActionListener(this);

			bar.add(typeMenu);

			JPanel info = new JPanel();
			info.setLayout(new GridLayout(2, 3));
			// first row: labels
			info.add(bar);
			JLabel l = new JLabel("Latitude");
			l.setFont(fontSmall);
			info.add(l);
			l = new JLabel("Longitude");
			l.setFont(fontSmall);
			info.add(l);
			// second row: data fields
			type = new JLabel("");
			info.add(type);
			lat = new JLabel("");
			info.add(lat);
			lon = new JLabel("");
			info.add(lon);
			mainPane.add(info, BorderLayout.NORTH);
			
			JPanel middle = new JPanel();
			middle.setLayout(new BoxLayout(middle, BoxLayout.PAGE_AXIS));
			l = new JLabel("name");
			l.setFont(fontSmall);
			middle.add(l);
			name = new JTextField("", NAME_WIDTH);
			middle.add(name);
			descr = new JTextField();
			l = new JLabel("Description");
			l.setFont(fontSmall);
			middle.add(l);
			middle.add(descr);
			mainPane.add(middle);
			
			// accept/cancel button
			accept = new JButton("CONFIRM");
			cancel = new JButton("CANCEL");
			delete = new JButton("DELETE");
			JPanel buttons = new JPanel();
			buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
			buttons.add(delete);
			buttons.add(Box.createRigidArea(new Dimension(40,0)));
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
			delete.addActionListener(this);
			
			chosenPoint = -1;
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
			map.selectMode(Map.Selection.NONE);
			map.highlight(-1,  null);
			
			// identify the nearest MeshPoint
			MeshPoint point = map.mesh.choosePoint(map_x, map_y);
			map.highlight(point.index, SELECTED_COLOR);
			chosenPoint = point.index;
			
			lat.setText(String.format("%.5f", parms.latitude(point.y)));
			lon.setText(String.format("%.5f", parms.longitude(point.x)));
			String d = nameMap[chosenPoint];
			if (d != null) {
				parseName(d);
			} else {
				type.setText("");
				descr.setText("");
				name.setText("");
			}
				
			// and enable the next selection
			map.selectMode(Map.Selection.POINT);
			return true;
		}
		
		/**
		 * update the map to name the new city
		 */
		private void confirmPoint() {
			nameMap[chosenPoint] = String.format("%s: %s - %s", type.getText(), name.getText(), descr.getText());
		}
		
		/**
		 * remove this city from the map
		 */
		private void deletePoint() {
			nameMap[chosenPoint] = null;
		}
		
		/**
		 * lex off the type, name, and description fields
		 * @param n - name field to be lexed
		 */
		private void parseName(String n) {
			// start with simple defaults
			type.setText("");
			name.setText("");
			descr.setText(n);
			
			// type is the field before the colon
			int blank = n.indexOf(' ');
			int colon = n.indexOf(':');
			if (colon > 0 && colon < blank) {
				type.setText(n.substring(0, colon));
				int dash = n.indexOf('-', colon+2);
				if (dash > 0) {
					name.setText(n.substring(colon+2, dash));
					descr.setText(n.substring(dash + 2));
				}
			}
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
		 * respond to button pushes and menu-item selections
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == accept) {
				confirmPoint();
			} else if (e.getSource() == cancel) {
				cancelDialog();
			} else if (e.getSource() == delete) {
				deletePoint();
			} else if (e.getSource() == none) {
				type.setText("");
			} else if (e.getSource() == capitol) {
				type.setText("capitol");
			} else if (e.getSource() == city) {
				type.setText("city");
			} else if (e.getSource() == town) {
				type.setText("town");
			} else if (e.getSource() == village) {
				type.setText("village");
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
