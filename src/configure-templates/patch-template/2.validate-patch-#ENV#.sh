#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SCRIPT_DIR=`dirname $0`

# Pick shell tput setaf using command "tput setaf color"
# e.g. green foreground: "tput setaf 2"
# 
# green : "tput setaf 2"
# yellow: "tput setaf 3"
# red   : "tput setaf 1"

#OPAL_TOOLS_SET_COLOR_COMMAND#

opal-install.sh --validate-only --config-file "${SCRIPT_DIR}/opal-installer.json" --connection-pool-file "${OPAL_TOOLS_USER_CONFIG_DIR}/connections-#ENV#.json"
