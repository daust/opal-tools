#!/bin/bash
source #OPAL_TOOLS_USER_ENV_SCRIPT#

opal-export.sh --output-dir "$OPAL_TOOLS_SRC_SQL_DIR" \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" \
    --pre-scripts ${OPAL_TOOLS_HOME_DIR}/conf/opal-export-pre-script.sql \
    --post-scripts ${OPAL_TOOLS_HOME_DIR}/conf/opal-export-post-script.sql \
    --skip-errors \
    --excludes SYS_* AQ$* \
    --extension-mappings package:pks "package body:pkb" \
    --directory-mappings "package body:packages" \
    --connection-pool-name #SCHEMA# \
    --connection-pool-file ${OPAL_TOOLS_USER_CONFIG_DIR}/connections-#ENV#.json

