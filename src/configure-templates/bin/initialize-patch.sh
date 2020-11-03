#!/bin/bash
source #OPAL_INSTALLER_USER_ENV_SCRIPT#

# setting up the target patch directory, e.g. \Patches\2020\2020-04-23-<patch name>
read -p 'Patch name, e.g. jira_123 or Project-v2.5.0 : ' PATCH_NAME
export PATCH_DIRECTORY=${OPAL_INSTALLER_PATCH_DIR}/${YEAR}/${DATE_STRING}-${PATCH_NAME}

# calling the initialization and copying the the template file structure to the target 
# patch directory
opal-installer.sh initPatch "${PATCH_DIRECTORY}" "${OPAL_INSTALLER_PATCH_TEMPLATE_DIR}"

echo
echo "To open the finder/explorer in new window automatically, add in this file:"  
echo "-------------------------------------------------------------------------:"
echo "  on MacOS: open \"\$PATCH_DIRECTORY\""
echo "  on Linux: nautilus \"\$PATCH_DIRECTORY\" &"
echo


