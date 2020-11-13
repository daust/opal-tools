#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

# Notes for shell scripting:
# - due to shell expansion, the wildcard % is preferred to * on Linux/Mac/Unix environments
# - if using multiline commands, the \ character MUST be the LAST character on the line, 
#   else you will get errors

read -p 'Filter for DDL object names including wildcards, e.g. "xlib%" or "xlib*" : ' FILTER

opal-export.sh --output-dir "$OPAL_TOOLS_SRC_SQL_DIR" \
    --dependent-objects table:comment,index,object_grant view:comment,object_grant,trigger "materialized view:comment,index,materialized_view_log,object_grant" \
    --pre-scripts "${OPAL_TOOLS_HOME_DIR}/export-scripts/opal-export-pre-script.sql" \
    --post-scripts "${OPAL_TOOLS_HOME_DIR}/export-scripts/opal-export-post-script.sql" \
    --skip-errors \
    --includes ${FILTER} \
    --excludes SYS_YOID% SYS_PLSQL% AQ$%  \
    --exclude-types LOB "TABLE_PARTITION" "INDEX PARTITION" "JAVA CLASS" JAVA "JAVA RESOURCE" INDEX \
    --filename-templates default:#schema#/#object_type_plural#/#object_name#.sql package:#schema#/packages/#object_name#.pks "package body:#schema#/packages/#object_name#.pkb" \
    --connection-pool-name #SCHEMA# \
    --connection-pool-file "${OPAL_TOOLS_USER_CONFIG_DIR}/connections-#ENV#.json" \
    --export-template-dir "${OPAL_TOOLS_HOME_DIR}/export-templates"

