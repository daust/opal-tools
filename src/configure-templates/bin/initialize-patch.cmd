@call #OPAL_INSTALLER_HOME_DIR#\bin\set-script-environment.cmd

@REM setting up the target patch directory, e.g. \Patches\2020\2020-04-23-<patch name>
SET /P PATCH_NAME=Patch name, e.g. fac_123 or KapaPlaner-v2.5.0 : 
set PATCH_DIRECTORY=%OPAL_INSTALLER_PATCH_DIR%\%YEAR%\%DATE_STRING%-%PATCH_NAME%

@REM calling the initialization and copying the the template file structure to the target 
@REM patch directory
@call opal-installer.cmd initPatch "%PATCH_DIRECTORY%" "%OPAL_INSTALLER_PATCH_TEMPLATE_DIR%"

start explorer "%PATCH_DIRECTORY%"
cd /d "%PATCH_DIRECTORY%"

cmd /k

