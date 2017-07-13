
public class Main {

	public static void main(String[] args) {

		Parameters parms = Parameters.getInstance();
		
		// create a set of random points
		Mesh m = new Mesh(parms.points, parms.x_extent, parms.y_extent, parms.improvements);
	}
}
