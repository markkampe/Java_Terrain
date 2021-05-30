package worldBuilder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * a class to render Cities and trade routes
 */
public class CityMap {
	private Map map;
	private Mesh mesh;		
	private String[] names;
	private boolean oceanic[];
	private double heights[];
	private Parameters parms;
	
	private static final int STROKE_WIDTH = 3;
	
	/**
	 * instantiate a city/route renderer
	 * @param map	to be rendered
	 */
	public CityMap(Map map) {
		this.map = map;
		this.mesh = map.mesh;
		this.names = map.getNameMap();
		this.oceanic = map.getDrainage().oceanic;
		this.heights = map.getHeightMap();
		this.parms = Parameters.getInstance();
	}
	
	/**
	 * Display the streams and rivers
	 * 
	 * @param g Graphics context
	 * @param width of the display map
	 * @param height of the display map
	 */
	public void paint(Graphics g, int width, int height) {
		// trade routes in black
		g.setColor(Color.BLACK);
		Graphics2D g2d = (Graphics2D) g;
		g2d.setStroke(new BasicStroke(STROKE_WIDTH));
		
		// start by rendering the trade routes
		LinkedList<TradeRoute> routes = map.tradeRoutes();
		if (routes != null) {
			for(Iterator<TradeRoute> it = routes.iterator(); it.hasNext(); ) {
				TradeRoute r = it.next();
				for(int i = 0; i < r.path.length-1; i++)
					connect(g, r.path[i], r.path[i+1]);
			}
		}
		g2d.setStroke(new BasicStroke(1));

		// put up the icons and place names
		FontMetrics m = g.getFontMetrics();
		int n_height = m.getHeight();
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (names[i] != null) {
				// make sure it is on the current screen
				if (!map.window.on_screen(mesh.vertices[i].x, mesh.vertices[i].y))
					continue;
				String s = names[i];
				String t = CityDialog.lexType(s);
				int x = map.window.screen_x(mesh.vertices[i].x);
				int y = map.window.screen_y(mesh.vertices[i].y);
				// draw the icon
				for(int j = 0; j < CityDialog.typeList.length; j++)
					if (map.window.iconImages[j] != null && CityDialog.typeList[j].equals(t)) {
						// get the pixels
						BufferedImage img = map.window.iconImages[j];
						// figure out where to put them
						int w = img.getWidth();
						int h = img.getHeight();
						x -= w/2;
						y -= h/2;
						// over-paint the black pixels
						int[] pixels = new int[w * h];
						img.getRaster().getPixels(0, 0, w, h, pixels);
						for(int r = 0; r < h; r++)
							for(int c = 0; c < w; c++)
								if (pixels[(r*w) + c] == 0)
									g.drawLine(x+c, y+r, x+c, y+r);
						
						x += w;		// name goes after the icon
						y += h/2;	// name goes at level of dot
						break;
					}
				
				// put up the name to the right of the icon
				String n = CityDialog.lexName(s);
				if (n != null && !n.equals(""))
					g.drawString(n, x, y+(n_height/3));
			}
		}

	}
	
	/**
	 * draw a line connecting two Mesh points
	 * @param n1	index of first MeshPoint
	 * @param n2	index of second MeshPoint
	 */
	private void connect(Graphics g, int n1, int n2) {
		// make sure this segment is on-screen
		if (!map.window.on_screen(mesh.vertices[n1].x, mesh.vertices[n1].y))
			return;
		if (!map.window.on_screen(mesh.vertices[n2].x, mesh.vertices[n2].y))
			return;
		
		// make sure the line is (at least partially) above water
		if (oceanic[n1] && oceanic[n2])
			return;
		
		// figure out where two end points are on the screen
		int x1 = map.window.screen_x(mesh.vertices[n1].x);
		int y1 = map.window.screen_y(mesh.vertices[n1].y);
		int x2 = map.window.screen_x(mesh.vertices[n2].x);
		int y2 = map.window.screen_y(mesh.vertices[n2].y);
		
		// if one end is Oceanic, shorten line to above water fraction
		if (oceanic[n1] || oceanic[n2]) {
			double z1 = heights[n1];
			double z2 = heights[n2];
			double dz = (z1 > z2) ? z1 - z2 : z2 - z1;
			double Zabove = ((z1 > z2) ? z1 : z2) - parms.sea_level;
			double Fabove = Zabove/dz;
			if (z1 > z2) {	// pull <x2,y2> in
				x2 = (int) (x1 + Fabove * (x2 - x1));
				y2 = (int) (y1 + Fabove * (y2 - y1));
			} else {		// pull <x1,y1> in
				x1 = (int) (x2 + Fabove * (x1 - x2));
				y1 = (int) (y2 + Fabove * (y1 - y2));
			}
		}
		
		g.drawLine(x1, y1, x2, y2);
	}
}
