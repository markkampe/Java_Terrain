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
		echo "... $@: /tmp/$@.json does not match expected";\
		false;				\
	else					\
		echo "... all heights/depths are as expected";\
		rm -f /tmp/$@ /tmp/$@.json;	\
	fi

