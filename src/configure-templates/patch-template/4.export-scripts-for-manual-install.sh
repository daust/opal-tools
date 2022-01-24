#!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SCRIPT_DIR=`dirname $0`

opal-export-scripts-for-manual-install.sh   --config-file "${SCRIPT_DIR}/opal-installer.json" \
                                            --defaults-config-file "${OPAL_TOOLS_HOME_DIR}/conf/opal-installer.json" \
                                            --zip-file "#PARENT_FOLDER_NAME#.zip" \
                                            --mandatory-attributes application patch author version \
                                            --source-list-file SourceFilesReference.conf \
                                            --source-dir "${OPAL_TOOLS_SRC_SQL_DIR}" \
                                            --silent \
                                            --zip-include-files "${OPAL_TOOLS_HOME_DIR}/export-scripts/prompt.sql" ReleaseNotes.txt 

opal-export-scripts-for-manual-install.sh   --config-file "${SCRIPT_DIR}/opal-installer.json" \
                                            --defaults-config-file "${OPAL_TOOLS_HOME_DIR}/conf/opal-installer.json" \
                                            --zip-file "#PARENT_FOLDER_NAME#-utf8.zip" \
                                            --convert-files-to-utf8 \
                                            --mandatory-attributes application patch author version \
                                            --source-list-file SourceFilesReference.conf \
                                            --source-dir "${OPAL_TOOLS_SRC_SQL_DIR}" \
                                            --silent \
                                            --zip-include-files "${OPAL_TOOLS_HOME_DIR}/export-scripts/prompt.sql" ReleaseNotes.txt 

opal-export-scripts-for-manual-install.sh   --config-file "${SCRIPT_DIR}/opal-installer.json" \
                                            --defaults-config-file "${OPAL_TOOLS_HOME_DIR}/conf/opal-installer.json" \
                                            --zip-file "#PARENT_FOLDER_NAME#-with-prompts.zip" \
                                            --mandatory-attributes application patch author version \
                                            --source-list-file SourceFilesReference.conf \
                                            --source-dir "${OPAL_TOOLS_SRC_SQL_DIR}" \
                                            --zip-include-files "${OPAL_TOOLS_HOME_DIR}/export-scripts/prompt.sql" ReleaseNotes.txt
