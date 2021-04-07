package worldBuilder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;


/**
 * main class - instantiate JFrame, map and widgets and service control actions on them
 */
public class WorldBuilder  extends JFrame 
						   implements ActionListener, ChangeListener,  WindowListener, ComponentListener {
	
	/** default configuration file to be read on start-up	*/
	public static final String DEFAULT_TEMPLATE = "/Templates/default_%d.json";
	
	// identification information
	private static final String version = "WorldBuilder 0.1";
	private static final String author = "Author: Mark Kampe (mark.kampe@gmail.com)";
	private static final String credit = "Inspired by Martin O'Leary's Uncharted Atlas terrain generator (mewo2.com)";
	private static final String license = "";	// TBD
	private static final String usage = "Usage: cmd [-v] [-d debuglevel] [-c configfile] [-p projectdir] [mesh file]";
	
	/** active mouse-hogging dialog (for serialization)	*/
	protected static boolean activeDialog;
	
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
	
	// counter to position off-map dialogs
	private int numDialogs = 0;
	private static final int MAX_DIALOGS = 4;
	
	// menu bar items
	private JMenuItem newMesh;
	private JMenuItem newRegion;
	private JMenuItem fileOpen;
	private JMenuItem fileSave;
	private JMenuItem fileSaveAs;
	private JMenuItem fileClose;
	private JMenuItem fileProject;
	private JMenuItem exportRaw;
	private JMenuItem exportRpgmOverworld;
	private JMenuItem exportRpgmOutside;
	private JMenuItem exportObject;
	private JMenuItem exportFoundation;
	private JMenuItem fileExit;
	private JMenuItem editWorld;
	private JMenuItem editMountain;
	private JMenuItem editLand;
	private JMenuItem editSlope;
	private JMenuItem editRiver;
	private JMenuItem editRain;
	private JMenuItem editFlora;
	private JMenuItem editRocks;
	private JMenuItem editCity;
	private JMenuItem editRoads;
	private JMenuItem viewPoints;
	private JMenuItem viewMesh;
	private JMenuItem viewTopo;
	private JMenuItem viewRain;
	private JMenuItem viewWater;
	private JMenuItem viewErode;
	private JMenuItem viewRocks;
	private JMenuItem viewFlora;
	private JMenuItem viewFauna;
	private JMenuItem viewZoom;
	private JMenuItem viewDebug;
	private JMenuItem helpInfo;
	private JMenuItem ruleDebug;
	private JMenuItem debug0;
	private JMenuItem debug1;
	private JMenuItem debug2;
	private JMenuItem debug3;
	private JMenuItem flora_legend;
	private JMenuItem rock_legend;
	
	// control widgets
	private JSlider seaLevel;
	
	// configuration
	private static Parameters parms;						// global program parameters
	
	private static final String[] ICON_IMAGES = {
			"/icons/builder-16.png",
			"/icons/builder-32.png",
			"/icons/builder-64.png",
			"/icons/builder-96.png",
			"/icons/builder-128.png"
	};

	private static final String SWITCH_CHAR = "-";			// command line switches
	private static final long serialVersionUID = 0xdeadbeef;	// this is stupid
	
	/**
	 * instantiate window containing map and menus
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
		
		// create the display map generator
		map = new Map(parms.width, parms.height);
		mainPane.add(map, BorderLayout.CENTER);	
		
		// if we were given an input file, use it
		map.read(filename);
		modified = false;	
		
		// create menus and widgets, put up the display
		createMenus();
		createWidgets();
		pack();
		setVisible(true);
	}
	
	/**
	 * create hierarchy of menus that will drive our actions
	 */
	private void createMenus() {
		
		// create File menu
		newMesh = new JMenuItem("World");
		newMesh.addActionListener(this);
		newRegion = new JMenuItem("sub-region");
		newRegion.addActionListener(this);
		JMenu fileNew = new JMenu("new");
		fileNew.add(newMesh);
		fileNew.add(newRegion);

		fileOpen = new JMenuItem("Open");
		fileOpen.addActionListener(this);
		fileSave = new JMenuItem("Save");
		fileSave.addActionListener(this);
		fileSaveAs = new JMenuItem("Save as");
		fileSaveAs.addActionListener(this);
		fileClose = new JMenuItem("Close file");
		fileClose.addActionListener(this);
		fileProject = new JMenuItem("Project dir");
		fileProject.addActionListener(this);

		exportRaw = new JMenuItem("Raw JSON");
		exportRaw.addActionListener(this);
		exportRpgmOverworld = new JMenuItem("Overworld");
		exportRpgmOverworld.addActionListener(this);
		exportRpgmOutside = new JMenuItem("Outside");
		exportRpgmOutside.addActionListener(this);
		exportFoundation = new JMenuItem("Foundation(WIP)");
		exportFoundation.addActionListener(this);
		exportObject = new JMenuItem("Object Overlay(WIP)");
		exportObject.addActionListener(this);
		JMenu fileExport = new JMenu("Export");
		JMenu exportRPGM = new JMenu("RPGMaker");
		exportRPGM.add(exportRpgmOverworld);
		exportRPGM.add(exportRpgmOutside);
		fileExport.add(exportRPGM);
		fileExport.add(exportFoundation);
		fileExport.add(exportObject);
		fileExport.add(exportRaw);
		
		fileExit = new JMenuItem("Exit");
		fileExit.addActionListener(this);
		
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(fileNew);
		fileMenu.add(fileOpen);
		fileMenu.add(fileSave);
		fileMenu.add(fileSaveAs);
		fileMenu.add(fileClose);
		fileMenu.add(fileProject);
		fileMenu.add(fileExport);
		fileMenu.add( new JSeparator() );
		fileMenu.add(fileExit);
		
		// create our edit menu
		editWorld = new JMenuItem("world parameters");
		editWorld.addActionListener(this);
		editMountain = new JMenuItem("add mountain(s)");
		editMountain.addActionListener(this);
		editLand = new JMenuItem("soil/height/transport");
		editLand.addActionListener(this);
		editSlope = new JMenuItem("continental slope");
		editSlope.addActionListener(this);
		editRain = new JMenuItem("define rainfall");
		editRain.addActionListener(this);
		editRiver = new JMenuItem("add arterial river");
		editRiver.addActionListener(this);
		editFlora = new JMenuItem("flora distribution");
		editFlora.addActionListener(this);
		editRocks = new JMenuItem("mineral distribution");
		editRocks.addActionListener(this);
		editCity = new JMenuItem("add city");
		editCity.addActionListener(this);
		editCity.setEnabled(false);	// SOMEDAY implement city creation
		editRoads = new JMenuItem("draw roads");
		editRoads.addActionListener(this);
		editRoads.setEnabled(false);	// SOMEDAY implement roads
		JMenu editMenu = new JMenu("Edit");
		editMenu.add(editWorld);
		editMenu.add(editSlope);
		editMenu.add(editMountain);
		editMenu.add(editLand);
		editMenu.add(editRiver);
		editMenu.add(editRain);
		editMenu.add(editFlora);
		editMenu.add(editRocks);
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
		viewErode = new JMenuItem("Erosion");
		viewErode.addActionListener(this);
		viewRocks = new JMenuItem("Minerals");
		viewRocks.addActionListener(this);
		viewFlora = new JMenuItem("Flora");
		viewFlora.addActionListener(this);
		viewFauna = new JMenuItem("Fauna");
		viewFauna.addActionListener(this);
		viewZoom = new JMenuItem("Zoom");
		viewZoom.addActionListener(this);
		viewDebug = new JMenuItem("Point Details");
		viewDebug.addActionListener(this);
		JMenu viewMenu = new JMenu("View");
		viewMenu.add(viewPoints);
		viewMenu.add(viewMesh);
		viewMenu.add(viewTopo);
		viewMenu.add(viewRain);
		viewMenu.add(viewWater);
		viewMenu.add(viewErode);
		viewMenu.add(viewRocks);
		viewMenu.add(viewFlora);
		viewMenu.add(viewFauna);
		viewMenu.add(viewZoom);
		viewMenu.add(new JSeparator());
		viewMenu.add(viewDebug);
		
		// create help menu
		helpInfo = new JMenuItem("about WorldBuilder");
		helpInfo.addActionListener(this);
		
		// Map color legends
		flora_legend = new JMenuItem("flora");
		rock_legend = new JMenuItem("minerals");
		JMenu legendMenu = new JMenu("Map legends");
		legendMenu.add(flora_legend);
		legendMenu.add(rock_legend);
		flora_legend.addActionListener(this);
		rock_legend.addActionListener(this);
		
		// output verbosity
		debug0 = new JMenuItem("none");
		debug1 = new JMenuItem("basic");
		debug2 = new JMenuItem("verbose");
		debug3 = new JMenuItem("debug");
		JMenu dbgMenu = new JMenu("Verbosity");
		dbgMenu.add(debug0);
		dbgMenu.add(debug1);
		dbgMenu.add(debug2);
		dbgMenu.add(debug3);
		ruleDebug = new JMenuItem("Rule Trace");
		ruleDebug.addActionListener(this);
		debug0.addActionListener(this);
		debug1.addActionListener(this);
		debug2.addActionListener(this);
		debug3.addActionListener(this);

		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(helpInfo);
		helpMenu.add(legendMenu);
		helpMenu.add(ruleDebug);
		helpMenu.add(dbgMenu);
		
		// assemble the menu bar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add( editMenu );
		menuBar.add( viewMenu );
		menuBar.add( helpMenu );
		setJMenuBar( menuBar );	
	}
	
	/**
	 * create a panel of control widgets (Sea-Level slider)
	 */
	private void createWidgets() {
		
		// get some fonts
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		
		// create some sliders in panel #3
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
		s_panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 10));
		sliderPanel.add(s_panel);

		add(sliderPanel, BorderLayout.SOUTH);
		
		// register the action listeners
		seaLevel.addChangeListener(this);
	}
	
	/**
	 * save the current map
	 * @param filename of the map to be saved
	 */
	private void doSave(String filename) {
		JFileChooser d = new JFileChooser();
		d.setApproveButtonText("Save");
		if (filename != null) {
			d.setDialogTitle("Save");
			d.setSelectedFile(new File(filename));
		} else {
			d.setDialogTitle("Save As");
			d.setSelectedFile(new File(parms.map_name + ".json"));
			if (parms.world_dir != null)
				d.setCurrentDirectory(new File(parms.world_dir));
		}
		
		d.setFileSelectionMode(JFileChooser.FILES_ONLY);
		FileFilter jsonFilter = new FileFilter() {
			public boolean accept(File f) {
				if (f.isDirectory())
					return true;
				String filename = f.getPath();
				return filename.endsWith(".json");
			}
			public String getDescription() {
				return("json");
			}
		};
		d.addChoosableFileFilter(jsonFilter);
		d.setFileFilter(jsonFilter);
		if (d.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			filename = d.getSelectedFile().getPath();
			map.write(filename);
			String dirname = d.getCurrentDirectory().getPath();
			if (dirname != null)
				parms.world_dir = dirname;
			modified = false;
		}
	}
	
	/**
	 * see the current map should be saved, and of so, save it
	 */
	private void checkSave() {
		if (JOptionPane.showConfirmDialog(new JFrame(), "Save current map?", "Save?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
			doSave(filename);
		}	
	}

	/**
	 * menu item selection handler ... where everything happens
	 */
	public void actionPerformed( ActionEvent e ) {
		Object o = e.getSource();
		// file menu opens, closes, saves, and exports files
		if (o == newMesh) {
			if (modified)
				checkSave();
			filename = null;
			placeDialog(new MeshDialog(map), true);
			modified = true;
		} else if (o == newRegion) {
			if (modified)
				checkSave();
			placeDialog(new RegionDialog(map), false);
			filename = null;
		} else if (o == fileOpen) {
			if (modified)
				checkSave();
			JFileChooser d = new JFileChooser();
			d.setDialogTitle("Choose world description");
			d.setCurrentDirectory(new File(parms.world_dir));
			d.setFileSelectionMode(JFileChooser.FILES_ONLY);
			FileFilter jsonFilter = new FileFilter() {
				public boolean accept(File f) {
					if (f.isDirectory())
						return true;
					String filename = f.getPath();
					return filename.endsWith(".json");
				}
				public String getDescription() {
					return("json");
				}
			};
			d.addChoosableFileFilter(jsonFilter);
			d.setFileFilter(jsonFilter);
			if (d.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				filename = d.getSelectedFile().getPath();
				map.read(filename);
				String dir = d.getCurrentDirectory().getPath();
				if (dir != null)
					parms.world_dir = dir;
				
				// newly loaded map may have changed the sea-level
				seaLevel.setValue((int)(parms.sea_level * parms.z_range));
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
		} else if (o == fileProject) {
			JFileChooser d = new JFileChooser();
			d.setDialogTitle("Choose project directory");
			d.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			d.setAcceptAllFileFilterUsed(false);
			if (d.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				parms.project_dir = d.getSelectedFile().getPath();
				new MapIndex(parms.project_dir);
			}
		} else if (o == exportRaw) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new RawExport(map), false);
				activeDialog = true;
			} 
		} else if (o == exportRpgmOverworld) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new RPGMexport("Overworld", map), false);
				activeDialog = true;
			} 
		} else if (o == exportRpgmOutside) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new RPGMexport("Outside", map), false);
				activeDialog = true;
			} 
		} else if (o == exportFoundation) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new FoundationExport(map), false);
				activeDialog = true;
			} 
		} else if (o == exportObject) { 
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new ObjectExport(map), false);
				activeDialog = true;
			} 
		} else if (o == fileExit) {
			if (modified)
				checkSave();
			shutdown(EXIT_OK);
		} 
		
		// edit menus pop up the corresponding dialogs
		else if (o == editWorld) {
			placeDialog(new WorldDialog(map.isSubRegion), true);
		} else if (o == editMountain) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new MountainDialog(map), false);
				activeDialog = true;
			}
		} else if (o == editLand) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new LandDialog(map), false);
				activeDialog = true;
			}
		} else if (o == editSlope) {
			placeDialog(new SlopeDialog(map), false);
		} else if (o == editRain) {
			parms.display_options = map.setDisplay(Map.SHOW_RAIN, true);
			placeDialog(new RainDialog(map), false);
		} else if (o == editRiver) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new RiverDialog(map), false);
				activeDialog = true;
			}
		} else if (o == editFlora) {
			if (activeDialog)
				twoDialogError();
			else {
				parms.display_options = map.setDisplay(Map.SHOW_FLORA, true);
				placeDialog(new FloraDialog(map), false);
				activeDialog = true;
			}
		} else if (o == editRocks) {
			if (activeDialog)
				twoDialogError();
			else {
				parms.display_options = map.setDisplay(Map.SHOW_ROCKS, true);
				placeDialog(new MineralDialog(map), false);
				activeDialog = true;
			}
		} else if (o == editCity) {
			System.err.println("implement edit:City");
		} else if (o == editRoads) {
			System.err.println("implement edit:Roads");
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
		else if (o == viewErode)
			parms.display_options = map.setDisplay(Map.SHOW_ERODE, (parms.display_options & Map.SHOW_ERODE) == 0);
		else if (o == viewRocks)
			parms.display_options = map.setDisplay(Map.SHOW_ROCKS, (parms.display_options & Map.SHOW_ROCKS) == 0);
		else if (o == viewFlora)
			parms.display_options = map.setDisplay(Map.SHOW_FLORA, (parms.display_options & Map.SHOW_FLORA) == 0);
		else if (o == viewFauna)
			parms.display_options = map.setDisplay(Map.SHOW_FAUNA, (parms.display_options & Map.SHOW_FAUNA) == 0);
		else if (o == viewZoom)	{
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new ZoomDialog(map), false);
				activeDialog = true;
			}
		} else if (o == viewDebug) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new PointDebug(map), false);
				activeDialog = true;
			}
		} else if (o == helpInfo) {		// help menu just shows info
			JOptionPane.showMessageDialog(new JFrame(), 
					version +"\n" + author + "\n" + credit + "\n" + license, 
					"Information", JOptionPane.INFORMATION_MESSAGE);
		} else if (o == ruleDebug) {
			placeDialog(new RuleDebug(), false);
		} else if (o == debug0) {
			parms.debug_level = 0;
			System.out.println("   verbosity:  " + parms.debug_level);
		} else if (o == debug1) {
			parms.debug_level = 1;
			System.out.println("   verbosity:  " + parms.debug_level);
		} else if (o == debug2) {
			parms.debug_level = 2;
			System.out.println("   verbosity:  " + parms.debug_level);
		} else if (o == debug3) {
			parms.debug_level = 3;
			System.out.println("   verbosity:  " + parms.debug_level);
		} else if (o == flora_legend) {
			placeDialog(new LegendDisplay("Flora", map.floraColors, map.floraNames), false);
		} else if (o == rock_legend) {
			placeDialog(new LegendDisplay("Mineral", map.rockColors, map.rockNames), false);
		}
		updateDisplayMenus(parms.display_options);
	}
	
	/**
	 * handle changes to erosion, sea-level and rainfall sliders
	 */
	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		if (o == seaLevel) {
			parms.sea_level = ((double) seaLevel.getValue()) / parms.z_range;
			map.setSeaLevel();
		}
	}
	
	private void updateDisplayMenus(int opts) {
		viewPoints.setText( (opts & Map.SHOW_POINTS) != 0 ? "~points" : "Points");
		viewMesh.setText( (opts & Map.SHOW_MESH) != 0 ? "~mesh" : "Mesh");
		viewTopo.setText( (opts & Map.SHOW_TOPO) != 0 ? "~topo" : "Topo");
		viewRain.setText( (opts & Map.SHOW_RAIN) != 0 ? "~rain" : "Rain");
		viewWater.setText( (opts & Map.SHOW_WATER) != 0 ? "~water" : "Water");
		viewErode.setText( (opts & Map.SHOW_ERODE) != 0 ? "~erosion" : "Erosion");
		viewRocks.setText( (opts & Map.SHOW_ROCKS) != 0 ? "~minerals" : "Minerals");
		viewFlora.setText( (opts & Map.SHOW_FLORA) != 0 ? "~flora" : "Flora");
		viewFauna.setText( (opts & Map.SHOW_FAUNA) != 0 ? "~fauna" : "Fauna");
	}
	
	/**
	 * put a dialog in a reasonable place on the screen
	 * 
	 * @param dialog to be placed
	 * @param can it overlap the map
	 */
	private void placeDialog(JFrame dialog, boolean overlap) {
		// figure out where it should go
		int x = this.getX();
		int y = this.getY();
		if (overlap) {
			// just center it on the screen
			x += (this.getWidth() - dialog.getWidth())/2;
			y += (this.getHeight() - dialog.getHeight())/2;
		} else {
			// place it above or to the right (depending on where there is space)
			// and stagger them so that concurrent dialogs don't overlap each other
			Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
			int xExtra = (int) (d.getWidth() - (this.getWidth() + dialog.getWidth()));
			int yExtra = (int) (d.getHeight() - (this.getHeight() + dialog.getHeight()));
			if (xExtra > yExtra) {
				x += this.getWidth();
				y += (this.getHeight() * (++numDialogs % MAX_DIALOGS))/MAX_DIALOGS;
			} else {
				x += (this.getWidth() * (++numDialogs % MAX_DIALOGS))/MAX_DIALOGS;
				y += this.getHeight();
			}
		}
		
		// put it there
		dialog.setLocation(x, y);
	}
	
	private void twoDialogError() {
		String message = "\nUnable to create this dialog until previous\n" +
						 "map window selection dialog is closed.";
		JOptionPane.showMessageDialog(new JFrame(),  message, "Dialog", JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * shut down the program
	 * 
	 * @param exitCode
	 */
	void shutdown(int exitCode) {
		System.exit(exitCode);
	}

	/** (perfunctory) */ public void windowClosing(WindowEvent e) { shutdown(EXIT_OK); }
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {	}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	/** (perfunctory) */ public void componentShown(ComponentEvent arg0) {}
	/** (perfunctory) */ public void componentHidden(ComponentEvent arg0) {}
	/** (perfunctory) */ public void componentMoved(ComponentEvent arg0) {}
	/** (perfunctory) */ public void componentResized(ComponentEvent e) {}

	/**
	 * main application entry point
	 * @param args [-v] [-d debug-level] [-c config-file] [-p project-dir] map-file
	 *		map-file is a (previously saved) map to be read in and displayed
	 *		config-file contains initial values for all parameters
	 *		project-dir is destination for exported maps
	 */
	public static void main(String[] args) {
		// process the arguments
		String filename = null;
		String configname = null;
		String project_dir = null;
		int debug = 0;
		for( int i = 0; i < args.length; i++ ) {
			if (args[i].startsWith(SWITCH_CHAR)) {	
				if (args[i].startsWith("-d")) {
					if (args[i].length() > 2)
						debug = new Integer(args[i].substring(2));
					else
						debug = new Integer(args[++i]);
				} else if (args[i].startsWith("-c")) {
					if (args[i].length() > 2)
						configname = args[i].substring(2);
					else
						configname = args[++i];
				} else if (args[i].startsWith("-p")) {
					if (args[i].length() > 2)
						project_dir = args[i].substring(2);
					else
						project_dir = args[++i];
				}else if (args[i].startsWith("-v")) {
					debug = 1;
				} else
					System.out.println(usage);
			} else {
				filename = args[i];
			}
		}
		// instantiate the parameters singletons
		parms = new Parameters(configname, debug);
		if (project_dir != null) {
			parms.project_dir = project_dir;
			new MapIndex(parms.project_dir);
		}
		
		// and create the map
		WorldBuilder w = new WorldBuilder(filename);
		
		// initialize the display type and options menus
		w.map.setDisplay(parms.display_options, true);
		w.updateDisplayMenus(parms.display_options);
	}
}
