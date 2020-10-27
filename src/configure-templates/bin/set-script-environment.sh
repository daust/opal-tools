#!/bin/bash
#--------------------------------------------------------------------------
# setting important environment variables for the scripts to be used. 
#--------------------------------------------------------------------------

# export variables used in the various scripts for the installer
export OPAL_INSTALLER_HOME_DIR=#OPAL_INSTALLER_HOME_DIR#
export OPAL_INSTALLER_JAVA_ARGS="-Dlog4j.configurationFile=${OPAL_INSTALLER_HOME_DIR}/conf/log4j2.xml -Djava.util.logging.config.file=${OPAL_INSTALLER_HOME_DIR}/conf/log4j.properties"

# for debugging, use these log file settings
# export OPAL_INSTALLER_JAVA_ARGS="-Dlog4j.configurationFile=${OPAL_INSTALLER_HOME_DIR}/conf/log4j2-debug.xml -Djava.util.logging.config.file=${OPAL_INSTALLER_HOME_DIR}/conf/log4j-debug.properties"

export OPAL_INSTALLER_USER_CONFIG_DIR=#OPAL_INSTALLER_USER_CONFIG_DIR#
export OPAL_INSTALLER_SRC_SQL_DIR=#OPAL_INSTALLER_SRC_SQL_DIR#
export OPAL_INSTALLER_PATCH_TEMPLATE_DIR=#OPAL_INSTALLER_PATCH_TEMPLATE_DIR#
export OPAL_INSTALLER_PATCH_DIR=#OPAL_INSTALLER_PATCH_DIR#

#@REM call user specific settings for java environment or other user specific variables
source ${OPAL_INSTALLER_USER_CONFIG_DIR}/setUserEnvironment.sh

# --- typically unchanged ---
# Date Strings to be used in scripts
export YEAR=`date +'%Y'`
export DATE_STRING=`date +"%Y-%m-%d"`
#echo DATE_STRING: $DATE_STRING
#echo YEAR: $YEAR

export PATH=${OPAL_INSTALLER_HOME_DIR}/bin:${PATH}




