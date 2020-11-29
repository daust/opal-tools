@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

SET CURRDIR=%~dp0

@REM Pick shell color using command "color <background><foreground>"
@REM e.g. black background with green foreground: "color 0A"
@REM call help for details "color ?"
@REM 
@REM green : "color 0A"
@REM yellow: "color 0E"
@REM red   : "color 0C"

#OPAL_TOOLS_SET_COLOR_COMMAND#

@call opal-install.cmd --config-file "%CURRDIR%opal-installer.json" ^
                       --connection-pool-file "%OPAL_TOOLS_USER_CONFIG_DIR%\connections-#ENV#.json" ^
                       --mandatory-attributes application patch author version ^
                       --source-files-filename SourceFilesReference.txt ^
                       --source-dir "%OPAL_TOOLS_SRC_SQL_DIR%" 

cmd /k
