# WorldBuilder (Technical Documentation)

   * **index.html** ... descriptive prose with links to the class diagrams and _JavaDoc_
   * **Makefile** ... builds complete _JavaDoc_ documentation from the current _worldBuilder_ sources.
   * **JavaDoc** ... this (not checked in) is where the **Makefile** puts the _JavaDoc_ output.

Building UML class diagrams from the source code would take forever and produce models
that were too complex to be read.  Therefore I have created a set of greatly simplified
skeletons that can be processed to yield a set of much simpler class diagrams:
   * **EditDialogs.java** ... classes involved in map creation and editing
   * **ViewDialogs.java** ... classes involved in painting maps on the screen
   * **ExportDialogs.java** ... classes involved in exporting maps for use by other programs
   * **Fundamental.java** ... building block classes (that everything uses)
