/** @opt operations */ interface ActionListener { void actionPerformed() {}; }
/** @opt operations */ interface WindowListener { void windowClosing() {}; }
/** @opt operations */ interface ChangeListener { void stateChanged() {}; }
/** @opt operations */ interface MouseMotionListener { void mouseDragged() {}; }
/** @opt operations */ interface MouseListener { void mousePressed() {}; 
											     void mouseClicked() {}; 
											     void mouseReleased() {}; }
/** * @opt operations */ interface MapListener {
	boolean regionSelected(double x0, double y0, double width, double height, boolean complete) {};
	boolean pointSelected(double x, double y) {};
}

/**
 * @opt all
 * @depend - - - Map
 */
class WorldDialog  implements ActionListener, ChangeListener, WindowListener {
}

/**
 * @opt all
 * @depend - - - Map
 */
class ZoomDialog  implements MapListener, ActionListener, WindowListener {
	ZoomDialog(Map map) {};
}

/**
 * @opt all
 */
class RuleDebug  implements ActionListener, WindowListener {
}

/**
 * @opt all
 * @depend - - - Map
 */
class PointDebug  implements MapListener, WindowListener {
	PointDebug(Map map) {};
}

/**
 * @opt all
 */
class Map implements MouseListener, MouseMotionListener {

	int screen_x(double map_x) {};	// ZoomDialog only
	int screen_y(double map_y) {};	// ZoomDialog only

	enum Selection {NONE, POINT, LINE, RECTANGLE, GROUP, ANY};
	void addMapListener(MapListener interested) {};
	void removeMapListener(MapListener which) {};
	void selectMode(Selection type) {};
	boolean checkSelection(Selection type) {};
	void selectRect(int x0, int y0, int x1, int y1) {};

	void highlight(int point, Color c) {};

	void setWindow(double x0, double y0, double x1, double y1) {};
}


/** @hidden */ class Color {}
/** @hidden */ class Graphics {}
