@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SET CURRDIR=%~dp0

@call opal-install-copy-source-files.cmd --source-dir "%OPAL_TOOLS_SRC_SQL_DIR%" ^
                                         --target-dir "%CURRDIR%sql" ^
                                         --source-list-file SourceFilesCopy.txt

cmd /k
