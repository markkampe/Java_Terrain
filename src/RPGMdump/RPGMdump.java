package RPGMdump;

import java.awt.FileDialog;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import javax.swing.JFrame;

public class RPGMdump extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public String filename;
	private static PrintStream output = System.out;
	
	/**
	 * if no arguments are given, put up dialogs for input and output files
	 */
	public RPGMdump() throws FileNotFoundException {
		FileDialog d = new FileDialog(this, "Select RPGMaker map to load", FileDialog.LOAD);
		d.setFile(filename == null ? "MAP001.json" : filename);
		d.setVisible(true);
		filename = d.getFile();
		if (filename != null) {
			String dir = d.getDirectory();
			if (dir != null)
				filename = dir + filename;
		}
		
		d = new FileDialog(this, "Select output file", FileDialog.LOAD);
		d.setVisible(true);
		String outfile = d.getFile();
		if (outfile != null) {
			String dir = d.getDirectory();
			output = new PrintStream((dir==null) ? outfile : dir + outfile);
		}
	}
	
	private static String SWITCH_CHAR = "-";
	private static final String usage = "RPGMdump [-l level] [-w width] [-s] [inputfile]";
	/**
	 * main: process arguments and produce output
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String filename = (args.length > 1) ? args[1] : new RPGMdump().filename;
		if (filename == null)
			System.exit(-1);
		
		int level = 0;
		int width = 4;
		boolean suppress = false;
		for( int i = 0; i < args.length; i++ ) {
			// eclipse does not honor argv[0] == command
			if (i == 0 && !args[i].startsWith(SWITCH_CHAR))
				continue;
			
			if (args[i].startsWith(SWITCH_CHAR)) {	
				if (args[i].startsWith("-l")) {
					if (args[i].length() > 2)
						level = new Integer(args[i].substring(2));
					else
						level = new Integer(args[++i]);
				} else if (args[i].startsWith("-w")) {
					if (args[i].length() > 2)
						width = new Integer(args[i].substring(2));
					else
						width = new Integer(args[++i]);
				} else if (args[i].startsWith("-s")) {
					suppress = true;
				} else
					System.out.println(usage);
			} else {
				filename = args[i];
			}
		}
		RPGMreader r = new RPGMreader(filename);

		// summary information
		output.println("map file: " + filename);
		int rows = r.height();
		int cols = r.width();
		output.println("map size: " + rows + "x" + cols);
		output.println("tile set: " + r.tileSet());
		
		// level dumps
		String format = "%" + width + "d";
		String blanks = "%" + width + "s";
		for(int l = 1; l <= 6; l++) {
			if (level > 0 && l != level)
				continue;
			output.println("\nLevel " + l + ":");
			
			int[][] thislevel = r.getLevel(l);
			for(int row = 0; row < rows; row++) {
				for(int col = 0; col < cols; col++) {
					int v = thislevel[row][col];
					if (suppress && v == 0)
						output.print(String.format(blanks, ""));
					else
						output.print(String.format(format, v));
				}
				output.println();
			}
		}
	}		
}
