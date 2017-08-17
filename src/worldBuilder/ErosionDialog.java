package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ErosionDialog extends JFrame implements ActionListener, ChangeListener, WindowListener {
	private Map map;
	private double[] oldErode;	// per MeshPoint deltaZ
	private double[] newErode;	// edited per MeshPoint deltaZ
	
	private Parameters parms;
	
	private JSlider amount;
	private JButton accept;
	private JButton cancel;
	
	private static final int DIALOG_OFFSET = 6;
	
	private static final long serialVersionUID = 1L;
	
	public ErosionDialog(Map map)  {
		// pick up references
		this.map = map;
		this.oldErode = map.getErodeMap();
		this.parms = Parameters.getInstance();
		
		// copy current map
		this.newErode = new double[oldErode.length];
		for(int i = 0; i < oldErode.length; i++)
			newErode[i] = oldErode[i];
		map.setErodeMap(newErode);

		// create the dialog box
		Container mainPane = getContentPane();
		int border = parms.dialogBorder;
		((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(border, border, border, border, Color.LIGHT_GRAY));
		setTitle("Erosion/Sedimentation");
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
		JLabel amtLabel = new JLabel("Erosion(cycles)", JLabel.CENTER);
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
		setLocation(parms.dialogDX + DIALOG_OFFSET * parms.dialogDelta, parms.dialogDY + DIALOG_OFFSET * parms.dialogDelta);
		setVisible(true);
		
		// add the action listeners
		amount.addChangeListener(this);
		accept.addActionListener(this);
		cancel.addActionListener(this);
	}
	
	private void erode(int cycles) {
		
		// zero out the erosion map
		for(int i = 0; i < newErode.length; i++)
			newErode[i] = 0;
		
		for(int c = 0; c < cycles; c++) {
			// re-sort by height
			// recompute downhill
			// for each point
				// if eroding
				//		erode
				//		update the load
				// else if depositing
				//		deposit
				//		update the load
		}
	}
	
	/**
	 * Window Close event handler ... implicit CANCEL
	 */
	public void windowClosing(WindowEvent e) {
		map.selectNone();
		if (oldErode != null) {
			map.setErodeMap(oldErode);
			map.repaint();
			oldErode = null;
		}
		this.dispose();
	}
	
	/**
	 * updates to the axis/inclination/profile sliders
	 */
	public void stateChanged(ChangeEvent e) {
			if (e.getSource() == amount) {
				erode(amount.getValue());
			} 
	}

	/**
	 * click events on ACCEPT/CANCEL buttons
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == cancel) {
			// revert to previous Erosion map
			map.setErodeMap(oldErode);
			map.repaint();
			oldErode = null;
		} else if (e.getSource() == accept) {
			// make the new parameters official
			parms.dErosion = amount.getValue();
			
			if (parms.debug_level > 0)
				System.out.println("Erode: " + amount.getValue() + " cycles");
			// we no longer need the old map
			oldErode = null;
		}
		
		// clean up the graphics
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
}
