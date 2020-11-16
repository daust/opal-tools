@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

@REM Notes for shell scripting:
@REM - if * is specified without characters around it (* vs. LOG* or *LOG) it needs to be put in quotes for the shell: "*"
@REM - if using multiline commands, the ^ character MUST be the LAST character on the line, else you will get errors

@REM setting up the filter for exporting sources
@echo "Object wildcards can be * or %%. The %%-sign needs to be escaped for the shell: %% => %%%%"
SET /P FILTER=Filter for DDL object names including wildcards, e.g. "xlib*" or "xlib%%%%": 

@call opal-export.cmd --config-file "%OPAL_TOOLS_HOME_DIR%\conf\opal-export.conf" ^
                      --connection-pool-name #SCHEMA# ^
                      --connection-pool-file "%OPAL_TOOLS_USER_CONFIG_DIR%\connections-#ENV#.json" ^
                      --includes %FILTER%

start explorer "%OPAL_TOOLS_SRC_SQL_DIR%\#SCHEMA#"
cd /d "%OPAL_TOOLS_SRC_SQL_DIR%\#SCHEMA#"

cmd /k
