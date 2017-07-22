package worldBuilder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


public class WorldBuilder  extends JFrame 
						   implements ActionListener, ChangeListener,  WindowListener, ComponentListener {
	
	// identification information
	private static final String version = "WorldBuilder 0.1";
	private static final String author = "Author: Mark Kampe (mark.kampe@gmail.com)";
	private static final String credit = "Based on Martin O'Leary's Uncharted Atlas terrain generator (mewo2.com)";
	private static final String license = "";	// TBD
	
	// active file
	private String filename;	// name of current input/output file
	private boolean modified;	// should this file be saved
	private static final String DEFAULT_MAP = "default_4096.json";
	
	private static final String INPUT_TYPE = "*.json";
	private static final String EXPORT_TYPE = "*.json";

	// exit codes
	private static final int EXIT_OK = 0;
	private static final int EXIT_ARGS = 1;
	private static final int EXIT_ERROR = 2;
	
	// 2D canvas
	private Container mainPane;
	private Map map;	
	
	// view status
	private int viewing_points;
	private int viewing_mesh;
	private int viewing_topo;
	private int viewing_rain;
	private int viewing_water;
	private int viewing_soil;
	
	// menu bar items
	private JMenuItem fileNew;
	private JMenuItem fileOpen;
	private JMenuItem fileSave;
	private JMenuItem fileSaveAs;
	private JMenuItem fileClose;
	private JMenuItem fileExport;
	private JMenuItem fileExit;
	private JMenuItem editWorld;
	private JMenuItem editMountain;
	private JMenuItem editSlope;
	private JMenuItem editRain;
	private JMenuItem editCity;
	private JMenuItem editRoads;
	private JMenuItem viewPoints;
	private JMenuItem viewMesh;
	private JMenuItem viewTopo;
	private JMenuItem viewRain;
	private JMenuItem viewWater;
	private JMenuItem viewSoil;
	private JMenuItem viewZoom;
	private JMenuItem helpInfo;
	
	// control widgets
	private JSlider rainfall;
	private JSlider erosion;
	private JSlider seaLevel;

	
	
	// configuration
	private static Parameters parms;						// global program parameters
	private static final int BORDER_WIDTH = 10;				// window border
	private static final String ICON_IMAGE = "images/world-32.png";
	private static final String SWITCH_CHAR = "-";			// command line switches
	private static final long serialVersionUID = 0xdeadbeef;	// this is stupid
	
	/**
	 * instantiate a map and control panel
	 */
	public WorldBuilder(String filename) {
		
		// set our window icon
		//Image myIcon = getToolkit().getImage(getClass().getResource(ICON_IMAGE));
		//setIconImage(myIcon);
		
		// get a handle on our display window
		mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
		setTitle("World Builder");
		addWindowListener( this );
		addComponentListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the main map panel, capture mouse events within it
		map = new Map(parms.width, parms.height);
		mainPane.add(map, BorderLayout.CENTER);	
		
		// create menus and widgets, put up the display
		createMenus();
		createWidgets();
		pack();
		setVisible(true);
		
		// if we were given an input file, use it
		if (filename != null) {
			Mesh m = new Mesh();
			m.read(filename);
			map.setMesh(m);
		}
		modified = false;
	}
	
	/**
	 * create the hierarchy of menus that will drive most 
	 * of our actions
	 */
	private void createMenus() {
		
		// create File menu
		fileNew = new JMenuItem("New");
		fileNew.addActionListener(this);
		fileOpen = new JMenuItem("Open");
		fileOpen.addActionListener(this);
		fileSave = new JMenuItem("Save");
		fileSave.addActionListener(this);
		fileSaveAs = new JMenuItem("Save as");
		fileSaveAs.addActionListener(this);
		fileClose = new JMenuItem("Close file");
		fileClose.addActionListener(this);
		fileExport = new JMenuItem("Export");
		fileExport.addActionListener(this);
		fileExit = new JMenuItem("Exit");
		fileExit.addActionListener(this);
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(fileNew);
		fileMenu.add(fileOpen);
		fileMenu.add(fileSave);
		fileMenu.add(fileSaveAs);
		fileMenu.add(fileClose);
		fileMenu.add(fileExport);
		fileMenu.add( new JSeparator() );
		fileMenu.add(fileExit);
		
		// create our edit menu
		editWorld = new JMenuItem("world size");
		editWorld.addActionListener(this);
		editMountain = new JMenuItem("add mountain(s)");
		editMountain.addActionListener(this);
		editSlope = new JMenuItem("define slope");
		editSlope.addActionListener(this);
		editRain = new JMenuItem("configure rain");
		editRain.addActionListener(this);
		editCity = new JMenuItem("add city");
		editCity.addActionListener(this);
		editRoads = new JMenuItem("draw roads");
		editRoads.addActionListener(this);
		JMenu editMenu = new JMenu("Edit");
		editMenu.add(editWorld);
		editMenu.add(editMountain);
		editMenu.add(editSlope);
		editMenu.add(editRain);
		editMenu.add(new JSeparator());
		editMenu.add(editCity);
		editMenu.add(editRoads);
		
		// create our view menu
		viewPoints = new JMenuItem("Points");
		viewPoints.addActionListener(this);
		viewMesh = new JMenuItem("Mesh");
		viewMesh.addActionListener(this);
		viewTopo = new JMenuItem("Topo");
		viewTopo.addActionListener(this);
		viewRain = new JMenuItem("Rain");
		viewRain.addActionListener(this);
		viewWater = new JMenuItem("Water");
		viewWater.addActionListener(this);
		viewSoil = new JMenuItem("Soil");
		viewSoil.addActionListener(this);
		viewZoom = new JMenuItem("Zoom");
		viewZoom.addActionListener(this);
		JMenu viewMenu = new JMenu("View");
		viewMenu.add(viewPoints);
		viewMenu.add(viewMesh);
		viewMenu.add(viewTopo);
		viewMenu.add(viewRain);
		viewMenu.add(viewWater);
		viewMenu.add(viewSoil);
		viewMenu.add(viewZoom);
		
		// create help menu
		helpInfo = new JMenuItem("about WorldBuilder");
		helpInfo.addActionListener(this);
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(helpInfo);
		
		// assemble the menu bar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add( editMenu );
		menuBar.add( viewMenu );
		menuBar.add( helpMenu );
		setJMenuBar( menuBar );	
	}
	
	/**
	 * create a panel of control widgets
	 */
	private void createWidgets() {
		
		// create some sliders in panel #3
		rainfall = new JSlider();
		erosion = new JSlider();
		seaLevel = new JSlider();
		JPanel sliderPanel = new JPanel(new GridLayout(2,3));
		sliderPanel.add(rainfall);
		sliderPanel.add(erosion);
		sliderPanel.add(seaLevel);
		sliderPanel.add(new JLabel("Rainfall", JLabel.CENTER));
		sliderPanel.add(new JLabel("Erosion", JLabel.CENTER));
		sliderPanel.add(new JLabel("Sea Level", JLabel.CENTER));
		JPanel sliders = new JPanel();	// no stretch
		sliders.add(sliderPanel);
		
		// add those three panels to a control panel
		JPanel controls = new JPanel(new GridLayout(3,1));
		// controls.add(buttons);
		// controls.add(combos);
		controls.add(sliders);
		add(controls, BorderLayout.SOUTH);
		
		// register the action listeners
		rainfall.addChangeListener(this);
		erosion.addChangeListener(this);
		seaLevel.addChangeListener(this);
	}
	
	/**
	 * save the current map
	 */
	private void doSave(String filename) {
		String title = (filename == null) ? "Save As" : "Save";
		FileDialog d = new FileDialog(this, title, FileDialog.SAVE);
		d.setFile(filename == null ? INPUT_TYPE : filename);
		d.setLocation(parms.dialogDX, parms.dialogDY);
		d.setVisible(true);
		filename = d.getFile();
		if (filename != null) {
			String dir = d.getDirectory();
			if (dir != null)
				filename = dir + filename;
			map.getMesh().write(filename);
			modified = false;
		}
	}
	
	/**
	 * see the current map should be saved
	 */
	private void checkSave() {
		if (JOptionPane.showConfirmDialog(new JFrame(), "Save current map?", "Save?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			doSave(filename);
		}	
	}

	public void actionPerformed( ActionEvent e ) {
		Object o = e.getSource();
		
		// file menu opens, closes, saves, and exports files
		if (o == fileNew) {
			if (modified)
				checkSave();
			filename = null;
			MeshDialog d = new MeshDialog(map);
			modified = true;
		} else if (o == fileOpen) {
			if (modified)
				checkSave();
			FileDialog d = new FileDialog(this, "Choose input file", FileDialog.LOAD);
			d.setFile(INPUT_TYPE);
			d.setLocation(parms.dialogDX, parms.dialogDY);
			d.setVisible(true);
			filename = d.getFile();
			if (filename != null) {
				Mesh m = new Mesh();
				String dir = d.getDirectory();
				if (dir != null)
					filename = dir + filename;
				m.read(filename);
				modified = false;
				map.setMesh(m);
			}
		} else if (o == fileSave) {
			doSave(filename);
		} else if (o == fileSaveAs) {
			doSave(null);
		} else if (o == fileClose) {
			if (modified)
				checkSave();
			map.setMesh(null);
			modified = false;
		} else if (o == fileExport) {
			FileDialog d = new FileDialog(this, "Export", FileDialog.SAVE);
			d.setFile(EXPORT_TYPE);
			d.setLocation(parms.dialogDX, parms.dialogDY);
			d.setVisible(true);
			String export_file = d.getFile();
			if (export_file != null) {
				String dir = d.getDirectory();
				if (dir != null)
					export_file = dir + export_file;
				new ExportDialog(map, export_file);
			}
		} else if (o == fileExit) {
			if (modified)
				checkSave();
			shutdown(EXIT_OK);
		} 
		
		// edit menus pop up the corresponding dialogs
		else if (o == editWorld) {
			new WorldDialog(map);
		} else if (o == editMountain) {
			new MountainDialog(map);
		} else if (o == editSlope) {
			new SlopeDialog(map);
		} else if (o == editRain) {
			System.out.println("implement edit:Rain");
		} else if (o == editCity) {
			System.out.println("implement edit:City");
		} else if (o == editRoads) {
			System.out.println("implement edit:Roads");
		}
		
		// view menu toggles individual views on and off
		else if (o == viewPoints) {
			viewing_points = map.setDisplay(Map.SHOW_POINTS, viewing_points == 0);
			viewPoints.setText(viewing_points != 0 ? "~points" : "Points");
		} else if (o == viewMesh) {
			viewing_mesh = map.setDisplay(Map.SHOW_MESH, viewing_mesh == 0);
			viewMesh.setText(viewing_mesh != 0 ? "~mesh" : "Mesh");
		} else if (o == viewTopo) {
			viewing_topo = map.setDisplay(Map.SHOW_TOPO, viewing_topo == 0);
			viewTopo.setText(viewing_topo != 0 ? "~topo" : "Topo");
		} else if (o == viewRain) {
			viewing_rain = map.setDisplay(Map.SHOW_RAIN, viewing_rain == 0);
			viewRain.setText(viewing_rain != 0 ? "~rain" : "Rain");
		} else if (o == viewWater) {
			viewing_water = map.setDisplay(Map.SHOW_WATER, viewing_water == 0);
			viewWater.setText(viewing_water != 0 ? "~water" : "Water");
		} else if (o == viewSoil) {
			viewing_soil = map.setDisplay(Map.SHOW_SOIL, viewing_soil == 0);
			viewSoil.setText(viewing_soil != 0 ? "~soil" : "Soil");
		} else if (o == viewZoom) {
			new ZoomDialog(map);
		}
		
		// help menu just shows info
		else if (o == helpInfo) {
			JOptionPane.showMessageDialog(new JFrame(), 
					version +"\n" + author + "\n" + credit + "\n" + license, 
					"Information", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	// these may all be just place holders
	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		
		if (o == erosion) {
			System.out.println("Erosion changed to " + erosion.getValue());
		} else if (o == seaLevel) {
			System.out.println("seaLevel changed to " + seaLevel.getValue());
		} else if (o == rainfall) {
			System.out.println("rainfall changed to " + seaLevel.getValue());
		}
	}
	
	/**
	 * window resize
	 */
	public void componentResized(ComponentEvent e) {
	}
	
	/**
	 * shut down the program
	 * 
	 * @param exitCode
	 */
	void shutdown(int exitCode) {
		System.exit(exitCode);
	}

	// perfunctory event handlers
	public void windowClosing(WindowEvent e) { shutdown(EXIT_OK); }	
	public void windowActivated(WindowEvent arg0) {	}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}
	public void componentShown(ComponentEvent arg0) {}
	public void componentHidden(ComponentEvent arg0) {}
	public void componentMoved(ComponentEvent arg0) {}

	public static void main(String[] args) {
		// instantiate a parameters singleton
		parms = Parameters.getInstance();
		String filename = null;
		
		// process the arguments
		for( int i = 0; i < args.length; i++ ) {
			if (args[i].startsWith(SWITCH_CHAR)) {	
				parms.parseSwitch( args[i].substring(1));
			} else {
				String fileName = args[i];		// Do I have non-switch args?
			}
		}
		
		// create our display
		if (filename == null)
			filename = DEFAULT_MAP;
		WorldBuilder w = new WorldBuilder(filename);
		
		// initialize the display type
		w.viewing_mesh = w.map.setDisplay(Map.SHOW_MESH, true);
		w.viewMesh.setText("~mesh");
	}
}
