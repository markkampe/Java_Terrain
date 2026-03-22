# Current Status
As of 2026 it no longer appears that this s/w is likely to be used, 
so I am putting the project to bed.  But, I am making these notes in
case we later want to revive it.

This is a brief overview of open *Issues*.

## Problems
All basic operations work well, but while reviewing the code and playing
with more subtle features I noticed many things that should/could be improved:

### SubRegion problems
The *WorldBuilder* was designed for creating large (100-1000 km on a side)
maps.  Most game play, however, is going to happen on higher resolution 
maps of smaller areas.  I created a *sub-region* operation to create a
higher resolution map of a defined rectangle on ther current map.
A relatively late addition was *sub-regions*, a new (distinct) higher
resolution map for a small rectangular area of the current map.

Sounds simple, doesn't it?  It mostly works, but in playing with 
operations I found a few things that were not happening correctly:

- [61, minor] Land/Water boundaries at the edge of the map are
more complex than necessary if part of the edge is off the sub-region
map.

- [104, minor] If a lake crosses a sub-region boundary, it may not 
automatically fill with water in the sub-region map.  This happens
if the inflowing source of that water is not present in the
sub-region.

- [111, minor] Rivers and trade routes entering and exiting a sub-region
may not span all the way to the edge of the sub-region map.  This is
a result of those entry/exit coordinates being Voronoi points in the
parent map.  The sub-region map will almost surely contain points
between those original coordinates and the sub-region map edges.

- [101, feature] Distances and travel costs are larger in a sub-region.
A straight line between two adjacent Voronoi points in a larger map 
will zig-zag through numerous points in the higher resolution sub-region
map.  As a result, both distances and elevation gain/loss will change.

### Problems with exports to other map formats
- [65, minor] Foundation output generation shows a few small anomalies.
- [94, minor] Foundation output generation has trouble with graceful grass-to-dirt 
transitions.
- [95, minor] RPGMaker tile sets only include single-wide N->S waterfall tile,
and as such, we cannot create small waterfalls in other orientations.

### Other things that could be better

- [112, JDK Version issue] When I started (under JDK 11) I needed to
include javax.json in my build tree.  As of version 14, javax.json
is included, and my version is detected as a conflict, and should
be removed.
