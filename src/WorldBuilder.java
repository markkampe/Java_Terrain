import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;


public class WorldBuilder  extends JFrame 
						   implements ActionListener, ChangeListener, 
						   MouseListener, MouseMotionListener,
						   WindowListener {
	
	// 2D canvas
	private Container mainPane;
	private Map map;	
	
	private int selectX;	// start of select
	private int selectY;	// start of select
	
	// view status
	private boolean viewing_points = false;
	private boolean viewing_mesh = false;
	private boolean viewing_topo = false;
	private boolean viewing_rain = false;
	private boolean viewing_water = false;
	private boolean viewing_soil = false;
	
	// menu items
	private JMenuItem fileOpen;
	private JMenuItem fileSave;
	private JMenuItem fileSaveAs;
	private JMenuItem fileClose;
	private JMenuItem fileExit;
	private JMenuItem editHeight;
	private JMenuItem editRain;
	private JMenuItem editErode;
	private JMenuItem editSealevel;
	private JMenuItem viewPoints;
	private JMenuItem viewMesh;
	private JMenuItem viewTopo;
	private JMenuItem viewRain;
	private JMenuItem viewWater;
	private JMenuItem viewSoil;
	
	// controls
	private JButton button1;
	private JButton button2;
	private JComboBox combo1;
	private JSlider slider1;
	private JSlider slider2;
	
	
	private static Parameters parms;						// global program parameters
	
	private static final int BORDER_WIDTH = 10;
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
		map = new Map(parms.width, parms.height, Color.BLACK);
		mainPane.add(map, BorderLayout.CENTER);
		map.addMouseListener(this);
		map.addMouseMotionListener(this);
		
		// create the menus and widgets
		createMenus();
		createWidgets();
		
		// validate widgets and display
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
		fileExit = new JMenuItem("Exit");
		fileExit.addActionListener(this);
		JMenu fileMenu = new JMenu("File");
		fileMenu.add(fileOpen);
		fileMenu.add(fileSave);
		fileMenu.add(fileSaveAs);
		fileMenu.add(fileClose);
		fileMenu.add( new JSeparator() );
		fileMenu.add(fileExit);
		
		// create our edit menu
		editHeight = new JMenuItem("Height");
		editHeight.addActionListener(this);
		editRain = new JMenuItem("Rain");
		editRain.addActionListener(this);
		editErode = new JMenuItem("erosion");
		editErode.addActionListener(this);
		editSealevel = new JMenuItem("sea level");
		editSealevel.addActionListener(this);
		JMenu editMenu = new JMenu("Edit");
		editMenu.add(editHeight);
		editMenu.add(editRain);
		editMenu.add(editErode);
		editMenu.add(editSealevel);
		
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
		
		// assemble the menu bar
		JMenuBar menuBar = new JMenuBar();
		menuBar.add(fileMenu);
		menuBar.add( editMenu );
		menuBar.add( viewMenu );
		setJMenuBar( menuBar );	
	}
	
	/**
	 * create a panel of control widgets
	 */
	private void createWidgets() {
		// create some buttons in panel #1
		button1 = new JButton("Button 1");
		button2 = new JButton("Button 2");
		JPanel buttons = new JPanel();	// horizontal, no-stretch
		buttons.add(button1);
		buttons.add(button2);
		
		// create a combobox in panel #2
		combo1 = new JComboBox();
		combo1.addItem("choice #1");
		combo1.addItem("choice #2");
		combo1.addItem("choice #3");
		JPanel comboPanel = new JPanel(new GridLayout(2,1));
		comboPanel.add(combo1);
		comboPanel.add(new JLabel("combo box"));
		JPanel combos = new JPanel();	// no stretch
		combos.add(comboPanel);
		
		// create some sliders in panel #3
		slider1 = new JSlider();
		slider2 = new JSlider();
		JPanel sliderPanel = new JPanel(new GridLayout(2,2));
		sliderPanel.add(slider1);
		sliderPanel.add(slider2);
		sliderPanel.add(new JLabel("slider #1"));
		sliderPanel.add(new JLabel("slider #2"));
		JPanel sliders = new JPanel();	// no stretch
		sliders.add(sliderPanel);
		
		// add those three panels to a control panel
		JPanel controls = new JPanel(new GridLayout(3,1));
		controls.add(buttons);
		controls.add(combos);
		controls.add(sliders);
		add(controls, BorderLayout.SOUTH);
		
		// register the action listeners
		button1.addActionListener(this);
		button2.addActionListener(this);
		combo1.addActionListener(this);;
		slider1.addChangeListener(this);
		slider2.addChangeListener(this);
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
		} else if (o == fileExit) {
			shutdown();
		} 
		
		// edit menus pop up the corresponding dialogs
		else if (o == editHeight) {
			System.out.println("implement edit:Height");
		} else if (o == editRain) {
			System.out.println("implement edit:Rain");
		} else if (o == editErode) {
			System.out.println("implement edit:Erode");
		} 
		
		// view menu toggles views on and off
		else if (o == viewPoints) {
			viewing_points = map.setDisplay(Map.POINTS, !viewing_points);
			viewPoints.setText(viewing_points ? "~points" : "Points");
		} else if (o == viewMesh) {
			viewing_mesh = map.setDisplay(Map.MESH, !viewing_mesh);
			viewMesh.setText(viewing_mesh ? "~mesh" : "Mesh");
		} else if (o == viewTopo) {
			viewing_topo = map.setDisplay(Map.TOPO, !viewing_topo);
			viewMesh.setText(viewing_mesh ? "~topo" : "Topo");
		} else if (o == viewRain) {
			viewing_rain = map.setDisplay(Map.RAIN, !viewing_rain);
			viewMesh.setText(viewing_mesh ? "~rain" : "Rain");
		} else if (o == viewWater) {
			viewing_water = map.setDisplay(Map.WATER, !viewing_water);
			viewMesh.setText(viewing_mesh ? "~water" : "Water");
		} else if (o == viewSoil) {
			viewing_soil = map.setDisplay(Map.SOIL, !viewing_soil);
			viewMesh.setText(viewing_mesh ? "~soil" : "Soil");
		} 
		
		// buttons are currently dummies
		else if (o == button1) {
			System.out.println("button1 pressed");		
		} else if (o == button2) {
				System.out.println("button2 pressed");
		} else if (o == combo1) {
				System.out.println("Selected " + combo1.getSelectedItem());
		}
	}
	
	public void stateChanged(ChangeEvent e) {
		Object o = e.getSource();
		
		if (o == slider1) {
			System.out.println("slider 1 changed to " + slider1.getValue());
		} else if (o == slider2) {
			System.out.println("slider 2 changed to " + slider2.getValue());
		}
	}
	
	/**
	 * note the start of a selection operation
	 */
	public void mousePressed(MouseEvent e) {
		selectX = e.getX();
		selectY = e.getY();
	}
	
	/**
	 * area selection
	 */
	public void mouseDragged(MouseEvent e) {
		int dx = e.getX() - selectX;
		int dy = e.getY() - selectY;
		map.select(selectX, selectY, dx, dy);
	}
	
	/**
	 * Note the end of a selection operation
	 */
	public void mouseReleased(MouseEvent e) {
		// take down the selection rectangle
		map.select(0, 0,  0,  0);
	}
	



	void shutdown() {
		// TODO prompt to save
		// TODO have an exit code
		System.exit(0);
	}

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent arg0) {}
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
		w.viewing_mesh = w.map.setDisplay(Map.MESH, true);
		w.viewMesh.setText("~mesh");
	}




}
