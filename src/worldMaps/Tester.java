package worldMaps;

import java.awt.FileDialog;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import javax.swing.JFrame;

public class Tester extends JFrame {
	
	private static final long serialVersionUID = 1L;
	public String filename;
	private static PrintStream output = System.out;
	
	public Tester() throws FileNotFoundException {
		FileDialog d = new FileDialog(this, "Select WorldBuilder Export to load", FileDialog.LOAD);
		d.setFile(filename == null ? "Happyville.json" : filename);
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
	
	public static void main(String[] args) throws FileNotFoundException {
		String filename = (args.length > 1) ? args[1] : new Tester().filename;
		if (filename == null)
			System.exit(-1);
		MapReader r = new MapReader(filename);

		output.println("File: " + filename);
		output.println("Region: " + r.name());
		output.println("location: " + r.latitude() + ", " + r.longitude());
		output.println("size: " + r.height() + "x" + r.width() + " x " + r.tileSize() + "m");
		output.print("altitude");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				output.print(String.format("%4.0f ", r.altitude(row, col)));
			}
		}
		
		output.println();
		output.print("slope:");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				output.print(String.format("%3.1f ", r.slope(row, col)));
			}
		}
		
		output.println();
		output.print("face:");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				String d = "   ";
				switch(r.face(row, col)) {
				case NONE: d = "   "; break;
				case NORTH: d = " N "; break;
				case NORTH_WEST: d = "NW "; break;
				case NORTH_EAST: d = "NE "; break;
				case SOUTH: d = " S "; break;
				case SOUTH_WEST: d = "SW "; break;
				case SOUTH_EAST: d = "SE "; break;
				case WEST: d = " W "; break;
				case EAST: d = " E "; break;
				}
				output.print(d);
			}
		}
		
		output.println();
		output.print("rainfall:");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				output.print(String.format("%3d ", r.rainfall(row, col)));
			}
		}
		
		output.println();
		output.print("hydration");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				output.print(String.format("%6.2f ", r.hydration(row, col)));
			}
		}
		
		output.println();
		output.print("soil");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				String s = "? ";
				switch(r.soilType(row, col)) {
				case UNKNOWN:	s = "   "; break;
				case IGNEOUS:	s = "I "; break;
				case METAMORPHIC: s = "M "; break;
				case SEDIMENTARY: s = "S "; break;
				case ALLUVIAL: s = "A "; break;
				}
				output.print(s);
			}
		}
		
		output.println();
		output.print("mean temp");
		for(int row = 0; row < r.height(); row++) {
			output.println();
			output.print("    ");
			for(int col = 0; col < r.width(); col++) {
				output.print(String.format("%.1fC ", r.meanTemp(row, col, MapReader.Seasons.SPRING)));
			}
		}
	}
}
