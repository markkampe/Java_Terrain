package worldMaps;

public class Tester {
	public static void main(String[] args) {
		
		MapReader r = new MapReader(args[1]);
		if (r.readMap()) {
			System.out.println("File: " + args[1]);
			System.out.println("Region: " + r.name());
			System.out.println("location: " + r.latitude() + ", " + r.longitude());
			System.out.println("size: " + r.height() + "x" + r.width() + " x " + r.tileSize() + "m");
			System.out.println("altitude");
			for(int row = 0; row < r.height(); row++) {
				System.out.print("\n    ");
				for(int col = 0; col < r.width(); col++) {
					System.out.print(r.altitude(row, col) + ",");
				}
			}
			System.out.println("rainfall");
			for(int row = 0; row < r.height(); row++) {
				System.out.print("\n    ");
				for(int col = 0; col < r.width(); col++) {
					System.out.print(r.rainfall(row, col) + ",");
				}
			}
		}
	
		r.close();
	}
}
