# worldMap
_worldBuilder_ maps are reasonably straight-forward JSON, but it
may not be easy to check a particular attribute at a particular _<x,y>_
location. This program reads an _worldBuilder_ map, and prints it out
in a more easily understood (multiple neat 2D arrays) form.
 
## Usage
```
    Tester [filename]
```
Read the specified _worldBuilder_ map and print out:
   * basic (name, region size) information.
   * a 2D array of altitudes (Z-values)
   * a 2D array of slopes 
   * a 2D array of facing (N, NW, W ...) compass directions
   * a 2D array of facing (0-359) directions
   * a 2D array of rainfalls
   * a 2D array of hydration values
   * a 2D array of soil types (I, M, S, A)
   * a 2D array of mean Spring temperatures (degC)

If no filename is specified, it puts up a file selection dialog and
allows you to browse to the desired map.

## Files

### MapReader.java
Class to read raw JSON for a saved _worldBuilder_ map,
and provide functions to return:
   * region name, latitude and logitude
   * map height, width, and tile size
   * arrays of _<x,y>_ values for altitude, slope, rainfall, hydration, and soil type.
   * array of _<x,y>_ values for seasonal mean temperature

### Tester.java
The main program, that gets a file name, (uses _MapReader_) to read in the
map, and prints it out in a more easily read form.
