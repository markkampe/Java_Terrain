The worldBuilder is a GUI for creating and editing large-region topographic
maps and exporting (parts of) them in formats consumable by various FRPGs.

This is a brief table of contents of the (over 10K lines) sources:

   The U/I: classes with which a user interacts
	
	WorldBuilder.java	... main program

	WorldDialog.java	... world map location and scale
	MountainDialog.java	... create mountains and valleys
	RainDialog.java		... adjust rainfall
	LandDialog.java		... adjust height, erosion and deposition
	RiverDialog.java	... create incoming arterial rivers
	SlopeDialog.java	... adjust the continental slope
	FloraDialog.java	... plant distributions

	MeshDialog.java		... load or create a new Mesh
	RegionDialog.java	... create a (higher resolution) sub-region map
	ZoomDialog.java		... zoom in on a sub-region

	ExportBase.java		... superclass for all export dialogs

	MapListener.java	... call-back for point/rectangular selections

	PointDebug.java		... pop-up for info about one MeshPoint

   Internal representations and engines

	Parameters.java		... default parameter values


	MeshPoint.java		... one point in the mesh
	MeshPointHasher.java	... map coordinates into a MeshPoint
	Mesh.java		... a mesh of points
	Map.java		... a set of per-MeshPoint values

	Hydrology.java		... compute water flow, erosion, deposition

	Cartesian.java		... interpolate a cartesion grid from MeshPoints
	Vicinity.java		... a collection of near-by MeshPoints
	Polygon.java		... a Vicinity defined by an enclosing polygon
	Proxcimity.java		... a Vicinity defined by nearest neighbors


   Render different parts of the map on display

	AltitudeMap.java	... altitudes rendered as shades
	TopoMap.java		... topographic lines
	RainMap.java		... rain fall
	SoilMap.java		... soil types
	ErodeMap.java		... erosion and deposition
	WaterMap.java		... lakes and seas
	RiverMap.java		... rivers

	FloraMap.java		... plant types
	FloraRule.java

	PreviewMap.java		... simple, color-per-meshpoint maps

   Classes to export maps in various formats

	Exporter.java		... interface for all Exporter entry points
	TerrainType.java	... types of terrain (mountains, swamps, etc)

	RawExport.java		... raw JSON export dialog
	JsonExporter.java	... simple Cartesian grid of per-point JSON attributes

	RPGMexport.java		... RPGM export dialog
	RPGMFlora.java		... RPGM flora placement dialog
	RuleDebug.java		... RPGM rule debug dialog (for debugging rules)
	RPGMTiler.java		... create a grid of RPGM tiles for export
	RPGMwriter.java		... write out a set of RPGM level maps
	TileRule.java		... description of a single RPGM tile
	TileRules.java		... load and search a set of RPGM tiles
	RPGMLeveler.java	... map altitudes/depths into RPGM levels
	RpgmMap.java		... key attributes of an RPGM map set
	MapIndex.java		... load and operate on an RPGM index of maps

	ObjectExport.java	... WIP object export dialog
	ObjectExporter.java	... topographic objects overlayed on a simple Cartesian grid
	OverlayObject.java	... a single (overlayable) object	
	OverlayObjects.java	... load and search a set of (overlayable) objects

	FoundationExport.java	... Foundation export dialog
	FoundExporter.java	... exporter for Foundation maps
	LuaWriter.java		... LUA file writer for Foundation exports



    Internal Utilities

	Bidder.java		... allow many tiles or rules to bid for a location

	RangeSlider.java	... a JSlider with high and low limits
	RangeSliderUI.java	... render the pointers for a RangeSlider

	DebugLog.java		... write (extensive) diagnostic output to a file
