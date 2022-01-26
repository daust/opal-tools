@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SET CURRDIR=%~dp0

@call opal-export-scripts-for-manual-install.cmd    --config-file "%CURRDIR%opal-installer.json" ^
                                                    --defaults-config-file "%OPAL_TOOLS_HOME_DIR%\conf\opal-installer.json" ^
                                                    --zip-file "#PARENT_FOLDER_NAME#.zip" ^
                                                    --mandatory-attributes application patch author ^
                                                    --source-list-file SourceFilesReference.conf ^
                                                    --source-dir "%OPAL_TOOLS_SRC_SQL_DIR%" ^
                                                    --silent ^
                                                    --zip-include-files "%OPAL_TOOLS_HOME_DIR%\export-scripts\prompt.sql" ReleaseNotes.txt

@call opal-export-scripts-for-manual-install.cmd    --config-file "%CURRDIR%opal-installer.json" ^
                                                    --defaults-config-file "%OPAL_TOOLS_HOME_DIR%\conf\opal-installer.json" ^
                                                    --zip-file "#PARENT_FOLDER_NAME#-with-prompts.zip" ^
                                                    --mandatory-attributes application patch author ^
                                                    --source-list-file SourceFilesReference.conf ^
                                                    --source-dir "%OPAL_TOOLS_SRC_SQL_DIR%" ^
                                                    --zip-include-files "%OPAL_TOOLS_HOME_DIR%\export-scripts\prompt.sql" ReleaseNotes.txt

@call opal-export-scripts-for-manual-install.cmd    --config-file "%CURRDIR%opal-installer.json" ^
                                                    --defaults-config-file "%OPAL_TOOLS_HOME_DIR%\conf\opal-installer.json" ^
                                                    --zip-file "#PARENT_FOLDER_NAME#-utf8.zip" ^
													--convert-files-to-utf8 ^
                                                    --mandatory-attributes application patch author ^
                                                    --source-list-file SourceFilesReference.conf ^
                                                    --source-dir "%OPAL_TOOLS_SRC_SQL_DIR%" ^
                                                    --silent ^
                                                    --zip-include-files "%OPAL_TOOLS_HOME_DIR%\export-scripts\prompt.sql" ReleaseNotes.txt

cmd /k
