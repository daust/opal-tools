@REM DEBUG: For debugging, you can turn on ECHO again ... and also OPAL_TOOLS_JAVA_ARGS below
@echo OFF

@REM --------------------------------------------------------------------------
@REM User specific configuration
@REM --------------------------------------------------------------------------
set PROJECT_ROOT=#PROJECT_ROOT#
set OPAL_TOOLS_USER_IDENTITY=#OPAL_TOOLS_USER_IDENTITY#
set OPAL_TOOLS_USER_CONFIG_DIR=#OPAL_TOOLS_USER_CONFIG_DIR#

@REM --- change if needed ---
@REM set ORACLE_HOME=c:\Progs\Oracle\Client\12.1\Home
@REM it MUST be a JDK, a JRE is NOT ENOUGH
@REM set JAVA_HOME=c:\Program Files (x86)\Java\jdk1.8.0_251

@REM --------------------------------------------------------------------------
@REM General configuration settings, typically unchanged
@REM --------------------------------------------------------------------------
set OPAL_TOOLS_HOME_DIR=#OPAL_TOOLS_HOME_DIR#
set OPAL_TOOLS_JAVA_ARGS=-Dlog4j.configurationFile="%OPAL_TOOLS_HOME_DIR%\conf\log4j2.xml" -Djava.util.logging.config.file="%OPAL_TOOLS_HOME_DIR%\conf\log4j.properties" -Dfile.encoding=#FILE.ENCODING#

@REM DEBUG: for debugging, use these log file settings
@REM set OPAL_TOOLS_JAVA_ARGS=-Dlog4j.configurationFile="%OPAL_TOOLS_HOME_DIR%\conf\log4j2-debug.xml" -Djava.util.logging.config.file="%OPAL_TOOLS_HOME_DIR%\conf\log4j-debug.properties" -Dfile.encoding=#FILE.ENCODING#

set OPAL_TOOLS_SRC_SQL_DIR=#OPAL_TOOLS_SRC_SQL_DIR#
set OPAL_TOOLS_PATCH_TEMPLATE_DIR=#OPAL_TOOLS_PATCH_TEMPLATE_DIR#
set OPAL_TOOLS_PATCH_DIR=#OPAL_TOOLS_PATCH_DIR#

@REM --- typically unchanged ---
@REM Date Strings to be used in scripts
@REM DATE_STRING=YYYY-MM-DD
@REM YEAR=YYYY

FOR /f %%a in ('WMIC OS GET LocalDateTime ^| find "."') DO set DTS=%%a
set DATE_STRING=%DTS:~0,4%-%DTS:~4,2%-%DTS:~6,2%
set YEAR=%DTS:~0,4%
@REM echo DATE_STRING: %DATE_STRING%
@REM echo YEAR: %YEAR%

set PATH=%OPAL_TOOLS_HOME_DIR%\bin;%PATH%


