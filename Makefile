#
#
#
PACKAGE=worldBuilder
BINARIES=bin
LIBRARIES=lib
WORK=/tmp/new_debian
VERSION=$(shell grep "PROGRAM_VERSION =" src/worldBuilder/Parameters.java | cut -d\" -f2 )

debian:	$(PACKAGE).jar control
	# delete any previous package and intermediates
	rm -f $(PACKAGE)-$(VERSION).deb
	sudo rm -rf $(WORK)
	#
	# create all the needed directories
	mkdir -p $(WORK)/debian/DEBIAN
	mkdir -p $(WORK)/debian/usr/bin
	mkdir -p $(WORK)/debian/usr/share/applications
	mkdir -p $(WORK)/debian/usr/share/$(PACKAGE)/images
	chmod -R 755 $(WORK)
	#
	# copy in the CLI
	cp packaging/worldBuilder.sh $(WORK)/debian/usr/bin/worldBuilder
	chmod 755 $(WORK)/debian/usr/bin/worldBuilder
	#
	# copy in the JAR
	mv $(PACKAGE).jar $(WORK)/debian/usr/share/$(PACKAGE)
	chmod 755 $(WORK)/debian/usr/share/$(PACKAGE)/$(PACKAGE).jar
	#
	# copy in the desktop file
	cp packaging/worldBuilder.desktop $(WORK)/debian/usr/share/applications
	chmod 644 $(WORK)/debian/usr/share/applications/worldBuilder.desktop
	#
	# copy in the icons
	chmod 755 $(WORK)/debian/usr/share/$(PACKAGE)/images
	cp bin/images/*.png $(WORK)/debian/usr/share/$(PACKAGE)/images
	chmod 644 $(WORK)/debian/usr/share/$(PACKAGE)/images/*.png
	#
	# finish and copy in the package configuration
	echo -n "Installed-Size: " >> control
	du -bsk $(WORK) | cut -f1 >> control
	mv control $(WORK)/debian/DEBIAN
	#
	# root should own everything
	sudo chown -R root $(WORK)/debian
	sudo chgrp -R root $(WORK)/debian
	#
	# finally, build the package
	cd $(WORK); dpkg --build debian
	mv $(WORK)/debian.deb $(PACKAGE)-$(VERSION).deb
	sudo rm -rf $(WORK)

$(PACKAGE).jar:
	jar --create --file worldBuilder.jar --manifest packaging/manifest \
		-C $(BINARIES) worldBuilder \
		-C $(BINARIES) Templates \
		-C $(BINARIES) images

control:
	echo "Package: $(PACKAGE)" 	> $@
	echo "Version: $(VERSION)"	>> $@
	echo "Section: Game"		>> $@
	echo "Priority: optional"	>> $@
	echo "Architecture: all"	>> $@
	echo "Maintainer: Mark Kampe <mark.kampe@gmail.com>"	>> $@
	echo "Description: FRPG World Builder"			>> $@
	echo " A tool for creating realistic topographies and exporting them"	>> $@
	echo " (as world maps) to various FRPG systems."	>> $@
	echo "Depends: openjdk-8-jre"	>> $@
	# echo "Homepage: http://NOT-YET"			>> $@
