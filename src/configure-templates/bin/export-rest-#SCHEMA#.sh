#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

# Notes for shell scripting:
# - due to shell expansion, the wildcard % is preferred to * on Linux/Mac/Unix environments
# - if using multiline commands, the \ character MUST be the LAST character on the line, 
#   else you will get errors

# go to the right directory for exporing APEX and/or REST applications through the 
# script: opal-tools/export-scripts/opal-export-post-script.sql
cd "${OPAL_TOOLS_SRC_REST_DIR}"
CURRDIR=$PWD

# don't export schema objects (--skip-export), only run the pre-script and post-script
opal-export.sh  --config-file "${OPAL_TOOLS_HOME_DIR}/conf/opal-export.conf" \
                --pre-scripts "${OPAL_TOOLS_HOME_DIR}/export-scripts/opal-export-pre-script.sql" \
                --post-scripts "${CURRDIR}/export-rest-#SCHEMA#.sql" \
                --connection-pool-name #SCHEMA# \
                --silent \
                --connection-pool-file "${OPAL_TOOLS_USER_CONFIG_DIR}/connections-#ENV#.json" \
                --skip-export
