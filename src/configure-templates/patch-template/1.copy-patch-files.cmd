@call #OPAL_TOOLS_USER_ENV_SCRIPT#

SET CURRDIR=%~dp0

@call opal-installer.cmd copyPatchFiles "%CURRDIR%sql" "%OPAL_TOOLS_SRC_SQL_DIR%" PatchFiles.txt

cmd /k

