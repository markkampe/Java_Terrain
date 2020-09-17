PACKAGE_NAME="worldbuilder"
PACKAGE_VERSION="0.1"
PROGRAM_NAME="worldBuilder"
PROGRAM_JAR="worldBuilder.jar"
SOURCE_DIR=HERE
TEMP_DIR="/tmp"

debian:
	#
	# copy the binaries (the Jar file)
	mkdir -p $(TEMP_DIR)/debian/lib
	cp -r bin $(TEMP_DIR)/debian/lib/$(PACKAGE_NAME)
	#
	# create the CLI
	mkdir -p $(TEMP_DIR)/debian/bin
	echo '#!/bin/sh' > $(TEMP_DIR)/debian/bin/$(PROGRAM_NAME)
	echo "java -jar $(PROGRAM_JAR)" >> $(TEMP_DIR)/debian/bin/$(PROGRAM_NAME)
	chmod 755 $(TEMP_DIR)/debian/bin/$(PROGRAM_NAME)
	#
	#
	# add the desktop icon/link
	mkdir -p $(TEMP_DIR)/debian/usr/share/applications
	cp debian/$(PROGRAM_NAME).desktop $(TEMP_DIR)/debian/usr/share/applications
	#
	# add the documentation and copyright
	mkdir -p $(TEMP_DIR)/debian/usr/share/doc/$(PACKAGE_NAME)
	cp DOCN_FILE $(TEMP_DIR)/debian/usr/share/doc/$(PACKAGE_NAME)
	chmod 644 $(TEMP_DIR)/debian/usr/share/doc/$(PACKAGE_NAME)/DOCN_FILE
	cp $(PROGRAM_NAME).svg $(TEMP_DIR)/debian/usr/share/doc/$(PACKAGE_NAME)
	cp debian/copyright $(TEMP_DIR)/debian/usr/share/doc/$(PACKAGE_NAME)
	chmod 644 $(TEMP_DIR)/debian/usr/share/doc/$(PACKAGE_NAME)/copyright
	#
	# create the control file
	mkdir -p $(TEMP_DIR)/debian/DEBIAN
	echo "Package:" $(PACKAGE_NAME) > $(TEMP_DIR)/debian/DEBIAN/control
	echo "Version:" $(PACKAGE_VERSION) >> $(TEMP_DIR)/debian/DEBIAN/control
	cat debian/control >> $(TEMP_DIR)/debian/DEBIAN/control
	PKG_SIZE=`du -bs $TEMP_DIR/debian | cut -f 1`
	PKG_BLOCKS=$((PKG_SIZE/1024))
	echo "Installed-Size: $PKG_BLOCKS" >> $TEMP_DIR/debian/DEBIAN/control
	#
	# root should own everything
	chown -R root $(TEMP_DIR)/debian
	chgrp -R root $(TEMP_DIR)/devian
	#
	# finally, build the package
	cd $(TEMP_DIR)
	dpkg --build debian
	mv debian.deb $(SOURCE_DIR)/$(PACKAGE_NAME)-$(PACKAGE_VERSION).deb
	rm -r $(TEMP_DIR)/debian

CONTROL FILE
Section: ???
Priority: optional
Architecture: all
Maintainer: Mark Kampe <mark.kampe@gmail.com>
Description: one-liner.
 multiple line 
 longer description
 .
 even multiple paragraphs
Depends: openjdk-8-jre ...
Hopepage: http://create one

DESKTOP FILE
[Desktop Entry]
Encoding=UTF-8
Name=$(PROGRAM_NAME)
Comment=RPG Map Builder
Exec=/?bin?/$(PROGRAM_NAME)
Icon=/usr/share/doc/$(PACKGAGE_NAME)/$(PROGRAM_NAME).svg
Terminal=falce
Type=Application
Categories=GNOME;Application;???
StartupNotify=true
