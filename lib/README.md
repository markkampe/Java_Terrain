This worldBuilder exploits a few esoteric libraries, which many people may not have installed.
To make things simpler, I have simply included their jars in this repo.
 - **javax.json** ... is standard token-a-time JSON parser that we use for reading configuration
   files and saved worldBuilder maps.
 - **OpenVoronoi.jar** ... the _worldBuilder_ operates on a (realtively small) set of (somewhat)
   randomly placed points that are interconnected in a Voronoi mesh (where each point is 
   connected to three neighbors.  Defining such a mesh turns out to be a problem that people
   found interesting and solved long ago.  The _OpenVoronoi_ library does this for me.
 - **aspectjrt.jar** ... seems to be used (by _javadoc_) to build structural models.
 - **commons-math3** ... is the Apache Commons math library, which contains a great many
   useful functions (upon which I occasionally drew)
