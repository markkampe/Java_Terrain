package worldBuilder;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog to control debug output for Flora-placement-rule execution
 */
public class RuleDebug extends JFrame implements WindowListener, ActionListener {
		private Parameters parms;
			
		private static final int BORDER_WIDTH = 5;
		private static final long serialVersionUID = 1L;
		
		JTextField rulename;
		JButton accept;
		JButton cancel;
		
		public RuleDebug()  {
			// pick up references
			this.parms = Parameters.getInstance();
			
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			setTitle("Rule Trace");
			addWindowListener( this );
			setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			
			JPanel info = new JPanel(new GridLayout(2,1));
			info.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
			rulename = new JTextField();
			if (parms.rule_debug != null)
				rulename.setText(parms.rule_debug);
			info.add(rulename);
			info.add(new JLabel("Name of rule to be traced"));
			mainPane.add(info,  BorderLayout.CENTER);
			
			JPanel buttons = new JPanel(new GridLayout(1,2));
			accept = new JButton("Accept");
			buttons.add(accept);
			cancel = new JButton("Cancel");
			buttons.add(cancel);
			mainPane.add(buttons, BorderLayout.SOUTH);
			
			pack();
			setVisible(true);
			
			// add the action listeners
			accept.addActionListener(this);
			cancel.addActionListener(this);
		}
		
		/**
		 * button pushes - accept or cancel
		 */
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == accept) {
				String s = rulename.getText();
				parms.rule_debug = (s == "") ? null : s;		
			} else if (e.getSource() == cancel) {
				parms.rule_debug = null;
			}
			this.dispose();
		}
		
		/**
		 * Window Close event handler ... do nothing
		 */
		public void windowClosing(WindowEvent e) {
			this.dispose();
		}

		public void windowActivated(WindowEvent arg0) {}
		public void windowClosed(WindowEvent arg0) {}
		public void windowDeactivated(WindowEvent arg0) {}
		public void windowDeiconified(WindowEvent arg0) {}
		public void windowIconified(WindowEvent arg0) {}
		public void windowOpened(WindowEvent arg0) {}
		public void mousePressed(MouseEvent arg0) {}
		public void mouseReleased(MouseEvent arg0) {}
}
