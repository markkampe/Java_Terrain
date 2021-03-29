package worldBuilder;

import java.awt.*;
import javax.swing.*;

/**
 * a JFrame that displays a legend for map colors
 */
public class LegendDisplay extends JFrame {
		private static final int BORDER_WIDTH = 5;
		private static final int V_GAP = 5;
		private static final int H_GAP = 5;
		
		private static final long serialVersionUID = 1L;
		
		/** instantiate the legend display	*/
		public LegendDisplay(String title, Color[] colors, String[] names)  {
			// create the dialog box
			Container mainPane = getContentPane();
			((JComponent) mainPane).setBorder(BorderFactory.createMatteBorder(BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, BORDER_WIDTH, Color.LIGHT_GRAY));
			
			// count the valid entries
			int entries = 0;
			for(int i = 0; i < names.length; i++)
				if (names[i] != null)
					entries++;
			
			// create and title the layout
			JPanel info = new JPanel(new GridLayout(entries+2, 1, H_GAP, V_GAP));
			info.setBorder(BorderFactory.createEmptyBorder(20,10,20,10));
			info.add(new JLabel("Legend"));
			info.add(new JLabel("(" + title + " colors)"));
			
			for(int i = 0; i < names.length; i++)
				if (names[i] != null) {
					Button button = new Button(names[i]);
					button.setBackground(colors[i]);
					info.add(button);
				}

			mainPane.add(info,  BorderLayout.CENTER);
			pack();
			setVisible(true);
		}
}
