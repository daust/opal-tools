@REM when being called directly, initialize the environment first by
@REM calling the script set-script-environment.cmd in the same directory as the script itself.
SET SCRIPT_DIR=%~dp0
@call %SCRIPT_DIR%\set-script-environment.cmd

java %OPAL_INSTALLER_JAVA_ARGS% -cp "%OPAL_INSTALLER_HOME_DIR%\lib\*" de.opal.Main %*
