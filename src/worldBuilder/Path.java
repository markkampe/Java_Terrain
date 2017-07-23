package worldBuilder;

/**
 * A Path is a connection between two adjacent vertices
 * 
 * this class also maintains a list of known paths
 */
public class Path {
	public MeshPoint source;
	public MeshPoint target;
	public int index;
	
	public Path(MeshPoint p1, MeshPoint p2, int index) {	
		source = p1;
		target = p2;
		this.index = index;
	}
}
