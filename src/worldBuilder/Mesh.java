package worldBuilder;

// note: this is the only class that knows about Voronoi
import org.rogach.jopenvoronoi.Edge;
import org.rogach.jopenvoronoi.Face;
import org.rogach.jopenvoronoi.HalfEdgeDiagram;
import org.rogach.jopenvoronoi.Point;
import org.rogach.jopenvoronoi.PointSite;
import org.rogach.jopenvoronoi.Vertex;
import org.rogach.jopenvoronoi.VertexType;
import org.rogach.jopenvoronoi.VoronoiDiagram;

/**
 * @module mesh ... functions to generate the basic map
 *
 *  A mesh is a set of irregularly spaced interconnected points:
 *      all <x,y,z> coordinates are relative to the center
 *      all <x,y,z> coordinates fit within a unit cube (-0.5 to +0.5)
 *  	operations on the mesh are agnostic to real-world size 
 *  
 *  Once created, the mesh (collection of connected points) does not
 *  change, but the attributes of the individual points (e.g. altitude
 *  and hydration) do evolve over time).
 *  
 *  The intent is that all other maps, even though much more detailed
 *  than the mesh, should be deterministically derivable from the
 *  mesh and its parameters.
 *  
 *  Notes on generation:
 *         
 *     O'Leary observed that a map created on a square grid never loses its
 *     regularity, so he wanted to build the map on an irregular grid. But
 *     he found randomly chosen grids to be too irregular. Mesh generation
 *     implements his compromise.
 *
 *         1. He starts by generating N completely randomly chosen points. But
 *         these turn out to be a little to clumpy, so he smoothes them out
 *         (improves them) by finding the Voronoi polygons around those points
 *         and using their vertices.
 *
 *         2. He uses those (improved) points as the centers for a second
 *         Voronoi tesselation, whose vertices become the map points, and
 *         whose edges become a connected mesh (each internal point has
 *         three neighbors).
 */
public class Mesh {
	public MeshPoint[] vertices;	// grid vertices	
	public Path[] edges;			// mesh connections
	
	private Parameters parms;		// global options
	
	/**
	 * create an initial set of points
	 * 
	 */
	public Mesh() {
		parms = Parameters.getInstance();
	}
	
	/**
	 * create a copy of an existing mesh
	 * 		with copies of all of the MapPoints and Paths
	 * 		so that we can change them w/o affecting original
	 * 
	 * @param	Mesh to copy
	 */
	public Mesh( Mesh m ) {
		parms = Parameters.getInstance();
		
		// create an entirely new list of mapPoints
		vertices = new MeshPoint[m.vertices.length];
		for(int i = 0; i < m.vertices.length; i++) {
			MeshPoint p = m.vertices[i];
			vertices[i] = new MeshPoint(p.x, p.y, i);
			vertices[i].z = p.z;
		}
		
		// create a corresponding list of edges
		edges = new Path[m.edges.length];
		for(int i = 0; i < m.edges.length; i++) {
			Path p = m.edges[i];
			edges[i] = new Path(vertices[p.source.index], vertices[p.target.index], i);
		}
	}
	
	/**
	 * create a new mesh
	 */
	public void create() {
		// create a set of random points
		MeshPoint points[] = new MeshPoint[parms.points];
		for (int i = 0; i < points.length; i++) {
			double x = parms.x_extent * (Math.random() - 0.5);
			double y = parms.y_extent * (Math.random() - 0.5);
			points[i] = new MeshPoint(x, y);
		}
			
		// even out the distribution
		for( int i = 0; i < parms.improvements; i++ )
			points = improve(points);
		
		// create a Voronoi mesh around the improved points
		makeMesh(points);
	}
	
	/**
	 * read mesh of MapPoints from a file
	 */
	public void read(String filename) {
		System.out.println("TODO: Implement Mesh.read(" + filename + ")");
		vertices = new MeshPoint[0];
		edges = new Path[0];
		// TODO implement Mesh:read
	}
	
	/**
	 * write a mesh of MapPoints out to a file
	 */
	public void write(String filename) {
		System.out.println("TODO: Implement Mesh.write(" + filename + ")");
		// TODO implement Mesh:write
	}
	

	public void export(String filename, double x, double y, double dx, double dy, int meters) {
		System.out.println("TODO: Implement Mesh.export to file " + filename + ", <" + x + "," + y + ">, " + dx + "x" + dy + ", grain=" + meters + "m");
		// TODO implement Mesh:export ... maybe move it to Map:export
	}
	
	/**
	 * is a point near the edge
	 * 
	 * @param point
	 */
	boolean isNearEdege(MeshPoint p) {
		return false;
	}


	/**
	 * truncate values outside the extent box to the edge of the box
	 */
	private Point truncate(Point p) {
		double x = p.x;
		if (x < -parms.x_extent/2)
			x = -parms.x_extent/2;
		else if (x > parms.x_extent/2)
			x = parms.x_extent/2;
		
		double y = p.y;
		if (y < -parms.y_extent/2)
			y = -parms.y_extent/2;
		else if (y > parms.y_extent/2)
			y = parms.y_extent/2;
		
		return new Point(x,y);
	}
	
