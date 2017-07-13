
import java.awt.Color;

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
 *         a mesh is a triangular tessalation of the map. a mesh includes:
 *         
 *         O'Leary observed that a map created on a square grid never loses its
 *         regularity, so he wanted to build the map on an irregular grid. But
 *         he found randomly chosen grids to be too irregular. Mesh generation
 *         implements his compromise.
 *
 *         1. He starts by generating N completely randomly chosen points. But
 *         these turn out to be a little to clumpy, so he smoothes them out
 *         (improves them) by finding the Voronoi polygons around those points
 *         and using their vertices.
 *
 *         2. He uses those (improved) points as the centers for a second
 *         Voronoi tesselation, whose vertices become the map points, and
 *         whose edges become a triangular mesh.
 *
 *         NOTE: <x,y> coordinates are relative to the center of the map
 */
public class Mesh {

	private double x_extent;		// arena width
	private double y_extent;		// arena height
	private MapPoint[] vertices;	// grid vertices
	private int numVertices;		// number of vertices
	
	private Parameters parms;		// global options
	
	/**
	 * create an initial set of points
	 * 
	 * @param number of points
	 * @param x range
	 * @param y range
	 * @param number of improvement iterations
	 */
	public Mesh(int num_points, double x_extent, double y_extent, int improvements) {
		this.x_extent = x_extent;
		this.y_extent = y_extent;
		parms = Parameters.getInstance();

		// create a set of random points
		MapPoint points[] = new MapPoint[num_points];
		for (int i = 0; i < num_points; i++) {
			double x = x_extent * (Math.random() - 0.5);
			double y = y_extent * (Math.random() - 0.5);
			points[i] = new MapPoint(x, y);
		}
		
		// diagnostic display of original points
		PointsDisplay pd = parms.show_points ? new PointsDisplay("Grid Points", 800, 800, Color.BLACK) : null;
		if (pd != null)
			pd.addPoints(points, PointsDisplay.Shape.CIRCLE, Color.GRAY);
			
		// even out the distribution
		for( int i = 0; i < improvements; i++ )
			points = improve(points);
		
		// diagnostic display of improved points
		if (pd != null) {
			pd.addPoints(points, PointsDisplay.Shape.DIAMOND, Color.WHITE);
			pd.repaint();
		}
		
		// create a Voronoi mesh around the improved points
		makeMesh(points);
		if (parms.show_grid) {
			new MeshDisplay("Raw Grid", vertices, 800, 800, Color.BLACK, Color.GRAY);
		}
	}

	/**
	 * truncate values outside the extent box to the edge of the box
	 * 
	 * FIX - should interpolate rather than flatten
	 */
	private static double truncate(double value, double extent) {
		if (value < -extent/2)
			return(-extent/2);
		else if (value > extent/2)
			return(extent/2);
		else
			return(value);
	}
	
	/**
	 * is a point within the arena
	 * 
	 * @param OpenVoronoi vertex
	 * @return if that point is within the box
	 * 
	 * 	(necessary because OpenVoronoi generates points outside the box)
	 */
	private boolean inTheBox(Vertex v) {
		if (v.position.x < -x_extent/2 || v.position.x > x_extent/2)
			return false;
		if (v.position.y < -y_extent/2 || v.position.y > y_extent/2)
			return false;
		return true;
	}
	
