package worldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Dialog to define the point at which an arterial river enters the map.
 */
public class RiverDialog extends JFrame implements ActionListener, ChangeListener, MapListener, WindowListener, KeyListener {

	private Map map;
	
	private Parameters parms;
	
	private JSlider flow;
	private JLabel entryPoint;
	private JButton accept;
	private JButton cancel;
	
	private double[] incoming;
	private double[] previous;
	private int whichPoint = -1;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * instantiate the widgets and register the listeners
	 */
	public RiverDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		this.incoming = map.getIncoming();
		
		// save the incoming flow before we start messing with it
		this.previous = new double[this.incoming.length];
		for(int i = 0; i < incoming.length; i++)
			previous[i] = incoming[i];
		
		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Arterial River");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");

		entryPoint = new JLabel("click an entry point");
		JLabel entryLabel = new JLabel("Point of entry");
		entryLabel.setFont(fontLarge);
		
		flow = new JSlider(JSlider.HORIZONTAL, 0, parms.tribute_max, parms.dTribute);
		flow.setMajorTickSpacing(Parameters.niceTics(0, parms.rain_max,true));
		flow.setMinorTickSpacing(Parameters.niceTics(0, parms.rain_max,false));
		flow.setFont(fontSmall);
		flow.setPaintTicks(true);
		flow.setPaintLabels(true);
		JLabel amtLabel = new JLabel("Incoming River Flow (" + Parameters.unit_f + ")", JLabel.CENTER);
		amtLabel.setFont(fontLarge);
		
		/*
		 * Pack them into:
		 * 		a horizontal grid with the position
		 * 		a vertical Box layout containing slider and buttons
		 * 		sliders are a 1x2 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		JPanel posPanel = new JPanel(new GridLayout(1,2));
		posPanel.add(entryLabel);
		posPanel.add(entryPoint);
		posPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
		
		JPanel diaPanel = new JPanel();
		diaPanel.setLayout(new BoxLayout(diaPanel, BoxLayout.PAGE_AXIS));
		diaPanel.add(amtLabel);
		diaPanel.add(flow);

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));

		diaPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		sliders.add(diaPanel);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

		mainPane.add(posPanel, BorderLayout.NORTH);
		mainPane.add(sliders);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		map.addMapListener(this);
		flow.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
		addKeyListener(this);
		map.addKeyListener(this);
		map.requestFocus();
		
		// wait for a new entry point to be selected
		map.selectMode(Map.Selection.NONE);
		map.selectMode(Map.Selection.POINT);
	}


	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		// undo any uncommitted updates
		for(int i = 0; i < incoming.length; i++)
			incoming[i] = previous[i];
		map.setIncoming(incoming);

		cancelDialog();
	}
	
	/**
	 * called when a point is selected on the map
	 * @param x		(map coordinate)
	 * @param y		(map coordinate)
	 */
	public boolean pointSelected(double x, double y) {
		if (incoming == null)
			return true;
		
		// choose the new point
		MeshPoint p = map.mesh.choosePoint(x, y);
		whichPoint = p.index;
		double lat = parms.latitude(p.x);
		double lon = parms.longitude(p.y);
		entryPoint.setText(String.format("<%.6f, %.6f>", lat, lon));
		pack();
		
		// note the change in flux
		incoming[whichPoint] = (int) flow.getValue();
		map.setIncoming(incoming);
		
		// report on the change
		if (parms.debug_level > 0) {
			System.out.println("Artial river enters at " + entryPoint.getText() + ", flow=" + (int) incoming[whichPoint] + " " + Parameters.unit_f);
			map.region_stats();
		}
		
		return true;
	}
	
	/**
	 * updates to flow slider
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == flow) {
			if (whichPoint >= 0 && incoming != null) {
				incoming[whichPoint] = (int) flow.getValue();
				map.setIncoming(incoming);
			}
			// report on the change
			if (parms.debug_level > 1) {
				System.out.println("Artial river enters at " + entryPoint.getText() + ", flow=" + (int) incoming[whichPoint] + " " + Parameters.unit_f);
				map.region_stats();
			}
		} 
	}
	
	/**
	 * unregister listener and dispose of our dialog
	 */
	private void cancelDialog() {
		map.removeMapListener(this);
		map.removeKeyListener(this);
		WorldBuilder.activeDialog = false;
		this.dispose();
	}
	
	/**
	 * ENTER means accept, ESCAPE means cancel
	 * @param e
	 */
	public void keyTyped(KeyEvent e) {
		int key = e.getKeyChar();
		if (key == KeyEvent.VK_ENTER) {
			accept();
		} else if (key == KeyEvent.VK_ESCAPE) {
			undo();
			map.setIncoming(incoming);
		}
	}
	
	/**
	 * make the newly selected entry points official
	 */
	private void accept() {
		for(int i = 0; i < incoming.length; i++)
			previous[i] = incoming[i];
		parms.dTribute = (int) flow.getValue();
	}
	
	/**
	 * back out any uncommitted entry points
	 */
	private void undo() {
		for(int i = 0; i < incoming.length; i++)
			incoming[i] = previous[i];
		map.setIncoming(incoming);
	}
	
	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel && whichPoint >= 0) {
			undo();
			cancelDialog();
		} else if (e.getSource() == accept) {
			accept();
		}
	}
	
	/** (perfunctory) */ public boolean groupSelected(boolean[] selected, boolean complete) { return false; }
	/** (perfunctory) */ public boolean regionSelected(double x, double y, double w, double h, boolean c) {return false;}
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
	/** (perfunctory) */ public void mousePressed(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseReleased(MouseEvent arg0) {}
	/** (perfunctory) */ public void keyPressed(KeyEvent arg0) {}
	/** (perfunctory) */ public void keyReleased(KeyEvent arg0) {}
}
