@call #OPAL_TOOLS_USER_ENV_SCRIPT#

SET CURRDIR=%~dp0

@call opal-install-copy-patch-files.cmd --source-path "%OPAL_TOOLS_SRC_SQL_DIR%" --target-path "%CURRDIR%sql" --patch-file-name PatchFiles.txt

cmd /k
