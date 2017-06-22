
public class Main {

	public static void main(String[] args) {
		MapPoints eight = new MapPoints(8);
		System.out.println("set = " + eight);
		
		MapPoint center = eight.centroid();
		System.out.println("center = " + center);
	}
}
