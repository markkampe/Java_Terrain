package worldBuilder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

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
	
	// exit codes
	private static final int EXIT_OK = 0;
	// private static final int EXIT_ARGS = 1;
	// private static final int EXIT_ERROR = 2;
	
	// 2D canvas
	private Container mainPane;
	private Map map;
	
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
	
	private static final String[] ICON_IMAGES = {
			"/images/builder-16.png",
			"/images/builder-32.png",
			"/images/builder-64.png",
			"/images/builder-96.png",
			"/images/builder-128.png"
	};

	private static final String SWITCH_CHAR = "-";			// command line switches
	
	private static final String DEFAULT_TEMPLATE = "Templates/default_4096.json";
	private static final String DEFAULT_CONFIG = "Templates/worldBuilder.json";
	
	private static final long serialVersionUID = 0xdeadbeef;	// this is stupid
	
	/**
	 * instantiate a map and control panel
	 */
	public WorldBuilder(String filename) {
		
		// set our window icon(s)
		ArrayList<Image> icons = new ArrayList<Image>();
		for(int i = 0; i < ICON_IMAGES.length; i++)
			icons.add(getToolkit().getImage(getClass().getResource(ICON_IMAGES[i])));
		setIconImages(icons);
		
		// get a handle on our display window
		mainPane = getContentPane();
		int border = parms.border;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle(parms.title);
		addWindowListener( this );
		addComponentListener(this);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// if we were given an input file, use it
		Mesh m = new Mesh();
		if (filename != null)
			m.read(filename);
		modified = false;
		
		// create the main map panel, capture mouse events within it
		map = new Map(m, parms.width, parms.height);
		mainPane.add(map, BorderLayout.CENTER);	
		
		// create menus and widgets, put up the display
		createMenus();
		createWidgets();
		pack();
		setVisible(true);
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
		editWorld = new JMenuItem("map size/location");
		editWorld.addActionListener(this);
		editMountain = new JMenuItem("add mountain(s)");
		editMountain.addActionListener(this);
		editSlope = new JMenuItem("define slope");
		editSlope.addActionListener(this);
		editRain = new JMenuItem("rainfall");
		editRain.addActionListener(this);
		editCity = new JMenuItem("add city");
		editCity.addActionListener(this);
		editRoads = new JMenuItem("draw roads");
		editRoads.addActionListener(this);
		JMenu editMenu = new JMenu("Edit");
		editMenu.add(editWorld);
		editMenu.add(editSlope);
		editMenu.add(editMountain);
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
		
		// get some fonts
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		
		// create some sliders in panel #3
		erosion = new JSlider();
		JLabel eroLabel = new JLabel("Erosion", JLabel.CENTER);
		eroLabel.setFont(fontLarge);
		JPanel e_panel = new JPanel();
		e_panel.setLayout(new BoxLayout(e_panel, BoxLayout.PAGE_AXIS));
		e_panel.add(eroLabel);
		e_panel.add(erosion);
		
		seaLevel = new JSlider(-parms.msl_range, parms.msl_range, 0);
		seaLevel.setMajorTickSpacing(Parameters.niceTics(-parms.msl_range, parms.msl_range, true));
		seaLevel.setMinorTickSpacing(Parameters.niceTics(-parms.msl_range, parms.msl_range, false));
		seaLevel.setFont(fontSmall);
		seaLevel.setPaintTicks(true);
		seaLevel.setPaintLabels(true);
		JLabel seaLabel = new JLabel("Sea Level (m)", JLabel.CENTER);
		seaLabel.setFont(fontLarge);
		JPanel s_panel = new JPanel();
		s_panel.setLayout(new BoxLayout(s_panel, BoxLayout.PAGE_AXIS));
		s_panel.add(seaLabel);
		s_panel.add(seaLevel);
	
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.LINE_AXIS));
		e_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));	
		s_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		sliderPanel.add(s_panel);
		sliderPanel.add(e_panel);

		add(sliderPanel, BorderLayout.SOUTH);
		
		// register the action listeners
		erosion.addChangeListener(this);
		seaLevel.addChangeListener(this);
	}
	
	/**
	 * save the current map
	 */
	private void doSave(String filename) {
		String title = (filename == null) ? "Save As" : "Save";
		FileDialog d = new FileDialog(this, title, FileDialog.SAVE);
		d.setFile(filename == null ? "world.json" : filename);
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
			new MeshDialog(map);
			modified = true;
		} else if (o == fileOpen) {
			if (modified)
				checkSave();
			FileDialog d = new FileDialog(this, "Choose input file", FileDialog.LOAD);
			d.setLocation(parms.dialogDX, parms.dialogDY);
			d.setVisible(true);
			filename = d.getFile();
			if (filename != null) {
				Mesh m = new Mesh();
				String dir = d.getDirectory();
				if (dir != null)
					filename = dir + filename;
				m.read(filename);
				map.setMesh(m);
				modified = false;
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
			new ExportDialog(map);
		} else if (o == fileExit) {
			if (modified)
				checkSave();
			shutdown(EXIT_OK);
		} 
		
		// edit menus pop up the corresponding dialogs
		else if (o == editWorld) {
			new WorldDialog();
		} else if (o == editMountain) {
			new MountainDialog(map);
		} else if (o == editSlope) {
			new SlopeDialog(map);
		} else if (o == editRain) {
			new RainDialog(map);
		} else if (o == editCity) {
			System.out.println("implement edit:City");
		} else if (o == editRoads) {
			System.out.println("implement edit:Roads");
		}
		
		// view menu toggles individual views on and off
		else if (o == viewPoints)
			parms.display_options = map.setDisplay(Map.SHOW_POINTS, (parms.display_options & Map.SHOW_POINTS) == 0);
		else if (o == viewMesh)
			parms.display_options = map.setDisplay(Map.SHOW_MESH, (parms.display_options & Map.SHOW_MESH) == 0);
		else if (o == viewTopo)
			parms.display_options = map.setDisplay(Map.SHOW_TOPO, (parms.display_options & Map.SHOW_TOPO) == 0);
		else if (o == viewRain)
			parms.display_options = map.setDisplay(Map.SHOW_RAIN, (parms.display_options & Map.SHOW_RAIN) == 0);
		else if (o == viewWater)
			parms.display_options = map.setDisplay(Map.SHOW_WATER, (parms.display_options & Map.SHOW_WATER) == 0);
		else if (o == viewSoil)
			parms.display_options = map.setDisplay(Map.SHOW_SOIL, (parms.display_options & Map.SHOW_SOIL) == 0);
		else if (o == viewZoom)
			new ZoomDialog(map);
		// help menu just shows info
		else if (o == helpInfo) {
			JOptionPane.showMessageDialog(new JFrame(), 
					version +"\n" + author + "\n" + credit + "\n" + license, 
					"Information", JOptionPane.INFORMATION_MESSAGE);
		}
		updateDisplayMenus(parms.display_options);
	}
	
	// these may all be just place holders
	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		if (o == erosion) {
			System.out.println("Erosion changed to " + erosion.getValue());
		} else if (o == seaLevel) {
			parms.sea_level = ((double) seaLevel.getValue()) / parms.z_range;
			map.repaint();
		} else if (o == rainfall) {
			System.out.println("rainfall changed to " + rainfall.getValue());
		}
	}
	
	private void updateDisplayMenus(int opts) {
		viewPoints.setText( (opts & Map.SHOW_POINTS) != 0 ? "~points" : "Points");
		viewMesh.setText( (opts & Map.SHOW_MESH) != 0 ? "~mesh" : "Mesh");
		viewTopo.setText( (opts & Map.SHOW_TOPO) != 0 ? "~topo" : "Topo");
		viewRain.setText( (opts & Map.SHOW_RAIN) != 0 ? "~rain" : "Rain");
		viewWater.setText( (opts & Map.SHOW_WATER) != 0 ? "~water" : "Water");
		viewSoil.setText( (opts & Map.SHOW_SOIL) != 0 ? "~soil" : "Soil");
	}
	/**
	 * window resize
	 */
	public void componentResized(ComponentEvent e) {
		// TODO: send resize to map
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
		// process the arguments
		String filename = DEFAULT_TEMPLATE;
		int debug = 0;
		for( int i = 0; i < args.length; i++ ) {
			if (args[i].startsWith(SWITCH_CHAR)) {	
				if (args[i].startsWith("-d"))
					debug = new Integer(args[i].substring(2));
			} else {
				filename = args[i];		// Do I have non-switch args?
			}
		}
		// instantiate the parameters singleton
		parms = new Parameters(DEFAULT_CONFIG, debug);

		// create our display
		WorldBuilder w = new WorldBuilder(filename);
		
		// initialize the display type and options menus
		w.map.setDisplay(parms.display_options, true);
		w.updateDisplayMenus(parms.display_options);
	}
}
