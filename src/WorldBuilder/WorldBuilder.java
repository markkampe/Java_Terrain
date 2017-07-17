package WorldBuilder;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


public class WorldBuilder  extends JFrame 
						   implements ActionListener, ChangeListener,  WindowListener {
	
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
	private JMenuItem fileOpen;
	private JMenuItem fileSave;
	private JMenuItem fileSaveAs;
	private JMenuItem fileClose;
	private JMenuItem fileExport;
	private JMenuItem fileExit;
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
	private JMenuItem helpInfo;
	
	// control widgets
	private JSlider rainfall;
	private JSlider erosion;
	private JSlider seaLevel;

	// messages
	private static String infoMessage = "WorldBuilder 0.1\nBased on Martin O'Leary's Uncharted Atlas terrain generator (mewo2.com)";
	
	// configuration
	private static Parameters parms;						// global program parameters
	private static final int BORDER_WIDTH = 10;				// window border
	private static final String ICON_IMAGE = "images/world-32.png";
	private static final String SWITCH_CHAR = "-";			// command line switches
	private static final long serialVersionUID = 0xdeadbeef;	// this is stupid
	
	/**
	 * instantiate a map and control panel
	 */
	public WorldBuilder() {
		
		// set our window icon
		//Image myIcon = getToolkit().getImage(getClass().getResource(ICON_IMAGE));
		//setIconImage(myIcon);
		
		// get a handle on our display window
		mainPane = getContentPane();
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
		setTitle("World Builder");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the main map panel, capture mouse events within it
		map = new Map(parms.width, parms.height);
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
		fileMenu.add(fileOpen);
		fileMenu.add(fileSave);
		fileMenu.add(fileSaveAs);
		fileMenu.add(fileClose);
		fileMenu.add(fileExport);
		fileMenu.add( new JSeparator() );
		fileMenu.add(fileExit);
		
		// create our edit menu
		editMountain = new JMenuItem("add mountain");
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
		JMenu viewMenu = new JMenu("View");
		viewMenu.add(viewPoints);
		viewMenu.add(viewMesh);
		viewMenu.add(viewTopo);
		viewMenu.add(viewRain);
		viewMenu.add(viewWater);
		viewMenu.add(viewSoil);
		
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
		// create some buttons in panel #1
		// button1 = new JButton("Button 1");
		// button2 = new JButton("Button 2");
		// JPanel buttons = new JPanel();	// horizontal, no-stretch
		// buttons.add(button1);
		// buttons.add(button2);
		
		// create a combobox in panel #2
		// combo1 = new JComboBox();
		// combo1.addItem("choice #1");
		// combo1.addItem("choice #2");
		// combo1.addItem("choice #3");
		// JPanel comboPanel = new JPanel(new GridLayout(2,1));
		// comboPanel.add(combo1);
		// comboPanel.add(new JLabel("combo box"));
		// JPanel combos = new JPanel();	// no stretch
		// combos.add(comboPanel);
		
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
		// button1.addActionListener(this);
		// button2.addActionListener(this);
		// combo1.addActionListener(this);;
		rainfall.addChangeListener(this);
		erosion.addChangeListener(this);
		seaLevel.addChangeListener(this);
	}
	
	public void actionPerformed( ActionEvent e ) {
		Object o = e.getSource();
		
		if (o == fileOpen) {
			System.out.println("implement file:open");

		} else if (o == fileSave) {
			System.out.println("implement file:Save");
		} else if (o == fileSaveAs) {
			System.out.println("implement file:SaveAs");
		} else if (o == fileClose) {
			System.out.println("implement file:Close");
		} else if (o == fileExport) {
			System.out.println("implement file:Export");
		} else if (o == fileExit) {
			shutdown();
		} 
		
		// edit menus pop up the corresponding dialogs
		else if (o == editMountain) {
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
		}
		
		// help menu just shows info
		else if (o == helpInfo) {
			JOptionPane.showMessageDialog(new JFrame(), infoMessage, "Information", JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
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
	

	void shutdown() {
		// TODO prompt to save
		// TODO have an exit code
		System.exit(0);
	}

	public void windowClosing(WindowEvent e) { shutdown(); }	
	public void windowActivated(WindowEvent arg0) {	}
	public void windowClosed(WindowEvent arg0) {}
	public void windowDeactivated(WindowEvent arg0) {}
	public void windowDeiconified(WindowEvent arg0) {}
	public void windowIconified(WindowEvent arg0) {}
	public void windowOpened(WindowEvent arg0) {}

	public static void main(String[] args) {
		// instantiate a parameters singleton
		parms = Parameters.getInstance();
		
		// process the arguments
		for( int i = 0; i < args.length; i++ ) {
			if (args[i].startsWith(SWITCH_CHAR)) {	
				parms.parseSwitch( args[i].substring(1));
			} else {
				String fileName = args[i];		// Do I have non-switch args?
			}
		}
		
		// create our display
		WorldBuilder w = new WorldBuilder();
		
		// create and display an initial mesh
		Mesh m = new Mesh();
		w.map.setMesh(m);
		w.viewing_mesh = w.map.setDisplay(Map.SHOW_MESH, true);
		w.viewMesh.setText("~mesh");
	}
}
