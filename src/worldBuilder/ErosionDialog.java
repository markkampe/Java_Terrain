package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 * a Dialog to control the degree of erosion/deposition
 */
public class ErosionDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {
	private Map map;
	private int oldCycles;
	
	private Parameters parms;
	
	private JSlider amount;
	private JButton accept;
	private JButton cancel;
	
	private static final long serialVersionUID = 1L;
	
	/**
	 * create the widgits and register the listeners
	 */
	public ErosionDialog(Map map)  {
		// pick up references
		this.map = map;
		this.parms = Parameters.getInstance();
		this.oldCycles = map.getErosion();

		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Erosion/Deposition");
		addWindowListener( this );
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		// create the basic widgets
		Font fontSmall = new Font("Serif", Font.ITALIC, 10);
		Font fontLarge = new Font("Serif", Font.ITALIC, 15);
		accept = new JButton("ACCEPT");
		cancel = new JButton("CANCEL");

		amount = new JSlider(JSlider.HORIZONTAL, 0, parms.erosion_max, parms.dErosion);
		amount.setMajorTickSpacing(Parameters.niceTics(0, parms.erosion_max,true));
		amount.setMinorTickSpacing(Parameters.niceTics(0, parms.erosion_max,false));
		amount.setFont(fontSmall);
		amount.setPaintTicks(true);
		amount.setPaintLabels(true);
		JLabel amtLabel = new JLabel("Erosion/Deposition(cycles)", JLabel.CENTER);
		amtLabel.setFont(fontLarge);
		
		/*
		 * Pack them into:
		 * 		a vertical Box layout containing sliders and buttons
		 * 		sliders are a 1x1 grid layout
		 * 			each being a vertical Box w/label and slider
		 * 		buttons a horizontal Box layout
		 */
		
		JPanel amtPanel = new JPanel();
		amtPanel.setLayout(new BoxLayout(amtPanel, BoxLayout.PAGE_AXIS));
		amtPanel.add(amtLabel);
		amtPanel.add(amount);
		

		JPanel sliders = new JPanel();
		sliders.setLayout(new BoxLayout(sliders, BoxLayout.LINE_AXIS));
		amtPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 0, 15));
		sliders.add(amtPanel);
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.LINE_AXIS));
		buttons.add(cancel);
		buttons.add(Box.createRigidArea(new Dimension(40,0)));
		buttons.add(accept);
		buttons.setBorder(BorderFactory.createEmptyBorder(20,100, 20, 10));

		mainPane.add(sliders);
		mainPane.add(buttons, BorderLayout.SOUTH);
		
		pack();
		setVisible(true);
		
		// add the action listeners
		amount.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectMode(Map.Selection.ANY);
		map.setErosion(oldCycles);
		map.repaint();
		this.dispose();
	}
	
	/**
	 * updates to the amount-of-erosion slider
	 */
	public void stateChanged(ChangeEvent e) {
		if (e.getSource() == amount) {
			map.setErosion(amount.getValue());
		} 
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			// revert to previous Erosion map
			map.setErosion(oldCycles);
			map.repaint();
		} else if (e.getSource() == accept) {
			// make the new parameters official
			parms.dErosion = amount.getValue();
			
			if (parms.debug_level > 0)
				System.out.println("Erode: " + amount.getValue() + " cycles" +
						", max erosion: " + String.format("%.1f", map.max_erosion) + Parameters.unit_z +
						", max deposition: " + String.format("%.1f",  map.max_deposition) + Parameters.unit_z);
		}
		
		// clean up the graphics
		this.dispose();
	}
	
	/** (perfunctory) */ public void mouseMoved(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseEntered(MouseEvent arg0) {}
	/** (perfunctory) */ public void mouseExited(MouseEvent arg0) {}
	/** (perfunctory) */ public void windowActivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowClosed(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeactivated(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowDeiconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowIconified(WindowEvent arg0) {}
	/** (perfunctory) */ public void windowOpened(WindowEvent arg0) {}
}
