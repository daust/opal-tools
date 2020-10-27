#!/bin/bash
# when being called directly, initialize the environment first by
# calling the script set-script-environment.sh in the same directory as the script itself.
SCRIPT_DIR=`dirname $0`
#echo running $SCRIPT_DIR/set-script-environment.sh
source $SCRIPT_DIR/set-script-environment.sh

java $OPAL_INSTALLER_JAVA_ARGS -cp "${OPAL_INSTALLER_HOME_DIR}/lib/*" de.opal.Main "$@"

