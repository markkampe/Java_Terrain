# 
# unit test cases
#
JAR = ../worldBuilder.jar
COARSE = ../src/Templates/default_1024.json
FINE = ../src/Templates/default_4096.json

all:	load_save sealevel mountains attributes

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
	@java -jar $(JAR) -s /tmp/$@ $(COARSE)
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
	@echo "set z_scale 4000m"				>  /tmp/$@
	@echo "set xy_scale 200km"				>> /tmp/$@
	@echo "display TC"					>> /tmp/$@
	@echo "row one: circles and NW/SE square & elipse..."
	@echo "outline square"					>> /tmp/$@
	@echo "mountain <-0.4,-0.3> 0.1 0.05 flat"		>> /tmp/$@
	@echo "region   <-0.45,-0.45> \"h=0.1, r=0.05, flat\""	>> /tmp/$@
	@echo "pit <-0.2,-0.3> -0.5km 15000m"			>> /tmp/$@
	@echo "region   <-0.3,-0.45> \"h=0.5km, r=15000m\""	>> /tmp/$@
	@echo "ridge <-0.05,-0.4>-<0.15,-0.15> 500m 5km round"	>> /tmp/$@
	@echo "region   <-0.1,-0.45> \"h=500m, r=5km, round\""	>> /tmp/$@
	@echo "outline elipse"					>> /tmp/$@
	@echo "valley <0.2,-0.4>-<0.4,-0.15> -500m 5km cone"	>> /tmp/$@
	@echo "region   <0.25,-0.45> \"h=-500m, r=5km, cone\""	>> /tmp/$@
	@echo "row two: N/S and NE/SW square & elipse..."
	@echo "outline elipse"					>> /tmp/$@
	@echo "ridge <-0.4,-0.05>-<-0.4,0.2> 500m 5km"		>> /tmp/$@
	@echo "region   <-0.45,-0.1> \"h=500m, r=5km, elipse\""	>> /tmp/$@
	@echo "outline square"					>> /tmp/$@
	@echo "valley <-0.2,-0.05>-<-0.2,0.2> -500m 5km	"	>> /tmp/$@
	@echo "region   <-0.25,-0.1> \"h=-500m, r=5km, square\"">> /tmp/$@
	@echo "outline elipse"					>> /tmp/$@
	@echo "ridge <0.15,-0.05>-<-0.05,0.2> 500m 5km"		>> /tmp/$@
	@echo "region   <-0.05,-0.1> \"h=500m, r=5km, elipse\""	>> /tmp/$@
	@echo "outline square"					>> /tmp/$@
	@echo "valley <0.4,-0.05>-<0.2,0.2> -500m 5km"		>> /tmp/$@
	@echo "region   <0.30,-0.1> \"h=-500m, r=5km, square\""	>> /tmp/$@
	@echo "row three: E/W square & elipse..."
	@echo "outline square"					>> /tmp/$@
	@echo "ridge <-0.4,0.35>-<-0.2,0.35> 500m 5km"		>> /tmp/$@
	@echo "region <-0.45,0.275> \"h=500m, r=5km, square\""	>> /tmp/$@
	@echo "outline elipse"					>> /tmp/$@
	@echo "valley <0.1,0.35>-<0.3,0.35> -500m 5km"		>> /tmp/$@
	@echo "region <0.025,0.275> \"h=500m, r=5km, elipse\""	>> /tmp/$@
	@echo "save /tmp/$@.json"				>> /tmp/$@
	@echo "exit 0"						>> /tmp/$@
	@java -jar $(JAR) -s /tmp/$@ $(FINE)
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
	@echo "display TRMFAC"					>> /tmp/$@
	@echo "rainfall <-0.5,-0.5>-<0.5,-0.4>	50"		>> /tmp/$@
	@echo "region <0.0,-0.45> rain=50"			>> /tmp/$@
	@echo "rainfall <-0.5,-0.4>-<0.5,-0.3>	100"		>> /tmp/$@
	@echo "region <0.0,-0.35> rain=100"			>> /tmp/$@
	@echo "rainfall <-0.5,-0.3>-<0.5,-0.2>	200cm/y"	>> /tmp/$@
	@echo "region <0.0,-0.25> rain=200cm/y"			>> /tmp/$@
	@echo "rainfall <-0.5,-0.2>-<0.5,-0.1>	300cm/y"	>> /tmp/$@
	@echo "region <0.0,-0.15> rain=300cm/y"			>> /tmp/$@
	@echo "rainfall <-0.5,-0.1>-<0.5,-0.0>	400cm/y"	>> /tmp/$@
	@echo "region <0.0,-0.05> rain=400cm/y"			>> /tmp/$@
	@echo "creating blocks of standard minerals ..."
	@echo "minerals <-0.5,0.0>-<-0.4,0.1>	Alluvial"	>> /tmp/$@
	@echo "region   <-0.5,0.05>		Alluvial"	>> /tmp/$@
	@echo "minerals <-0.4,0.0>-<-0.3,0.1>	\"Sand Stone\""	>> /tmp/$@
	@echo "region   <-0.4,0.05>		\"Sand Stone\""	>> /tmp/$@
	@echo "minerals <-0.3,0.0>-<-0.2,0.1>	Granite"	>> /tmp/$@
	@echo "region   <-0.3,0.05>		Granite"	>> /tmp/$@
	@echo "minerals <-0.2,0.0>-<-0.1,0.1>	Basalt"		>> /tmp/$@
	@echo "region   <-0.15,0.05>		Basalt"		>> /tmp/$@
	@echo "minerals <-0.1,0.0>-<-0.0,0.1>	\"Lime Stone\""	>> /tmp/$@
	@echo "region   <-0.1,0.05>		\"Lime Stone\""	>> /tmp/$@
	@echo "minerals  <0.0,0.0>-<0.1,0.1>	Marble"		>> /tmp/$@
	@echo "region   <0.0,0.05>		Marble"		>> /tmp/$@
	@echo "minerals <0.1,0.0>-<0.2,0.1>	\"Copper Ore\""	>> /tmp/$@
	@echo "region   <0.1,0.05>		\"Copper Ore\""	>> /tmp/$@
	@echo "minerals <0.2,0.0>-<0.3,0.1>	\"Iron Ore\""	>> /tmp/$@
	@echo "region   <0.2,0.05>		\"Iron Ore\""	>> /tmp/$@
	@echo "minerals <0.3,0.0>-<0.4,0.1>	Silver"		>> /tmp/$@
	@echo "region   <0.3,0.05>		Silver"		>> /tmp/$@
	@echo "minerals <0.4,0.0>-<0.5,0.1>	Gold"		>> /tmp/$@
	@echo "region   <0.425,0.05>		Gold"		>> /tmp/$@
	@echo "creating blocks of standard floral ecotopes ..."
	@echo "flora    <-0.5,0.15>-<-0.4,0.25>	Alpine"		>> /tmp/$@
	@echo "region   <-0.5,0.20>		Alpine"		>> /tmp/$@
	@echo "flora    <-0.4,0.15>-<-0.3,0.25>	Desert"		>> /tmp/$@
	@echo "region   <-0.4,0.20>		Desert"		>> /tmp/$@
	@echo "flora    <-0.3,0.15>-<-0.2,0.25>	Grassland"	>> /tmp/$@
	@echo "region   <-0.3,0.20>		Grassland"	>> /tmp/$@
	@echo "flora    <-0.2,0.15>-<-0.1,0.25>	Brush"		>> /tmp/$@
	@echo "region   <-0.175,0.17>		Brush"		>> /tmp/$@
	@echo "flora    <-0.1,0.15>-<0.0,0.25>	Marsh"		>> /tmp/$@
	@echo "region   <-0.1,0.17>		Marsh"		>> /tmp/$@
	@echo "flora    <0.0,0.15>-<0.1,0.25>	Riperian"	>> /tmp/$@
	@echo "region   <0.0,0.20>		Riperian"	>> /tmp/$@
	@echo "flora    <0.1,0.15>-<0.2,0.25>	Palms"		>> /tmp/$@
	@echo "region   <0.125,0.20>		Palms"		>> /tmp/$@
	@echo "flora    <0.2,0.15>-<0.3,0.25>	Jungle"		>> /tmp/$@
	@echo "region   <0.225,0.20>		Jungle"		>> /tmp/$@
	@echo "flora    <0.3,0.15>-<0.4,0.25>	Broadleaf"	>> /tmp/$@
	@echo "region   <0.3,0.20>		Broadleaf"	>> /tmp/$@
	@echo "flora    <0.4,0.15>-<0.5,0.25>	Conifers"	>> /tmp/$@
	@echo "region   <0.420,0.175>		Conifers"	>> /tmp/$@
	@echo "creating blocks of standard fauna types ..."
	@echo "fauna    <-0.5,0.3>-<-0.4,0.4>	Fish"		>> /tmp/$@
	@echo "region   <-0.5,0.325>		Fish"		>> /tmp/$@
	@echo "fauna    <-0.4,0.3>-<-0.3,0.4>	Birds"		>> /tmp/$@
	@echo "region   <-0.375,0.35>		Birds"		>> /tmp/$@
	@echo "fauna    <-0.3,0.3>-<-0.2,0.4>	Rabbits"	>> /tmp/$@
	@echo "region   <-0.275,0.35>		Rabbits"	>> /tmp/$@
	@echo "fauna    <-0.2,0.3>-<-0.1,0.4>	Foxes"		>> /tmp/$@
	@echo "region   <-0.175,0.325>		Foxes"		>> /tmp/$@
	@echo "fauna    <-0.1,0.3>-<0.0,0.4>	Deer"		>> /tmp/$@
	@echo "region   <-0.1,0.35>		Deer"		>> /tmp/$@
	@echo "fauna    <0.0,0.3>-<0.1,0.4>	Bear"		>> /tmp/$@
	@echo "region   <0.025,0.35>		Bear"		>> /tmp/$@
	@echo "save /tmp/$@.json"				>> /tmp/$@
	@echo "exit 0"						>> /tmp/$@
	@java -jar $(JAR) -s /tmp/$@ $(COARSE)
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
	@java -jar $(JAR) -s /tmp/$@ $(FINE)
	@cmp /tmp/$@.json waterflow_test.json;	\
	if [ $$? -ne 0 ];			\
	then					\
		echo "... $@: /tmp/$@.json does not match expected";\
		false;				\
	else					\
		echo "... all water depths are as expected";\
		rm -f /tmp/$@ /tmp/$@.json;	\
	fi
