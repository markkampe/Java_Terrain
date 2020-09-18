#!/bin/bash
#
# The worldBuilder is a java application, but rather than force people
# to figure out how to run an application in a jar, this script does
# that for them.
#
# any and ll command-line parameters are passed to the app.
#

ARCHIVE=/usr/share/worldBuilder/worldBuilder.jar
PERSONAL=$HOME/.worldBuilder/worldBuilder.json

#
# see if this user is overriding the default configuration file(s)
#
if [ -n "$WORLDBUILDER_CONFIG" -a -f "$WORLDBUILDER_CONFIG" ]
then
    # config specified in environment variable
    echo "using WORLDBUILDER_CONFIG=$WORLDBUILDER_CONFIG"
    java -jar $ARCHIVE -c $WORLDBUILDER_CONFIG $@
elif [ -f "$PERSONAL" ]
then
    # user has their own personal worldBuilder configuration
    echo "using personal configuration $PERSONAL"
    java -jar $ARCHIVE -c $PERSONAL $@ 
else
    # use default configuration in the jar
    java -jar $ARCHIVE $@
fi
