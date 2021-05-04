# 
# unit test cases
#
JAR = ../worldBuilder.jar

all:	load_save sealevel

#
# load a complex map and then save it ... output should be identical to input
#
load_save: $(JAR)
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
	@java -jar $(JAR) -s /tmp/$@
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
