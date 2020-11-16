#!/bin/bash
#--------------------------------------------------------------------------
# User specific configuration
#--------------------------------------------------------------------------
export PROJECT_ROOT="#PROJECT_ROOT#"
export OPAL_TOOLS_USER_IDENTITY="#OPAL_TOOLS_USER_IDENTITY#"

# --- change if needed ---
# export ORACLE_HOME=c:\Progs\Oracle\Client\12.1\Home
# export JAVA_HOME=c:\Program Files (x86)\Java\jdk1.8.0_251 

#--------------------------------------------------------------------------
# General configuration settings, typically unchanged 
#--------------------------------------------------------------------------
export OPAL_TOOLS_HOME_DIR="#OPAL_TOOLS_HOME_DIR#"
export OPAL_TOOLS_JAVA_ARGS="-Dlog4j.configurationFile=${OPAL_TOOLS_HOME_DIR}/conf/log4j2.xml -Djava.util.logging.config.file=${OPAL_TOOLS_HOME_DIR}/conf/log4j.properties -Dfile.encoding=#FILE.ENCODING#"

# for debugging, use these log file settings
# export OPAL_TOOLS_JAVA_ARGS="-Dlog4j.configurationFile=${OPAL_TOOLS_HOME_DIR}/conf/log4j2-debug.xml -Djava.util.logging.config.file=${OPAL_TOOLS_HOME_DIR}/conf/log4j-debug.properties -Dfile.encoding=#FILE.ENCODING#"

export OPAL_TOOLS_USER_CONFIG_DIR="#OPAL_TOOLS_USER_CONFIG_DIR#"
export OPAL_TOOLS_SRC_SQL_DIR="#OPAL_TOOLS_SRC_SQL_DIR#"
export OPAL_TOOLS_PATCH_TEMPLATE_DIR="#OPAL_TOOLS_PATCH_TEMPLATE_DIR#"
export OPAL_TOOLS_PATCH_DIR="#OPAL_TOOLS_PATCH_DIR#"

# --- typically unchanged ---
# Date Strings to be used in scripts
export YEAR=`date +'%Y'`
export DATE_STRING=`date +"%Y-%m-%d"`
#echo DATE_STRING: $DATE_STRING
#echo YEAR: $YEAR

export PATH="${OPAL_TOOLS_HOME_DIR}/bin":${PATH}