	/**
	 * even out (the spacing of) a set of random points
	 * 
	 * O'Leary did this by computing the Voronoi polygons surrounding the
	 * chosen points, and then taking the centroids of those polygons.
	 */
	private MapPoint[] improve( MapPoint[] points) {
		MapPoint newPoints[] = new MapPoint[points.length];
		
		// sort the points (left to right)	??? WHY ???
		MapPoint.quickSort(points, 0, points.length-1);

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
			if (v.position.x > x_extent / 2 || v.position.x < -x_extent / 2 || v.position.y > y_extent / 2
					|| v.position.y < -y_extent / 2) {
				continue;
			}

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
				x_sum += truncate(e.source.position.x, x_extent);
				y_sum += truncate(e.source.position.y, y_extent);
				numPoints++;
			}
			newPoints[i++] = new MapPoint(x_sum/numPoints, y_sum/numPoints);
			// System.out.println("initial point <" + v.position + "> -> <" + newPoints[i-1] + ">");
		}
		return(newPoints);
	}
	
	/**
	 * This class is used to generate a single unique MapPoint
	 * for every vertex coordinate, and find the already 
	 * allocated point when coordinates are repeated.
	 */
	private class MapPointHasher {
		public MapPoint vertices[];	// known unique vertices
		public int numVertices;		// # unique vertices
		private int tableSize;		// size of hash table
		private int[] hashTable;	// index into vertices array
		
		/**
		 * allocate a hash table amd vertex list
		 * @param max ... max # of vertices
		 */
		public MapPointHasher(int max) {
			vertices = new MapPoint[max];
			numVertices = 0;
			tableSize = max * 3 / 2;	// hash table efficiency
			hashTable = new int[tableSize];
			for( int i = 0; i < tableSize; i++ )
				hashTable[i] = -1;
		}
		
		/**
		 * find or create reference to MapPoint(x,y)
		 * 
		 * 	We use an open hash table to note the index associated
		 *  with a particular hash value
		 * 		
		 * @param x  x coordinate
		 * @param y  y coordinate
		 * @return   associated MapPoint
		 */
		public MapPoint findPoint(double x, double y) {
			double value = ((x + x_extent/2) + (y + y_extent/2)) * tableSize;
			int guess = ((int) value) % tableSize;
			while(hashTable[guess] != -1) {
				MapPoint m = vertices[hashTable[guess]];
				if (m.x == x && m.y == y)
					return m;	// this one is already known
				guess = (guess == tableSize - 1) ? 0 : guess + 1;
			}
			// add a new MapPoint to the list
			MapPoint m = new MapPoint(x,y,numVertices);
			hashTable[guess] = numVertices;
			vertices[numVertices++] = m;
			return m;
		}
	}
	
	/**
	 * turn a set of points into a mesh
	 * 		compute the Voronoi tesselation
	 * 		for each edge (that is within the box)
	 * 			add each new end to our vertex list
	 * 			add the edge as path in/out of each vertex
	 */
	private void makeMesh( MapPoint[] points ) {
		// compute the Voronoi teselation of the current point set
		VoronoiDiagram vd = new VoronoiDiagram();
		for (int i = 0; i < points.length; i++) {
			vd.insert_point_site(new Point(points[i].x, points[i].y));
		}
		HalfEdgeDiagram g = vd.get_graph_reference();
		
		// allocate hash table to track known vertices
		int n = g.num_vertices();
		MapPointHasher hash = new MapPointHasher(n);

		// locate all the vertices and edges
		for( Edge e: g.edges ) {
			// ignore edges that are not entirely within the arena
			Vertex v1 = e.source;
			if (!inTheBox(v1))
				continue;
			Vertex v2 = e.target;
			if (!inTheBox(v2))
				continue;
			
			// assign/get the vertex ID of each end
			MapPoint p1 = hash.findPoint(v1.position.x, v1.position.y);
			MapPoint p2 = hash.findPoint(v2.position.x, v2.position.y);
			
			// note that each is a neighbor of the other
			p1.addNeighbor(p2);
			p2.addNeighbor(p1);
		
			// add this to my own list of edges
			Path.addPath(p1, p2);
		} 
		
		// copy out the list of unique vertices
		vertices = new MapPoint[hash.numVertices];
		for(int i = 0; i < hash.numVertices; i++)
			vertices[i] = hash.vertices[i];
		numVertices = hash.numVertices;
		System.out.println(points.length + " points-> " + numVertices + "/" + n + " mesh vertices");
		System.out.println("->" + Path.added + "/" + Path.adds + " paths");
	}
}