	/**
	 * is a point within the arena
	 * 
	 * @param OpenVoronoi vertex
	 * @return if that point is within the box
	 * 
	 * 	(necessary because OpenVoronoi generates points outside the box)
	 */
	private boolean inTheBox(Point p) {
		if (p.x < -parms.x_extent/2 || p.x > parms.x_extent/2)
			return false;
		if (p.y < -parms.y_extent/2 || p.y > parms.y_extent/2)
			return false;
		return true;
	}
	
	/**
	 * even out (the spacing of) a set of random points
	 * 
	 * O'Leary did this by computing the Voronoi polygons surrounding the
	 * chosen points, and then taking the centroids of those polygons.
	 * He also sorted the list of random points (left to right).  He
	 * did not explain this, but it might have been to optimize the
	 * (later) use of the Planchon-Darboux water-level algorithm. 
	 */
	private MeshPoint[] improve( MeshPoint[] points) {
		MeshPoint newPoints[] = new MeshPoint[points.length];
		
		// sort the points (left to right)
		MeshPoint.quickSort(points, 0, points.length-1);

		// create the Voronoi tesselation
		VoronoiDiagram vd = new VoronoiDiagram();
		for (int i = 0; i < points.length; i++) {
			vd.insert_point_site(new Point(points[i].x, points[i].y));
		}
		HalfEdgeDiagram g = vd.get_graph_reference();
		
		// find the centroid of each voronoi polygon
		int i = 0;
		for (Vertex v : g.vertices) {
			// start with the initial point sites
			if (v.type != VertexType.POINTSITE)
				continue;
			
			// ignoring "infinity" points OpenVoronoi added
			if (!inTheBox(v.position))
				continue;

			// find the face that owns the point
			Face chosenFace = null;
			for (Face f: g.faces) {
				PointSite p = (PointSite) f.site;
				if (p.position() != v.position)
					continue;
				chosenFace = f;
				break;
			}
			assert(chosenFace != null);
			
			// walk the edges and average their coordinates
			double x_sum = 0;
			double y_sum = 0;
			int numPoints = 0;
			for (Edge e: g.face_edges(chosenFace)) {
				Point p = truncate(e.source.position);
				x_sum += p.x;
				y_sum += p.y;
				numPoints++;
			}
			newPoints[i++] = new MeshPoint(x_sum/numPoints, y_sum/numPoints);
			
			if (parms.debug_level > 2)
				System.out.println("initial point <" + v.position + "> -> <" + newPoints[i-1] + ">");
		}
		return(newPoints);
	}
	
	
	/**
	 * turn a set of points into a mesh
	 * 		compute the Voronoi tesselation
	 * 		for each edge (that is within the box)
	 * 			add each new end to our vertex list
	 * 			add the edge as path in/out of each vertex
	 * 
	 * NOTE: that the original points are replaced with the
	 * 		 vertices of the corresponding Voronoi polygons.
	 */
	private void makeMesh( MeshPoint[] points ) {
		// compute the Voronoi teselation of the current point set
		VoronoiDiagram vd = new VoronoiDiagram();
		for (int i = 0; i < points.length; i++) {
			vd.insert_point_site(new Point(points[i].x, points[i].y));
		}
		HalfEdgeDiagram g = vd.get_graph_reference();
		
		// allocate hash table to track known vertices
		MeshPointHasher pointhash = new MeshPointHasher(g.num_vertices(), parms.x_extent, parms.y_extent);
		PathHasher pathhash = new PathHasher(g.num_edges());

		// locate all the vertices and edges
		//	NOTE: unfortunate OpenVoronoi behavior
		//		  1. it returns not only vertices, but edge mid-points
		//		  2. it returns vertices that are outside of the box
		for( Edge e: g.edges ) {
			// ignore APEX (mid-line) points when they are sources
			if (e.source.type == VertexType.APEX)
				continue;
			Point p1 = e.source.position;
			
			// bypass APEX points when they are targets
			Point p2 = null;
			if (e.target.type == VertexType.APEX) {
				// find the target at the other end
				for(Edge e1: e.target.out_edges) {
					if (e1.target != e.source) {
						p2 = e1.target.position;
						assert(e1.target.type == VertexType.NORMAL);
					}
				}
				assert(p2 != null);
			} else
				p2 = e.target.position;
			
			// ignore paths originating outside the box
			if (!inTheBox(p1))
				continue;
			// TODO short-circuit paths ending outside the box
			if (!inTheBox(p2))
				continue;		
			
			// assign/get the vertex ID of each end
			MeshPoint mp1 = pointhash.findPoint(p1.x, p1.y);
			MeshPoint mp2 = pointhash.findPoint(p2.x, p2.y);
			
			// note that each is a neighbor of the other
			mp1.addNeighbor(mp2);
			mp2.addNeighbor(mp1);
			
			// and note the path that connects them
			pathhash.findPath(mp1, mp2);
		} 
		
		// TODO stitch together out-of-the-box paths
		
		// copy out the list of unique Vertices
		vertices = new MeshPoint[pointhash.numVertices];
		for(int i = 0; i < pointhash.numVertices; i++)
			vertices[i] = pointhash.vertices[i];
	
		// copy out the list of unique Edges
		edges = new Path[pathhash.numPaths];
		for(int i = 0; i < pathhash.numPaths; i++)
			edges[i] = pathhash.paths[i];
		
		if (parms.debug_level > 0)
			System.out.println(points.length + " points-> " + vertices.length + "/" + g.num_vertices() + 
					" mesh vertices, " + edges.length + "/" + g.num_edges() + " mesh paths");
	}
}
