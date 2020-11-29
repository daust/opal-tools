 #!/bin/bash
source "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SCRIPT_DIR=`dirname $0`
#@REM Yellow forground color for Integration / T&A
#color 0E
opal-install-copy-source-files.sh --source-path "$OPAL_TOOLS_SRC_SQL_DIR" \
                                  --target-path "${SCRIPT_DIR}/sql" \
                                  --patch-file-name "${SCRIPT_DIR}/SourceFilesCopy.txt"
