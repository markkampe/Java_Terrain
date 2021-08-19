package worldBuilder;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import javax.json.Json;
import javax.json.stream.JsonParser;

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
 *  a set of irregularly spaced interconnected points:
 *      all (x,y,z) coordinates are relative to the center of a
 *      (-0.5 to +0.5) unit-cube.
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
	/** the MeshPoints in this Mesh */
	public MeshPoint[] vertices;
	private Parameters parms;		// global options
	
	private static final int MESH_DEBUG = 3;
	
	/**
	 * create an initial (empty) array of MeshPoints
	 */
	public Mesh() {
		parms = Parameters.getInstance();
		vertices = new MeshPoint[0];
	}
	
	/**
	 * create a set of mesh points
	 * @param numpoints in the desired mesh
	 * @param seeds list of pre-chosen points
	 * 		Pre-chosen points are used for river/route entry/exit points
	 * 		in new SubRegions, to ensure that they are at the same positions
	 * 		they were in the parent map.
	 * @return array of new MeshPoints
	 */
	public MeshPoint[] makePoints( int numpoints, ArrayList<MeshPoint> seeds ) {
		// allocate an array of the requested size
		MeshPoint points[] = new MeshPoint[numpoints];
		int first = 0;
		
		// start initializing it with seeded points
		if (seeds != null) {
			for(MeshPoint p : seeds)
				points[first++] = p;
		}
		
		// fill it with random points
		for (int i = first; i < points.length; i++) {
			double x = Parameters.x_extent * (Math.random() - 0.5);
			double y = Parameters.y_extent * (Math.random() - 0.5);
			points[i] = new MeshPoint(x, y);
		}
			
		// even out the distribution
		for( int i = 0; i < parms.improvements; i++ )
			points = improve(points);
		
		return(points);
	}
	
	/**
	 * read mesh of MapPoints from a file
	 * 
	 * @param filename of input file
	 */
	public void read(String filename) {
		JsonParser parser;
		BufferedReader r;
		if (filename == null) {
			filename = String.format(WorldBuilder.DEFAULT_TEMPLATE, parms.points);
			InputStream s = getClass().getResourceAsStream(filename);
			r = new BufferedReader(new InputStreamReader(s));
		} else {
			try {
				r = new BufferedReader(new FileReader(filename));
			} catch (FileNotFoundException e) {
				System.err.println("FATAL: unable to open input file " + filename);
				vertices = new MeshPoint[0];
				return;
			}
		}
		parser = Json.createParser(r);

		// parameters being read
		String thisKey = "";
		boolean inPoints = false;
		boolean inMesh = false;
		boolean inNeighbors = false;
		double x = 0;
		double y = 0;
		
		int length = 0;	// expected number of points
		int points = 0;	// number of points read
		int paths = 0;	// number of paths created
		
		while(parser.hasNext()) {
			JsonParser.Event e = parser.next();
			switch(e) {		
			case KEY_NAME:
				thisKey = parser.getString();
				if (length > 0 && thisKey.equals("points")) {
					inPoints = true;
					points = 0;
				} else if (length > 0 && thisKey.equals("mesh")) {
					inMesh = true;
					points = 0;
				}
				break;
				
			case VALUE_FALSE:
			case VALUE_TRUE:
			case VALUE_STRING:
			case VALUE_NUMBER:
				if (inNeighbors) {
					int n = new Integer(parser.getString());
					vertices[points].addNeighbor(vertices[n]);
					if (points < n)	// a path only counts once
						paths++;
					break;
				}
				
				switch(thisKey) {
					case "length":
						length = new Integer(parser.getString());
						vertices = new MeshPoint[length];
						break;
						
					case "x":
						x = new Double(parser.getString());
						break;
						
					case "y":
						y = new Double(parser.getString());
						break;
				}
				break;
				
			case END_OBJECT:
				if (inPoints) {
					vertices[points] = new MeshPoint(x,y,points);
					points++;
				}
				break;
				
			case START_ARRAY:
				if (inMesh)
					inNeighbors = true;
				break;
				
			case END_ARRAY:
				if (inPoints)
					inPoints = false;
				else if (inNeighbors) {
					inNeighbors = false;
					points++;
				} else if (inMesh)
					inMesh = false;
				break;
				
			case START_OBJECT:
			default:
				break;
			}
		}
		parser.close();
		try {
			r.close();
		} catch (IOException e) {
			System.err.println("FATAL: close error on input file " + filename);			
		}
		
		if (points == 0) {
			System.out.println("ERROR: file " + filename + " does not contain any mesh points");
			return;
		} else if (parms.debug_level > 0) {
			System.out.println("Loaded " + points + "/" + length + " points, " + paths + " paths from file " + filename);
		}
	}
	
	/**
	 * find the MeshPoint closest to a map coordinate
	 * @param x desired map coordinate (e.g. -0.5 to 0.5)
	 * @param y desired map coordinate (e.g. -0.5 to 0.5)
	 * @return MeshPoint (closest)
	 */
	public MeshPoint choosePoint(double x, double y) {
		MeshPoint spot = new MeshPoint(x,y);
		MeshPoint closest = null;
		double distance = 2 * Parameters.x_extent;
		
		for( int i = 0; i < vertices.length; i++ ) {
			MeshPoint p = vertices[i];
			double d = p.distance(spot);
			if (d < distance) {
				closest = p;
				distance = d;
			}
		}
		return(closest);
	}

	/**
	 * truncate values outside the extent box to the edge of the box
	 * @param p Point coordinates to be adjusted
	 */
	private Point truncate(Point p) {
		double x = p.x;
		if (x < -Parameters.x_extent/2)
			x = -Parameters.x_extent/2;
		else if (x > Parameters.x_extent/2)
			x = Parameters.x_extent/2;
		
		double y = p.y;
		if (y < -Parameters.y_extent/2)
			y = -Parameters.y_extent/2;
		else if (y > Parameters.y_extent/2)
			y = Parameters.y_extent/2;
		
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
		if (p.x < -Parameters.x_extent/2 || p.x > Parameters.x_extent/2)
			return false;
		if (p.y < -Parameters.y_extent/2 || p.y > Parameters.y_extent/2)
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
			
			if (parms.debug_level  >= MESH_DEBUG)
				System.out.println("initial point <" + v.position + "> -> <" + newPoints[i-1] + ">");
		}
		return(newPoints);
	}
	
	
	/**
	 * turn a set of points into a mesh
	 * @param points set of (not yet connected) MeshPoints
	 *
	 * 		compute the Voronoi tesselation
	 * 		for each edge (that is within the box)
	 * 			add each new end to our vertex list
	 * 			add the edge as path in/out of each vertex
	 * 
	 * NOTE: that the original points are replaced with the
	 * 		 vertices of the corresponding Voronoi polygons.
	 */
	public void makeMesh( MeshPoint[] points ) {
		int numPaths = 0;
		
		// compute the Voronoi teselation of the current point set
		VoronoiDiagram vd = new VoronoiDiagram();
		for (int i = 0; i < points.length; i++) {
			vd.insert_point_site(new Point(points[i].x, points[i].y));
		}
		HalfEdgeDiagram g = vd.get_graph_reference();
		
		// allocate hash table to track known vertices
		MeshPointHasher pointhash = new MeshPointHasher(g.num_vertices(), Parameters.x_extent, Parameters.y_extent);

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
			if (!inTheBox(p2))
				continue;		
			
			// assign/get the vertex ID of each end
			MeshPoint mp1 = pointhash.findPoint(p1.x, p1.y);
			MeshPoint mp2 = pointhash.findPoint(p2.x, p2.y);
			
			// note that each is a neighbor of the other
			mp1.addNeighbor(mp2);
			mp2.addNeighbor(mp1);
			numPaths++;
		} 
		
		// copy out the list of unique Vertices
		vertices = new MeshPoint[pointhash.numVertices];
		for(int i = 0; i < pointhash.numVertices; i++)
			vertices[i] = pointhash.vertices[i];
		
		/*
		 * NOTE
		 * I was irritated by the number of disconnected edge-points and
		 * had been thinking about stitching together non-fully connected
		 * points (with a clockwise scan, connecting adjacent 
		 * non-3-neighbor MeshPoints). 
		 * But the primary purpose of the neighbor list is water flow 
		 * ... which ends at the edges anyway.
		 */

		if (parms.debug_level  >= MESH_DEBUG)
			System.out.println(points.length + " points-> " + vertices.length + "/" + g.num_vertices() + 
					" mesh vertices, " + numPaths/2 + "/" + g.num_edges() + " mesh paths");
	}
}
