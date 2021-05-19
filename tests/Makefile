# 
# unit test cases
#
JAR = ../worldBuilder.jar
MESH = ../src/Templates/default_1024.json

all:	load_save sealevel mountains

#
# load a complex map and then save it ... output should be identical to input
#
load_save: $(JAR) test_world_2.json
	@echo "loading and saving a complex map"
	@echo "load test_world_2.json"		>  /tmp/$@
	@echo "save /tmp/$@.json"		>> /tmp/$@
	@echo "exit 0"				>> /tmp/$@
	@rm -f /tmp/$@.json
	@java -jar $(JAR) -s /tmp/$@
	@cmp test_world_2.json /tmp/$@.json;	\
	if [ $$? -ne 0 ];			\
	then					\
		echo "... $@: /tmp/$@.json differs from original input";\
		false;				\
	else					\
		echo "... saved output identical to original input";	\
		rm -f /tmp/$@ /tmp/$@.json;	\
	fi

sealevel: $(JAR)
	@echo changing sea-level with Z and meter values
	@echo "set z_scale 4000m"		>  /tmp/$@
	@echo "sealevel -100m"			>> /tmp/$@
	@echo "save /tmp/$@-a.json"		>> /tmp/$@
	@echo "sealevel 0.001"			>> /tmp/$@
	@echo "save /tmp/$@-b.json"		>> /tmp/$@
	@echo "exit 0"				>> /tmp/$@
	@rm -f /tmp/$@-?.json
	@java -jar $(JAR) -s /tmp/$@ $(MESH)
	@v=`grep sealevel /tmp/$@-a.json | cut -d: -f2 | cut -d\" -f2`; \
	if [ $$v="-100m" ];			\
	then					\
		echo "... sealevel -100m correctly saved as $$v";	\
		rm -f /tmp/$@-a.json;			\
	else					\
		echo "... sealevel -100m returned $$v";	\
		false;				\
	fi
	@v=`grep sealevel /tmp/$@-b.json | cut -d: -f2 | cut -d\" -f2`; \
	if [ $$v="4m" ];			\
	then					\
		echo "... sealevel 0.001 correctly saved as $$v ";	\
		rm -f /tmp/$@-a.json;			\
	else					\
		echo "... sealevel 0.001 returned $$v";	\
		false;				\
	fi

mountains: $(JAR) mountain_test.json
	@echo "creating mountains/ridges/pits/valleys w/height & width in all units"
	@echo "set z_scale 4000m"			>  /tmp/$@
	@echo "set xy_scale 200km"			>> /tmp/$@
	@echo "display T"				>> /tmp/$@
#	row one: circles and NW/SE
	@echo "outline square"				>> /tmp/$@
	@echo "mountain <-0.4,-0.3> 0.1 0.05 flat"	>> /tmp/$@
	@echo "pit <-0.2,-0.3> -0.5km 15000m"		>> /tmp/$@
	@echo "ridge <-0.05,-0.4>-<0.15,-0.15> 500m 5km round"	>> /tmp/$@
	@echo "outline elipse"				>> /tmp/$@
	@echo "valley <0.2,-0.4>-<0.4,-0.15> -500m 5km cone"	>> /tmp/$@
#	row two: N/S and NE/SW
	@echo "outline elipse"				>> /tmp/$@
	@echo "ridge <-0.4,-0.05>-<-0.4,0.2> 500m 5km"	>> /tmp/$@
	@echo "outline square"				>> /tmp/$@
	@echo "valley <-0.2,-0.05>-<-0.2,0.2> -500m 5km"	>> /tmp/$@
	@echo "outline elipse"				>> /tmp/$@
	@echo "ridge <0.15,-0.05>-<-0.05,0.2> 500m 5km"	>> /tmp/$@
	@echo "outline square"				>> /tmp/$@
	@echo "valley <0.4,-0.05>-<0.2,0.2> -500m 5km"	>> /tmp/$@
#	row three E/W
	@echo "outline square"				>> /tmp/$@
	@echo "ridge <-0.4,0.35>-<-0.2,0.35> 500m 5km"	>> /tmp/$@
	@echo "outline elipse"				>> /tmp/$@
	@echo "valley <0.1,0.35>-<0.3,0.35> -500m 5km"	>> /tmp/$@
#	save and compare
	@echo "save /tmp/$@.json"			>> /tmp/$@
	@echo "exit 0"					>> /tmp/$@
	@java -jar $(JAR) -s /tmp/$@ $(MESH)
	@cmp /tmp/$@.json mountain_test.json;	\
	if [ $$? -ne 0 ];			\
	then					\
		echo "... $@: /tmp/$@.json does not match expected (mountain_test.json)";\
		false;				\
	else					\
		echo "... all heights/depths are as expected (mountain_test.json)";\
		rm -f /tmp/$@ /tmp/$@.json;	\
	fi

