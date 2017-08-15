package worldMaps;

public class Tester {
	public static void main(String[] args) {
		
		MapReader r = new MapReader(args[1]);
		System.out.println("File: " + args[1]);
		System.out.println("Region: " + r.name());
		System.out.println("location: " + r.latitude() + ", " + r.longitude());
		System.out.println("size: " + r.height() + "x" + r.width() + " x " + r.tileSize() + "m");
		System.out.print("altitude");
		for(int row = 0; row < r.height(); row++) {
			System.out.print("\n    ");
			for(int col = 0; col < r.width(); col++) {
				System.out.print(String.format("%4.0f ", r.altitude(row, col)));
			}
		}
		
		System.out.print("\nslope:");
		for(int row = 0; row < r.height(); row++) {
			System.out.print("\n    ");
			for(int col = 0; col < r.width(); col++) {
				System.out.print(String.format("%3.1f ", r.slope(row, col)));
			}
		}
		
		System.out.print("\nface:");
		for(int row = 0; row < r.height(); row++) {
			System.out.print("\n    ");
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
				System.out.print(d);
			}
		}
		
		System.out.print("\nrainfall:");
		for(int row = 0; row < r.height(); row++) {
			System.out.print("\n    ");
			for(int col = 0; col < r.width(); col++) {
				System.out.print(String.format("%3d ", r.rainfall(row, col)));
			}
		}
		
		System.out.print("\nhydration");
		for(int row = 0; row < r.height(); row++) {
			System.out.print("\n    ");
			for(int col = 0; col < r.width(); col++) {
				System.out.print(String.format("%3d ", r.hydration(row, col)));
			}
		}
		
		System.out.print("\nsoil");
		for(int row = 0; row < r.height(); row++) {
			System.out.print("\n    ");
			for(int col = 0; col < r.width(); col++) {
				String s = "? ";
				switch(r.soilType(row, col)) {
				case UNKNOWN:	s = "   "; break;
				case IGNEOUS:	s = "I "; break;
				case METAMORPHIC: s = "M "; break;
				case SEDIMENTARY: s = "S "; break;
				case ALLUVIAL: s = "A "; break;
				}
				System.out.print(s);
			}
		}
	}
}
