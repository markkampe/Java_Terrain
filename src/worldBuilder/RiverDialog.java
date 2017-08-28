package worldBuilder;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

public class RiverDialog extends JFrame implements ActionListener, ChangeListener, MouseListener, WindowListener {

	private Map map;
	
	private Parameters parms;
	
	private JSlider flow;
	private JLabel entryPoint;
	private JButton accept;
	private JButton cancel;
	
	private MeshPoint oldArtery, newArtery;
	private double oldFlow, newFlow;
	
	private static final int DIALOG_OFFSET = 8;
	
	private static final long serialVersionUID = 1L;
	
	public RiverDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		
		// note the incoming river at entry
		oldArtery = map.getArtery();
		oldFlow = map.getArterial();
		newArtery = oldArtery;
		newFlow = oldFlow;
		
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
		setLocation(parms.dialogDX + DIALOG_OFFSET * parms.dialogDelta, parms.dialogDY + DIALOG_OFFSET * parms.dialogDelta);
		setVisible(true);
		
		// add the action listeners
		map.addMouseListener(this);
		flow.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
	}


	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.setArtery(oldArtery,  oldFlow);
		map.removeMouseListener(this);
		this.dispose();
	}
	
	public void mouseClicked(MouseEvent e) {
		newArtery = map.choosePoint(e.getX(), e.getY());
		double lat = parms.latitude(newArtery.x);
		double lon = parms.longitude(newArtery.y);
		entryPoint.setText(String.format("<%.6f, %.6f>", lat, lon));
		pack();
		map.setArtery(newArtery,  newFlow);
	}
	
	/**
	 * updates to the axis/inclination/profile sliders
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == flow) {
			// get the flow
			newFlow = (int) flow.getValue();
			if (newFlow == 0)
				newFlow = 1;
			map.setArtery(newArtery,  newFlow);
		} 
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel || newArtery == null) {
			map.setArtery(oldArtery,  oldFlow);
			map.repaint();
		} else if (e.getSource() == accept) {
			// make the new parameters official
			parms.dTribute = (int) newFlow;
			if (parms.debug_level > 0)
				System.out.println("Artial river enters at " + entryPoint.getText() + ", flow=" + (int) newFlow + " " + Parameters.unit_f);
		}
		
		// clean up the graphics
		map.removeMouseListener(this);
		this.dispose();
	}
	
	// perfunctory methods
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
