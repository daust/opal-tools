@call #OPAL_INSTALLER_HOME_DIR#\bin\set-script-environment.cmd

SET CURRDIR=%~dp0

@call opal-installer.cmd copyPatchFiles "%CURRDIR%sql" "%OPAL_INSTALLER_SRC_SQL_DIR%" PatchFiles.txt

cmd /k

