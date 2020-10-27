@REM --------------------------------------------------------------------------
@REM setting important environment variables for the scripts to be used. 
@REM --------------------------------------------------------------------------

@REM For debugging, you can turn on ECHO again
@echo OFF

@REM set variables used in the various scripts for the installer
set OPAL_INSTALLER_HOME_DIR=#OPAL_INSTALLER_HOME_DIR#
set OPAL_INSTALLER_JAVA_ARGS=-Dlog4j.configurationFile=%OPAL_INSTALLER_HOME_DIR%\conf\log4j2.xml -Djava.util.logging.config.file=%OPAL_INSTALLER_HOME_DIR%\conf\log4j.properties

@REM for debugging, use these log file settings
@REM set OPAL_INSTALLER_JAVA_ARGS=-Dlog4j.configurationFile=%OPAL_INSTALLER_HOME_DIR%\conf\log4j2-debug.xml -Djava.util.logging.config.file=%OPAL_INSTALLER_HOME_DIR%\conf\log4j-debug.properties

set OPAL_INSTALLER_USER_CONFIG_DIR=#OPAL_INSTALLER_USER_CONFIG_DIR#
set OPAL_INSTALLER_SRC_SQL_DIR=#OPAL_INSTALLER_SRC_SQL_DIR#
set OPAL_INSTALLER_PATCH_TEMPLATE_DIR=#OPAL_INSTALLER_PATCH_TEMPLATE_DIR#
set OPAL_INSTALLER_PATCH_DIR=#OPAL_INSTALLER_PATCH_DIR#

@REM call user specific settings for java environment or other user specific variables
call %OPAL_INSTALLER_USER_CONFIG_DIR%\setUserEnvironment.cmd

@REM --- typically unchanged ---
@REM Date Strings to be used in scripts
@REM DATE_STRING=YYYY-MM-DD
@REM YEAR=YYYY

FOR /f %%a in ('WMIC OS GET LocalDateTime ^| find "."') DO set DTS=%%a
set DATE_STRING=%DTS:~0,4%-%DTS:~4,2%-%DTS:~6,2%
set YEAR=%DTS:~0,4%
echo DATE_STRING: %DATE_STRING%
echo YEAR: %YEAR%

set PATH=%OPAL_INSTALLER_HOME_DIR%\bin;%PATH%

