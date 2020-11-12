#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

java $OPAL_TOOLS_JAVA_ARGS -cp "${OPAL_TOOLS_HOME_DIR}/lib/*" de.opal.installer.CopyPatchTemplateMain "$@"

