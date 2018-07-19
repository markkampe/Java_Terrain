package worldBuilder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.event.*;


public class WorldBuilder  extends JFrame 
						   implements ActionListener, ChangeListener,  WindowListener, ComponentListener {
	
	// default map/mesh to load on start-up
	public static final String DEFAULT_TEMPLATE = "/Templates/default_%d.json";
	
	// identification information
	private static final String version = "WorldBuilder 0.1";
	private static final String author = "Author: Mark Kampe (mark.kampe@gmail.com)";
	private static final String credit = "Inspired by Martin O'Leary's Uncharted Atlas terrain generator (mewo2.com)";
	private static final String license = "";	// TBD
	private static final String usage = "Usage: cmd [-v] [-d debuglevel] [-c configfile] [-p projectdir] [mesh file]";
	
	// active mouse-hogging dialog (for serialization)
	public static boolean activeDialog;
	
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
	private JMenuItem fileExit;
	private JMenuItem editWorld;
	private JMenuItem editMountain;
	private JMenuItem editSlope;
	private JMenuItem editRiver;
	private JMenuItem editRain;
	private JMenuItem editErode;
	private JMenuItem editCity;
	private JMenuItem editRoads;
	private JMenuItem viewMesh;
	private JMenuItem viewTopo;
	private JMenuItem viewRain;
	private JMenuItem viewWater;
	private JMenuItem viewErode;
	private JMenuItem viewSoil;
	private JMenuItem viewZoom;
	private JMenuItem viewDebug;
	private JMenuItem helpInfo;
	private JMenuItem ruleDebug;
	private JMenuItem debug0;
	private JMenuItem debug1;
	private JMenuItem debug2;
	private JMenuItem debug3;
	
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
	 * create the hierarchy of menus that will drive most 
	 * of our actions
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
		JMenu fileExport = new JMenu("Export");
		fileExport.add(exportRaw);
		JMenu exportRPGM = new JMenu("RPGMaker");
		exportRPGM.add(exportRpgmOverworld);
		exportRPGM.add(exportRpgmOutside);
		fileExport.add(exportRPGM);
		
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
		editWorld = new JMenuItem("map scale/location");
		editWorld.addActionListener(this);
		editMountain = new JMenuItem("add mountain(s)");
		editMountain.addActionListener(this);
		editSlope = new JMenuItem("define slope");
		editSlope.addActionListener(this);
		editRain = new JMenuItem("define rainfall");
		editRain.addActionListener(this);
		editRiver = new JMenuItem("add arterial river");
		editRiver.addActionListener(this);
		editErode = new JMenuItem("erosion/deposition");
		editErode.addActionListener(this);
		editCity = new JMenuItem("add city");
		editCity.addActionListener(this);
		editCity.setEnabled(false);	// TODO implement city creation
		editRoads = new JMenuItem("draw roads");
		editRoads.addActionListener(this);
		editRoads.setEnabled(false);	// TODO implement city creation
		JMenu editMenu = new JMenu("Edit");
		editMenu.add(editWorld);
		editMenu.add(editSlope);
		editMenu.add(editMountain);
		editMenu.add(editRiver);
		editMenu.add(editRain);
		editMenu.add(editErode);
		editMenu.add(new JSeparator());
		editMenu.add(editCity);
		editMenu.add(editRoads);
		
		// create our view menu
//		viewPoints = new JMenuItem("Points");
//		viewPoints.addActionListener(this);
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
		viewSoil = new JMenuItem("Soil");
		viewSoil.addActionListener(this);
		viewZoom = new JMenuItem("Zoom");
		viewZoom.addActionListener(this);
		viewDebug = new JMenuItem("Point Details");
		viewDebug.addActionListener(this);
		JMenu viewMenu = new JMenu("View");
//		viewMenu.add(viewPoints);
		viewMenu.add(viewMesh);
		viewMenu.add(viewTopo);
		viewMenu.add(viewRain);
		viewMenu.add(viewWater);
		viewMenu.add(viewErode);
		viewMenu.add(viewSoil);
		viewMenu.add(viewZoom);
		viewMenu.add(new JSeparator());
		viewMenu.add(viewDebug);
		
		// create help menu
		helpInfo = new JMenuItem("about WorldBuilder");
		debug0 = new JMenuItem("0");
		debug1 = new JMenuItem("1");
		debug2 = new JMenuItem("2");
		debug3 = new JMenuItem("3");
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
		helpInfo.addActionListener(this);
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(helpInfo);
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
	 * create a panel of control widgets
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
	 */
	private void doSave(String filename) {
		String title = (filename == null) ? "Save As" : "Save";
		FileDialog d = new FileDialog(this, title, FileDialog.SAVE);
		if (filename == null)
			filename = parms.map_name + ".json";
		d.setFile(filename);
		d.setVisible(true);
		filename = d.getFile();
		if (filename != null) {
			String dir = d.getDirectory();
			if (dir != null)
				filename = dir + filename;
			map.write(filename);
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

	/**
	 * enable/disable menus according to sub-region status
	 */
	private void menuEnable(boolean isSubRegion) {
		editRain.setEnabled(!isSubRegion);
		editSlope.setEnabled(!isSubRegion);
		editRiver.setEnabled(!isSubRegion);
		seaLevel.setEnabled(!isSubRegion);
	}
	
	public void actionPerformed( ActionEvent e ) {
		Object o = e.getSource();
		// file menu opens, closes, saves, and exports files
		if (o == newMesh) {
			if (modified)
				checkSave();
			filename = null;
			placeDialog(new MeshDialog(map), true);
			menuEnable(false);
			modified = true;
		} else if (o == newRegion) {
			if (modified)
				checkSave();
			placeDialog(new RegionDialog(map), false);
			filename = null;
			menuEnable(true);
		} else if (o == fileOpen) {
			if (modified)
				checkSave();
			FileDialog d = new FileDialog(this, "Choose input file", FileDialog.LOAD);
			d.setVisible(true);
			filename = d.getFile();
			if (filename != null) {
				String dir = d.getDirectory();
				if (dir != null)
					filename = dir + filename;
				map.read(filename);
	
				// newly loaded map may have changed the sea-level
				seaLevel.setValue((int)(parms.sea_level * parms.z_range));
				menuEnable(map.isSubRegion);
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
				placeDialog(new ExportDialog(map), false);
				activeDialog = true;
			} 
		} else if (o == exportRpgmOverworld) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new ExportDialog(map), false);
				activeDialog = true;
			} 
		} else if (o == exportRpgmOutside) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new ExportDialog(map), false);
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
		} else if (o == editSlope) {
			placeDialog(new SlopeDialog(map), false);
		} else if (o == editRain) {
			placeDialog(new RainDialog(map), false);
		} else if (o == editRiver) {
			if (activeDialog)
				twoDialogError();
			else {
				placeDialog(new RiverDialog(map), false);
				activeDialog = true;
			}
		} else if (o == editErode) {
			placeDialog(new ErosionDialog(map), true);
		} else if (o == editCity) {
			System.out.println("implement edit:City");
		} else if (o == editRoads) {
			System.out.println("implement edit:Roads");
		}
		
		// view menu toggles individual views on and off
//		else if (o == viewPoints)
//			parms.display_options = map.setDisplay(Map.SHOW_POINTS, (parms.display_options & Map.SHOW_POINTS) == 0);
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
		else if (o == viewSoil)
			parms.display_options = map.setDisplay(Map.SHOW_SOIL, (parms.display_options & Map.SHOW_SOIL) == 0);
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
		// viewPoints.setText( (opts & Map.SHOW_POINTS) != 0 ? "~points" : "Points");
		viewMesh.setText( (opts & Map.SHOW_MESH) != 0 ? "~mesh" : "Mesh");
		viewTopo.setText( (opts & Map.SHOW_TOPO) != 0 ? "~topo" : "Topo");
		viewRain.setText( (opts & Map.SHOW_RAIN) != 0 ? "~rain" : "Rain");
		viewWater.setText( (opts & Map.SHOW_WATER) != 0 ? "~water" : "Water");
		viewErode.setText( (opts & Map.SHOW_ERODE) != 0 ? "~erosion" : "Erosion");
		viewSoil.setText( (opts & Map.SHOW_SOIL) != 0 ? "~soil" : "Soil");
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
	public void componentResized(ComponentEvent e) {}

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
