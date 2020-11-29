@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SET CURRDIR=%~dp0

@call opal-install-copy-source-files.cmd --source-path "%OPAL_TOOLS_SRC_SQL_DIR%" ^
                                         --target-path "%CURRDIR%sql" ^
                                         --patch-file-name SourceFilesCopy.txt

cmd /k
