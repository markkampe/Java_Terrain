package JsonDump;

import java.awt.FileDialog;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import javax.swing.JFrame;

public class Tester extends JFrame {
	/**
	 * This program reads in a Raw JSON worldBuilder export, and then prints:
	 *   - its basic (location, size) attributes
	 *	 - a grid of per-point altitudes
	 *   - a grid of per-point slopes
	 *   - a grid of per point face (compas) directions
	 *   - a grid of per-point rainfall
	 *   - a grid of per-point depth
	 *   - a grid of per-point soil-type
	 *	 - a grid of per-point mean (Spring) temperatures
	 */
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
		// process arguments
		String filename = null;
		boolean show_graphics = false;
		boolean show_topo = false;
		boolean show_rain = false;
		boolean show_soil = false;
		boolean show_temp = false;
		boolean flags = false;
		for(int i = 0; i < args.length; i++) {
			String a = args[i];
			if (a.charAt(0) == '-') {
				flags = true;
				for(int x = 1; x < a.length(); x++) {
					char c = a.charAt(x);
					switch (c) {
					case 'a':
						show_topo = true;
						break;
					case 'r':
						show_rain = true;
						break;
					case 's':
						show_soil = true;
						break;
					case 't':
						show_temp = true;
						break;
					case 'g':
						show_graphics = true;
						break;
					}
				}
			} else if (filename == null)
				filename = args[i];
		}
		// if no flags, send everything to stdout
		if (!flags) {
			show_topo = true;
			show_rain = true;
			show_soil = true;
			show_temp = true;
		}

		// if we didn't get an input file, prompt for one
		if (filename == null)
			filename = new Tester().filename;
		if (filename == null)
			System.exit(-1);
		MapReader r = new MapReader(filename);

		output.println("File: " + filename);
		output.println("Region: " + r.name());
		output.println("location: " + r.latitude() + ", " + r.longitude());
		output.println("size: " + r.height() + "x" + r.width() + " x " + r.tileSize() + "m");

		if (show_graphics) {
			if (show_topo)
				new TopoPreview(r).display();
			if (show_rain)
				new RainPreview(r).display();
			if (show_soil)
				new SoilPreview(r).display();
			
			// (sleazy) wait for all open windows to close
			do {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {}
			} while (PreviewMap.openWindows > 0);
			System.exit(0);
		}
		
		if (show_topo) {
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
			output.print("direction:");
			for(int row = 0; row < r.height(); row++) {
				output.println();
				output.print("    ");
				for(int col = 0; col < r.width(); col++) {
					output.print(String.format("%4d", (int) r.direction(row,col)));
				}
				output.println("");
			}

			output.println();
			output.print("depth");
			for(int row = 0; row < r.height(); row++) {
				output.println();
				output.print("    ");
				for(int col = 0; col < r.width(); col++) {
					output.print(String.format("%6.2f ", r.depth(row, col)));
				}
			}
		}

		if (show_rain) {
			output.println();
			output.print("rainfall:");
			for(int row = 0; row < r.height(); row++) {
				output.println();
				output.print("    ");
				for(int col = 0; col < r.width(); col++) {
					output.print(String.format("%3d ", r.rainfall(row, col)));
				}
			}
		}

		if (show_soil) {
			output.println();
			output.print("soil");
			for(int row = 0; row < r.height(); row++) {
				output.println();
				output.print("    ");
				for(int col = 0; col < r.width(); col++) {
					String s = r.soilType(row, col);
					output.print(s == null ? "---- " : (s.substring(0,4) + " "));
				}
			}
		}

		if (show_temp) {
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
		
		System.exit(0);
	}
}
