@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

@REM Notes for shell scripting:
@REM - if * is specified without characters around it (* vs. LOG* or *LOG) it needs to be put in quotes for the shell: "*"
@REM - if using multiline commands, the ^ character MUST be the LAST character on the line, else you will get errors

@REM setting up the filter for exporting sources
@echo "Object wildcards can be * or %%. The %%-sign needs to be escaped for the shell: %% => %%%%"
SET /P FILTER=Filter for DDL object names including wildcards, e.g. "xlib*" or "xlib%%%%": 

@call opal-export.cmd --output-dir "%OPAL_TOOLS_SRC_SQL_DIR%" ^
    --dependent-objects table:comment,index,object_grant view:comment,object_grant,trigger "materialized view:comment,index,materialized_view_log,object_grant" ^
    --pre-scripts "%OPAL_TOOLS_HOME_DIR%/conf/opal-export-pre-script.sql" ^
    --post-scripts "%OPAL_TOOLS_HOME_DIR%/conf/opal-export-post-script.sql" ^
    --skip-errors ^
    --includes %FILTER% ^
    --excludes SYS_YOID* SYS_PLSQL* AQ$*  ^
    --exclude-types LOB "TABLE_PARTITION" "INDEX PARTITION" "JAVA CLASS" JAVA "JAVA RESOURCE" INDEX ^
    --extension-mappings package:pks "package body:pkb" ^
    --directory-mappings "package body:packages" ^
    --connection-pool-name #SCHEMA# ^
    --connection-pool-file "%OPAL_TOOLS_USER_CONFIG_DIR%\connections-#ENV#.json"

start explorer "%OPAL_TOOLS_SRC_SQL_DIR%\#SCHEMA#"
cd /d "%OPAL_TOOLS_SRC_SQL_DIR%\#SCHEMA#"

cmd /k
