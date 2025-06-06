#!/bin/bash
java $OPAL_TOOLS_JAVA_ARGS -cp "${OPAL_TOOLS_HOME_DIR}/lib/*" de.opal.tests.SQLclTest "$@"

