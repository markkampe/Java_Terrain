
public class Main {
	static private double x_extent = 1; // X map diameter
	static private double y_extent = 1; // Y map diameter
	static private final int POINTS = 200; // number of points

	public static void main(String[] args) {

		// create a set of random points
		Mesh m = new Mesh(POINTS, x_extent, y_extent, 1);
	}
}
