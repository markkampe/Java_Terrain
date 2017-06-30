
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
 *         a mesh is a triangular tessalation of the map. a mesh includes: pts
 *         ... the original well spaced points vor ... Voronoi tesselation of
 *         those points vxs ... <x,y> coordinate of each Voronoi vertex adj ...
 *         list of vertex indices of neighors of each vertex tris ... list of
 *         <x,y> coordinates neighbors of each vertex
 *
 *         edges ... list of [index, index, <x,y>, <x,y>] tupples
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

	private double x_extent;
	private double y_extent;

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

		// create a set of random points
		Point points[] = new Point[num_points];
		for (int i = 0; i < num_points; i++) {
			double x = x_extent * (Math.random() - 0.5);
			double y = y_extent * (Math.random() - 0.5);
			points[i] = new Point(x, y);
		}
		
		// even out the distribution a little
		PointsDisplay pd = new PointsDisplay("Grid Points", 800, 800, Color.BLACK);
		pd.addPoints(points, PointsDisplay.Shape.CIRCLE, Color.GRAY);
		for( int i = 0; i < improvements; i++ )
			points = improve(points);
		pd.addPoints(points, PointsDisplay.Shape.DIAMOND, Color.WHITE);
		pd.repaint();
	}

	/**
	 * constrain coordinates to legal values
	 */
	private double truncate(double value, double extent) {
		if (value < -extent/2)
			return(-extent/2);
		else if (value > extent/2)
			return(extent/2);
		else
			return(value);
	}

	/**
	 * even out a set of random points
	 * 
	 * O'Leary did this by computing the Vornoi polygons surrounding the
	 * chosen points, and then taking the centroids of those polygons.
	 */
	private Point[] improve( Point[] points) {
		Point newPoints[] = new Point[points.length];

		// create the Voronoi tesselation
		VoronoiDiagram vd = new VoronoiDiagram();
		for (int i = 0; i < points.length; i++) {
			vd.insert_point_site(points[i]);
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
			newPoints[i++] = new Point(x_sum/numPoints, y_sum/numPoints);
			// System.out.println("initial point <" + v.position + "> -> <" + newPoints[i-1] + ">");
		}
		return(newPoints);
	}
	
	private void triangles() {
		// compute the Voronoi teselation
		// accumulate and label the vertices
		// identify the neighbors
	}
}
