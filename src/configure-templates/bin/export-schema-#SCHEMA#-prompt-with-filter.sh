#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

# Notes for shell scripting:
# - due to shell expansion, the wildcard % is preferred to * on Linux/Mac/Unix environments
# - if using multiline commands, the \ character MUST be the LAST character on the line, 
#   else you will get errors

read -p 'Filter for DDL object names including wildcards, e.g. "xlib%" or "xlib*" : ' FILTER

# go to the right directory for exporing APEX and/or REST applications through the 
# script: opal-tools/export-scripts/opal-export-post-script.sql
cd "${OPAL_TOOLS_SRC_SQL_DIR}"

opal-export.sh  --config-file "${OPAL_TOOLS_HOME_DIR}/conf/opal-export.conf" \
                --output-dir "${OPAL_TOOLS_SRC_SQL_DIR}" \
                --pre-scripts "${OPAL_TOOLS_HOME_DIR}/export-scripts/opal-export-pre-script.sql" \
                --connection-pool-name #SCHEMA# \
                --connection-pool-file "${OPAL_TOOLS_USER_CONFIG_DIR}/connections-#ENV#.json" \
                --includes ${FILTER} 
