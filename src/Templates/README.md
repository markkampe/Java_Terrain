# Templates - configuration files and default meshes

## Configuration Files
We have attempted to avoid hard-coded defaults, and to make almost
all map generation parameters configurable.  Other files can be 
specified on the command line or in export dialogs, but these 
are the configuration files that are used by default:
   * **worldBuilder.json** ... initial values for all parameters in the _Parameters_ singleton.
   * **Outside.json** ... descriptions of **RPGMaker Outside** tile sets for various levels, terrains, and plant covers.
   * **Overworld.json** ... descriptions of **RPGMaker Overworld** tile sets for various levels, terrains, and plant covers.
   * **flora.json** ... definitions of flora-type sub-classes and the conditions they like
   * **Outside_flora.json** ... definitions of flora-type sub-classes and the conditions they like
   * **Overworld_flora.json** ... definitions of flora-type sub-classes and the conditions they like

## Default initial meshes
One of the first inspirations we took from O'Leary was projecting
uniform geometric figures onto a (cleaned up) random Voronoi mesh
to turn those regular deformations into very natural-looking
topographic features.

If you start the _worldBuilder_ without a map name, it will (by default)
create a new map (with the specified number of mesh points).
It is capable of generating and refining new Voronoi meshes (and 
does so when a **new Mesh** operation is performed, but doing so
takes a moderate amount of computation, so we have created a
series of default meshes for a range of reasonable sizes 
(1-16K points), and these are the meshes that are normally used
to create a new map:
   * default_1024.json
   * default_2048.json
   * default_4096.json
   * default_8192.json
   * default_16384.json

These default meshes have no altitudes or other attributes beyond
their _<x,y>_ coordinates, and are only intended to be a source of
topographic noise.  Even the coarsest (1024 point) is so fine that
using that same starting point will not impose any visible similarities
on the maps that are ultimately created based on it.
