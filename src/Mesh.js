/**
 * @module mesh ... functions to generate the basic map
 *
 *	a mesh is a triangular tessalation of the map.
 *	a mesh includes:
 *		pts	... the original well spaced points
 *		vor	... Voronoi tesselation of those points
 *		vxs	... <x,y> coordinate of each Voronoi vertex
 *		adj	... list of vertex indices of neighors of each vertex
 *		tris	... list of <x,y> coordinates neighbors of each vertex
 *
 *		edges	... list of [index, index, <x,y>, <x,y>] tupples
 *
 *	O'Leary observed that a map created on a square grid never
 *	loses its regularity, so he wanted to build the map on an
 *	irregular grid.  But he found randomly chosen grids to be
 *	too irregular.  Mesh generation implements his compromise.
 *
 *	1. He starts by generating N completely randomly chosen points.
 *	   But these turn out to be a little to clumpy, so he smoothes
 *	   them out (improves them) by finding the Voronoi polygons
 *	   around those points and using their vertices.
 *
 *	2. He uses those (improved) points as the centers for a
 *	   second Voronoi tesselation.  The edges of those polygons
 *	   are then converted into a triangular grid
 *
 * NOTE: <x,y> coordinates are relative to the center of the map
 */
public class OldMesh {
	
	public MapPoint[] points;	// the points



/**
 * improvePoints: smooth a set of random points
 *
 * @param 	set of <x,y> points
 * @param	number of smoothing iterations
 * @param	extent (range limits)
 * @return	list of smoother <x,y> coordinates
 *
 * each iteration smooths out the distribution of the points
 *	for each point in the set
 *	    generate surrounding Voronoi polygon
 *	    use the centroid of that polygon
 */
function improvePoints(pts, n, extent) {
    n = n || 1;
    extent = extent || defaultExtent;
    for (var i = 0; i < n; i++) {
        pts = voronoi(pts, extent)
            .polygons(pts)
            .map(centroid);
    }
    return pts;
}

/**
 * generateGoodPoints: generate attractive random grid
 *
 * @param	number of points
 * @param	extent (range limits)
 * @return	list of <x,y> coordinates
 *
 * 1. generate a set of random points in the map
 * 2. run one improvement iteration on them
 */
function generateGoodPoints(n, extent) {
    extent = extent || defaultExtent;
    var pts = generatePoints(n, extent);
    pts = pts.sort(function (a, b) {
        return a[0] - b[0];
    });
    return improvePoints(pts, 1, extent);
}

// identify the Voronoi sets associated with a set of points
/**
 * voronoi: compute the Voronoi tesselation for a set or points
 *
 * @param	list of <x,y> coordinates
 * @param	extent (range limits)
 * @param	list of Voronoi regions
 */
function voronoi(pts, extent) {
    extent = extent || defaultExtent;
    var w = extent.width/2;
    var h = extent.height/2;
    return d3.voronoi().extent([[-w, -h], [w, h]])(pts);
}

/**
 * makeMesh - turn a set of well distributed points into a mesh
 *
 * @param	list of <x,y> coordinates
 * @param	extent (size range)
 */
function makeMesh(pts, extent) {
    extent = extent || defaultExtent;

    // compute the Voronoi polygons
    var vor = voronoi(pts, extent);
    var vxs = [];	// vertex locations
    var vxids = {};	// vertex ID #s
    var adj = [];	// adjacent vertices	
    var edges = [];	// list of vertex IDs and positions
    var tris = [];	// coordinates of neighbors of this vertex

    // for each edge of each Voronoi polygon
    for (var i = 0; i < vor.edges.length; i++) {
	// get the two end points of this edge
        var e = vor.edges[i];
        if (e == undefined) continue;

	// lookup (or assign) their vertex IDs
        var e0 = vxids[e[0]];
        if (e0 == undefined) {
            e0 = vxs.length;	
            vxids[e[0]] = e0;
            vxs.push(e[0]);
        }
        var e1 = vxids[e[1]];
        if (e1 == undefined) {
            e1 = vxs.length;
            vxids[e[1]] = e1;
            vxs.push(e[1]);
        }

	// note that each end-point is adjacent to the other
        adj[e0] = adj[e0] || [];
        adj[e0].push(e1);
        adj[e1] = adj[e1] || [];
        adj[e1].push(e0);

	// add indices and coordinates to known edges
        edges.push([e0, e1, e.left, e.right]);

	// note all edges entering the left end point
        tris[e0] = tris[e0] || [];
        if (!tris[e0].includes(e.left)) tris[e0].push(e.left);
        if (e.right && !tris[e0].includes(e.right)) tris[e0].push(e.right);

	// note all edges entering the right end point
        tris[e1] = tris[e1] || [];
        if (!tris[e1].includes(e.left)) tris[e1].push(e.left);
        if (e.right && !tris[e1].includes(e.right)) tris[e1].push(e.right);
    }

    // the new mesh contains all of these things
    var mesh = {
        pts: pts,	// a set of nicely spaced random points
        vor: vor,	// Voronoi tesselation of those points
        vxs: vxs,	// locations of each vertex
        adj: adj,	// indices of neighbors
        tris: tris,	// coordinates of neighbors
        edges: edges,	// the set of all edges
        extent: extent	// the scale 
    }

    /*
     * mesh.map(f) applies f to every vertex in mesh
     */
    mesh.map = function (f) {
        var mapped = vxs.map(f);
        mapped.mesh = mesh;
        return mapped;
    }
    return mesh;
}


/**
 * generateGoodMesh - top level mesh generation
 *
 * @param	number of desired points
 * @param	extent (size limits)
 * @return	mesh
 */
function generateGoodMesh(n, extent) {
    extent = extent || defaultExtent;
    var pts = generateGoodPoints(n, extent);
    return makeMesh(pts, extent);
}

/**
 * isedge - is a point on the map edge
 *
 * @param	mesh
 * @param	index of point of interest
 * @return	true ... point is on the edge
 *
 * In the final (triangular) grid points on the edge have 
 * only two neighbors, while internal points have 3 or more.
 */
function isedge(mesh, i) {
    return (mesh.adj[i].length < 3);
}

// near edge means in the outer 5% of the map
/**
 * isnearedge - is a point near the map edge
 *
 * @param	mesh
 * @param	index of point of interest
 * @return	true ... point is within 5% of edge
 */
function isnearedge(mesh, i) {
    var x = mesh.vxs[i][0];
    var y = mesh.vxs[i][1];
    var w = mesh.extent.width;
    var h = mesh.extent.height;
    return x < -0.45 * w || x > 0.45 * w || y < -0.45 * h || y > 0.45 * h;
}

/**
 * neighbors - neighbors of a vertex
 *
 * @param	mesh
 * @param	index of point of interest
 * @return	list of indices (of neighboring points)
 */
function neighbours(mesh, i) {
    var onbs = mesh.adj[i];
    var nbs = [];
    for (var i = 0; i < onbs.length; i++) {
        nbs.push(onbs[i]);
    }
    return nbs;
}

/**
 * distance - distance between two points
 *
 * @param	mesh
 * @param	index of first point
 * @param	index of second point
 * @return	(positive) distance between them
 */
function distance(mesh, i, j) {
    var p = mesh.vxs[i];
    var q = mesh.vxs[j];
    return Math.sqrt((p[0] - q[0]) * (p[0] - q[0]) + (p[1] - q[1]) * (p[1] - q[1]));
}


/**
 * visualizePoints - plot points on a map
 *	
 * @param	SVG field 
 *		(1000x1000, centered <0,0>)
 * @param	list of <x,y> coordinates
 *		in range <-0.5,-0.5> to <0.5,0.5>
 */
function visualizePoints(svg, pts) {
    // remove all exising circles from the SVG
    var circle = svg.selectAll('circle').data(pts);
    circle.enter()		// HELP
        .append('circle');	// HELP
    circle.exit().remove();	// HELP

    // translate 0-1 coordinates into 1Kx1K coordinaces
    // with radius of 1% of field with
    d3.selectAll('circle')
        .attr('cx', function (d) {return 1000*d[0]})
        .attr('cy', function (d) {return 1000*d[1]})
        .attr('r', 100 / Math.sqrt(pts.length));
}

/**
 * makeD3Path - construct path connecting a set of points
 *	start at first point, draw line to each subsequent point
 *
 * @param	list of <x,y> coordinates
 * @return	string representation of connecting path
 */
function makeD3Path(path) {
    var p = d3.path();
    p.moveTo(1000*path[0][0], 1000*path[0][1]);
    for (var i = 1; i < path.length; i++) {
        p.lineTo(1000*path[i][0], 1000*path[i][1]);
    }
    return p.toString();
}

/**
 * drawPaths - draw line connecting a set of points
 *
 * @param	SVG field
 * @param	class of path to draw
 * @param	list of <x,y> coordinates
 */
function drawPaths(svg, cls, paths) {
    // remove all existing paths from the SVG
    var paths = svg.selectAll('path.' + cls).data(paths)
    paths.enter()
            .append('path')
            .classed(cls, true)
    paths.exit()
            .remove();

    // draw line along the connecting path
    svg.selectAll('path.' + cls)
        .attr('d', makeD3Path);
}
}