attributes: $(JAR) attribute_test.json
	@echo "creating stripes of rainfall ..."
	@echo "set z_scale 4000m"				>  /tmp/$@
	@echo "set xy_scale 200km"				>> /tmp/$@
	@echo "display TRMFA"					>> /tmp/$@
	@echo "rainfall <-0.5,-0.5>-<0.5,-0.4>	50"		>> /tmp/$@
	@echo "rainfall <-0.5,-0.4>-<0.5,-0.3>	100"		>> /tmp/$@
	@echo "rainfall <-0.5,-0.3>-<0.5,-0.2>	200cm/y"	>> /tmp/$@
	@echo "rainfall <-0.5,-0.2>-<0.5,-0.1>	300cm/y"	>> /tmp/$@
	@echo "rainfall <-0.5,-0.1>-<0.5,-0.0>	400cm/y"	>> /tmp/$@
	@echo "creating blocks of stanard minerals ..."
	@echo "minerals <-0.5,0.0>-<-0.4,0.1>	Alluvial"	>> /tmp/$@
	@echo "minerals <-0.4,0.0>-<-0.3,0.1>	\"Sand Stone\""	>> /tmp/$@
	@echo "minerals <-0.3,0.0>-<-0.2,0.1>	Granite"	>> /tmp/$@
	@echo "minerals <-0.2,0.0>-<-0.1,0.1>	Basalt"		>> /tmp/$@
	@echo "minerals <-0.1,0.0>-<-0.0,0.1>	\"Lime Stone\""	>> /tmp/$@
	@echo "minerals  <0.0,0.0>-<0.1,0.1>	Marble"		>> /tmp/$@
	@echo "minerals  <0.1,0.0>-<0.2,0.1>	\"Copper Ore\""	>> /tmp/$@
	@echo "minerals  <0.2,0.0>-<0.3,0.1>	\"Iron Ore\""	>> /tmp/$@
	@echo "minerals  <0.3,0.0>-<0.4,0.1>	Silver"		>> /tmp/$@
	@echo "minerals  <0.4,0.0>-<0.5,0.1>	Gold"		>> /tmp/$@
	@echo "creating blocks of stanard floral ecotopes ..."
	@echo "flora    <-0.5,0.15>-<-0.4,0.25>	Alpine"		>> /tmp/$@
	@echo "flora    <-0.4,0.15>-<-0.3,0.25>	Desert"		>> /tmp/$@
	@echo "flora    <-0.3,0.15>-<-0.2,0.25>	Grassland"	>> /tmp/$@
	@echo "flora    <-0.2,0.15>-<-0.1,0.25>	Brush"		>> /tmp/$@
	@echo "flora    <-0.1,0.15>-<0.0,0.25>	Marsh"		>> /tmp/$@
	@echo "flora     <0.0,0.15>-<0.1,0.25>	Riperian"	>> /tmp/$@
	@echo "flora     <0.1,0.15>-<0.2,0.25>	Palms"		>> /tmp/$@
	@echo "flora     <0.2,0.15>-<0.3,0.25>	Jungle"		>> /tmp/$@
	@echo "flora     <0.3,0.15>-<0.4,0.25>	Broadleaf"	>> /tmp/$@
	@echo "flora     <0.4,0.15>-<0.5,0.25>	Conifers"	>> /tmp/$@
	@echo "creating blocks of stanard fauna types ..."
	@echo "fauna    <-0.5,0.3>-<-0.4,0.4>	Fish"		>> /tmp/$@
	@echo "fauna    <-0.4,0.3>-<-0.3,0.4>	Birds"		>> /tmp/$@
	@echo "fauna    <-0.3,0.3>-<-0.2,0.4>	Rabbits"	>> /tmp/$@
	@echo "fauna    <-0.2,0.3>-<-0.1,0.4>	Foxes"		>> /tmp/$@
	@echo "fauna    <-0.1,0.3>-<0.0,0.4>	Deer"		>> /tmp/$@
	@echo "fauna     <0.0,0.3>-<0.1,0.4>	Bear"		>> /tmp/$@
#	save and compare
	@echo "save /tmp/$@.json"				>> /tmp/$@
	@echo "exit 0"						>> /tmp/$@
	@java -jar $(JAR) -s /tmp/$@ $(MESH)
	@cmp /tmp/$@.json attribute_test.json;	\
	if [ $$? -ne 0 ];			\
	then					\
		echo "... $@: /tmp/$@.json does not match expected (attribute_test.json)";\
		false;				\
	else					\
		echo "... all point attributes are as expected (attribute_test.json)";\
		rm -f /tmp/$@ /tmp/$@.json;	\
	fi

waterflow: $(JAR) waterflow_test.json
	@echo "set z_scale 4000m"			>  /tmp/$@
	@echo "set xy_scale 200km"			>> /tmp/$@
	@echo "display TW"				>> /tmp/$@
	@echo "creating hills and valleys"
	@echo "adding rain-fall"
	@echo "adding arterial rivers"
#	save and compare
	@echo "save /tmp/$@.json"			>> /tmp/$@
	@echo "exit 0"					>> /tmp/$@
	@java -jar $(JAR) -s /tmp/$@ $(MESH)
	@cmp /tmp/$@.json waterflow_test.json;	\
	if [ $$? -ne 0 ];			\
	then					\
		echo "... $@: /tmp/$@.json does not match expected";\
		false;				\
	else					\
		echo "... all water depths are as expected";\
		rm -f /tmp/$@ /tmp/$@.json;	\
	fi
