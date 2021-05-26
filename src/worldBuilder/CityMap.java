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
	private Journey[] journeyNodes;
	
	private static final int STROKE_WIDTH = 3;
	
	/**
	 * instantiate a city/route renderer
	 * @param map	to be rendered
	 */
	public CityMap(Map map) {
		this.map = map;
		this.mesh = map.mesh;
		this.names = map.getNameMap();
		this.journeyNodes = map.journeys();
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
		LinkedList<TradeRoutes.TradeRoute> routes = map.tradeRoutes();
		if (routes != null) {
			for(Iterator<TradeRoutes.TradeRoute> it = routes.iterator(); it.hasNext(); ) {
				TradeRoutes.TradeRoute r = it.next();
				for(int n = r.node1; journeyNodes[n].route >= 0; n = journeyNodes[n].route)
					connect(g, n, journeyNodes[n].route);
				if (journeyNodes[r.node2] != null) {	// non-oceanic path
					connect(g, r.node1, r.node2);
					for(int n = r.node2; journeyNodes[n].route >= 0; n = journeyNodes[n].route)
						connect(g, n, journeyNodes[n].route);
				}
			}
		}
		g2d.setStroke(new BasicStroke(1));

		// put up the icons and place names
		FontMetrics m = g.getFontMetrics();
		int n_height = m.getHeight();
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (names[i] != null) {
				// make sure it is on the current screen
				if (!map.on_screen(mesh.vertices[i].x, mesh.vertices[i].y))
					continue;
				String s = names[i];
				String t = CityDialog.lexType(s);
				int x = map.screen_x(mesh.vertices[i].x);
				int y = map.screen_y(mesh.vertices[i].y);
				// draw the icon
				for(int j = 0; j < CityDialog.typeList.length; j++)
					if (map.iconImages[j] != null && CityDialog.typeList[j].equals(t)) {
						// get the pixels
						BufferedImage img = map.iconImages[j];
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
		if (!map.on_screen(mesh.vertices[n1].x, mesh.vertices[n1].y))
			return;
		if (!map.on_screen(mesh.vertices[n2].x, mesh.vertices[n2].y))
			return;
		
		int x1 = map.screen_x(mesh.vertices[n1].x);
		int y1 = map.screen_y(mesh.vertices[n1].y);
		int x2 = map.screen_x(mesh.vertices[n2].x);
		int y2 = map.screen_y(mesh.vertices[n2].y);
		g.drawLine(x1, y1, x2, y2);
	}
}
