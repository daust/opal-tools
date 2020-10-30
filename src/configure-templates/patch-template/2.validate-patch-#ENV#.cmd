@call #OPAL_INSTALLER_USER_ENV_SCRIPT#

SET CURRDIR=%~dp0

@REM Pick shell color using command "color <background><foreground>"
@REM e.g. black background with green foreground: "color 0A"
@REM call help for details "color ?"
@REM 
@REM green : "color 0A"
@REM yellow: "color 0E"
@REM red   : "color 0C"

#OPAL_INSTALLER_SET_COLOR_COMMAND#

@call opal-installer.cmd executePatch VALIDATE_ONLY "%CURRDIR%opal-installer.json" "%OPAL_INSTALLER_USER_CONFIG_DIR%\connections-#ENV#.json"

cmd /k
