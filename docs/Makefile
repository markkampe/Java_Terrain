LIBS=../lib/OpenVoronoi.jar:../lib/javax.json-1.0.jar
SOURCES=../src/worldBuilder/*.java
SCOPE=package
JD_OPTS=-public -notree -exclude java.awt --show-members $(SCOPE)

all:	javadoc uml
	
javadoc:
	javadoc $(JD_OPTS) -d JavaDoc -cp $(LIBS) $(SOURCES)

uml: Fundamental.png EditDialogs.png ViewDialogs.png ExportDialogs.png

%.png : %.java
	umlgraph $* png
