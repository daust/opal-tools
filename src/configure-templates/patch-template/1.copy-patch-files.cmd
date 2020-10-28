@call #OPAL_INSTALLER_USER_ENV_SCRIPT#

SET CURRDIR=%~dp0

@call opal-installer.cmd copyPatchFiles "%CURRDIR%sql" "%OPAL_INSTALLER_SRC_SQL_DIR%" PatchFiles.txt

cmd /k

