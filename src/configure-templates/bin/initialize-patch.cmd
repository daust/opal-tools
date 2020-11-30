@call "#OPAL_TOOLS_USER_ENV_SCRIPT#"

@REM setting up the target patch directory, e.g. \Patches\2020\2020-04-23-<patch name>
SET /P PATCH_NAME=Patch name, e.g. jira_123 or Project-v2.5.0: 
set PATCH_DIRECTORY=%OPAL_TOOLS_PATCH_DIR%\%YEAR%\%DATE_STRING%-%PATCH_NAME%

@REM calling the initialization and copying the the template file structure to the target 
@REM patch directory
@call opal-install-copy-template.cmd --source-dir "%OPAL_TOOLS_PATCH_TEMPLATE_DIR%" --target-dir "%PATCH_DIRECTORY%"

start explorer "%PATCH_DIRECTORY%"
cd /d "%PATCH_DIRECTORY%"

cmd /k

