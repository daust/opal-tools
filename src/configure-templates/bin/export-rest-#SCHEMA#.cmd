@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

@REM Notes for shell scripting:
@REM - if * is specified without characters around it (* vs. LOG* or *LOG) it needs to be put in quotes for the shell: "*"
@REM - if using multiline commands, the ^ character MUST be the LAST character on the line, else you will get errors

@REM go to the right directory for exporing APEX and/or REST applications through the 
@REM script: opal-tools/export-scripts/opal-export-post-script.sql
cd "%OPAL_TOOLS_SRC_REST_DIR%"

SET CURRDIR=%~dp0

@REM don't export schema objects (--skip-export), only run the pre-script and post-script
@call opal-export.cmd --config-file "%OPAL_TOOLS_HOME_DIR%\conf\opal-export.conf" ^
                      --pre-scripts "%OPAL_TOOLS_HOME_DIR%\export-scripts\opal-export-pre-script.sql" ^
                      --post-scripts "%CURRDIR%\export-rest-#SCHEMA#.sql" ^
                      --connection-pool-name #SCHEMA# ^
                      --silent ^
                      --connection-pool-file "%OPAL_TOOLS_USER_CONFIG_DIR%\connections-#ENV#.json" ^
                      --skip-export

start explorer "%OPAL_TOOLS_SRC_REST_DIR%"
cd /d "%OPAL_TOOLS_SRC_REST_DIR%"

cmd /k
