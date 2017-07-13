import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * A Path is a connection between two adjacent vertices
 * 
 * this class also maintains a list of known paths
 */
public class Path {
	public MapPoint source;
	public MapPoint target;
	
	static public List<Path> paths;
	static public int adds;
	static public int added;
	
	public Path(MapPoint p1, MapPoint p2) {	
		source = p1;
		target = p2;
	}
	
	static Path addPath(MapPoint p1, MapPoint p2) {
		adds++;
		if (paths == null)
			paths = new LinkedList<Path>();
		
		// FIX: a hash would be better
		for (Iterator<Path> iterator = paths.iterator(); iterator.hasNext(); ) {
			Path p = iterator.next();
			if (p.source == p1 && p.target == p2)
				return p;
			if (p.source == p2 && p.target == p1)
				return p;
		}
		Path p = new Path(p1, p2);
		paths.add(p);
		added++;
		return p;
	}
}
