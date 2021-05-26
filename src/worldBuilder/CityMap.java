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
		this.journeyNodes = map.journeys;
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
				int n = r.node1;
				while (n >= 0) {
					// see if we have reached the end of the first half
					if (journeyNodes[n].route < 0)
						if (n == r.city1) {
							n = r.node2;
							continue;
						} else
							break;
					// FIX, draw the missing piece
					
					// figure out where the end-points are on the screen
					int x1 = map.screen_x(mesh.vertices[n].x);
					int y1 = map.screen_y(mesh.vertices[n].y);
					n = journeyNodes[n].route;
					int x2 = map.screen_x(mesh.vertices[n].x);
					int y2 = map.screen_y(mesh.vertices[n].y);
					
					g.drawLine(x1, y1, x2, y2);
				}
			}
		}
		g2d.setStroke(new BasicStroke(1));

		// put up the icons and place names
		FontMetrics m = g.getFontMetrics();
		int n_height = m.getHeight();
		for(int i = 0; i < map.mesh.vertices.length; i++) {
			if (names[i] != null) {
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
}
