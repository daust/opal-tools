#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

# Notes for shell scripting:
# - due to shell expansion, the wildcard % is preferred to * on Linux/Mac/Unix environments
# - if using multiline commands, the \ character MUST be the LAST character on the line, 
#   else you will get errors

opal-export.sh --config-file "${OPAL_TOOLS_HOME_DIR}/conf/opal-export.conf" \
    --connection-pool-name #SCHEMA# \
    --connection-pool-file "${OPAL_TOOLS_USER_CONFIG_DIR}/connections-#ENV#.json"
