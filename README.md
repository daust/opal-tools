# What is the opal-installer?

After 20+ years of developing Oracle software based on SQL, PL/SQL and APEX I have finally implemented a flexible and highly configurable mechanism to move code and code changes from one environment to the next. It has to be easy to use and easy to customize, because the file system layout is different in every project. 

The installer uses SQLcl under the hood to actually run the SQL scripts. The core engine is very simple. It will execute SQL files which it finds in the filesystem. 

It uses regular expressions in order to figure out a mapping between a file system path and the matching connection pool. 

It can be configured in multiple ways so that there is no requirement for a specific layout of the filesystem. 

It should work for most operating systems, I have tested it on Windows, MacOS and Linux. 

# Download

The files can be downloaded here: [https://github.com/daust/opal-installer/releases](https://github.com/daust/opal-installer/releases).

# Setup

Once downloaded and unzipped you run the command ``setup.sh`` or ``setup.cmd``. This will copy and customize the appropriate files. 

During the setup process you will be prompted to enter specific file locations (directories) so that you can fully customize the environment. For starters it is recommended to use the defaults and get familiar with it. 

The prompts are: 
* ``Project root directory, typically the target of a GIT or SVN export``: 
    - In most cases you will use a central directory and all files for this project will be stored in subdirectories. 
    - Also, when using subversion, GIT or a similar tool you might have multiple exports of the same project in different locations. 
* ``SW install directory (contains bin and lib directories, use '.' for local files)``: 
    - Here we will install the actual software itself and create the subdirectories bin, conf and lib.
* ``Patch template directory``: 
    - This directory and all files in it will be copied into a new patch. 
* ``Local configuration directory (connection pools, user dependent config)``: 
    - Here the installer will find the connection pools for this user environment. 
    - Typically, this is NOT part of the SVN or GIT repository because it contains passwords and also might be different for each user. 
* ``Local script to initialize the user environment for this project``: 
    - This script will include all locations and store them in variables. Only those variables are used in the generated scripts. 
    - Thus, all you need to change is the contents of this file. 
    - When you are developing with multiple developer you can : 
        - choose to store this file ON EACH developer machine in the same location, then you enter a fully qualified path, e.g.: ``c:\local-config\setProjectEnvironment.cmd``
        - choose to change the %PATH% variable and add this script to the path, then you enter the script name without any path, e.g. ``setProjectEnvironment.cmd``
* ``Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.)``:
    - in this directory you will store the sources for the project
* ``Patch directory (patches, has subdirectories e.g. year/patch_name)``:
    - in this directory we will generate the new patch directories. The default layout is ``patches\<year>\<year-month-day>-<patch name>``, it can be changed in the file ``bin\initializePatch.cmd``
* ``List of database schemas (comma-separated, e.g. HR,SCOTT)``:
    - how many different schemas do we want to install into? 
    - This comma separated list will be used to generate the connection pool files. 
* ``List of environments (comma-separated, e.g. DEV,INT,PROD)``:
    - how many different environments do we want to install into? 
    - This comma separated list will be used to generate the connection pool files. 

Running the setup: 
* Windows: 
    ```
    setup.cmd <project root directory>
    e.g.
    setup.cmd c:\app\opal-installer
    ```

## Setup example for Windows

In this example we will start the ``setup.cmd`` script with the parameter ``c:\app\opal-installer``, this will be used as the default directory and most others will be relative to this one. 
The generated file locations are ONLY suggestions, you can change all of them. 

When using ``<ENTER>`` you will accept the default which is specified in brackets, e.g.: ``[c:\app\opal-installer]``. 

In this sample we will:
- use ``c:\app\opal-installer`` as the project root,
- store the installation files in 
    - ``c:\app\opal-installer\opal-installer\bin``
    - ``c:\app\opal-installer\opal-installer\conf``
    - ``c:\app\opal-installer\opal-installer\lib``
- store the patch-template files in 
    - ``c:\app\opal-installer\patch-template``
- store the generated patches in 
    - ``c:\app\opal-installer\patches``
    - ``c:\app\opal-installer\patches\<year>\<year-month-day>-<patch name>``
        - e.g.: ``C:\app\opal-installer\patches\2020\2020-10-29-test``
- use one connection pool named ``jri_test``
- install on two environments named ``dev`` and ``int`` with the respective connection pool files (each one having a definition for ``jri_test``)

<pre style="overflow-x: auto; white-space: pre-wrap; white-space: -moz-pre-wrap; white-space: -pre-wrap; white-space: -o-pre-wrap; word-wrap: break-word;">
c:\app\opal-installer-1.1.0-sqlcl-20.2.0><b>setup.cmd c:\app\opal-installer</b>

Project root directory, typically the target of a GIT or SVN export [c:\app\opal-installer]:

SW install directory (contains bin and lib directories, use '.' for local files) [%PROJECT_ROOT%\opal-installer]:

Patch template directory [%PROJECT_ROOT%\patch-template]:

Local configuration directory (connection pools, user dependent config) [c:\app\opal-installer\conf-user]: <b>c:\opal-installer-local</b>

Local script to initialize the user environment for this project [c:\opal-installer-local\setProjectEnvironment.cmd]: <b>c:\opal-installer-local\setProjectEnvironment-project1.cmd</b>

Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.) [%PROJECT_ROOT%\sql]:

Patch directory (patches, has subdirectories e.g. year/patch_name) [%PROJECT_ROOT%\patches]:

List of database schemas (comma-separated, e.g. HR,SCOTT) [HR,SCOTT]: <b>jri_test</b>

List of environments (comma-separated, e.g. DEV,INT,PROD) [DEV,INT,PROD]: <b>dev,int</b>

Please press <enter> to proceed ...

----------------------------------------------------------
copy sw files from: C:\app\opal-installer-1.1.0-sqlcl-20.2.0\lib
              to  : C:\app\opal-installer\opal-installer\lib
----------------------------------------------------------
copy sw files from :C:\app\opal-installer-1.1.0-sqlcl-20.2.0\conf
              to  : C:\app\opal-installer\opal-installer\conf

copy template directory from: C:\app\opal-installer-1.1.0-sqlcl-20.2.0\configure-templates\patch-template
                        to  : C:\app\opal-installer\patch-template

  process file 1.copy-patch-files.cmd
  process file 2.validate-patch-#ENV#.cmd
  process file 3.install-patch-#ENV#.cmd
  process file opal-installer.json
  process file PatchFiles-body.txt
  process file PatchFiles-header.txt
  process file ReleaseNotes.txt

process local conf directory in: C:\opal-installer-local

  Process environment: dev => C:\opal-installer-local\connections-dev.json
    JDBC url for environment dev:  [jdbc:oracle:thin:@127.0.0.1:1521:xe]:
    Password for schema jri_test in environment dev:  []: 
  Process environment: int => C:\opal-installer-local\connections-int.json
    JDBC url for environment int:  [jdbc:oracle:thin:@127.0.0.1:1521:xe]:
    Password for schema jri_test in environment int:  []: 

db source directory from: C:\app\opal-installer-1.1.0-sqlcl-20.2.0\configure-templates\src-sql
                    to  : %PROJECT_ROOT%\sql

patch directory from: c:\app\opal-installer-1.1.0-sqlcl-20.2.0\patches
                to  : C:\app\opal-installer\patches

process bin directory

  process file initialize-patch.cmd
  process file opal-installer.cmd

c:\app\opal-installer-1.1.0-sqlcl-20.2.0>
</pre>

All environment variables are set up in the "Local script to initialize the user environment", e.g.: ``c:\opal-installer-local\setProjectEnvironment-project1.cmd``: 

<pre style="overflow-x: auto; white-space: pre-wrap; white-space: -moz-pre-wrap; white-space: -pre-wrap; white-space: -o-pre-wrap; word-wrap: break-word;">
@REM --------------------------------------------------------------------------
@REM setting important environment variables for the scripts to be used. 
@REM --------------------------------------------------------------------------

@REM --- change if needed ---
@REM set ORACLE_HOME=c:\Progs\Oracle\Client\12.1\Home
@REM it MUST be a JDK, a JRE is NOT ENOUGH
@REM set JAVA_HOME=c:\Program Files (x86)\Java\jdk1.8.0_251

@REM DEBUG: For debugging, you can turn on ECHO again ... and also OPAL_INSTALLER_JAVA_ARGS below
@echo OFF

@REM set variables used in the various scripts for the installer
set PROJECT_ROOT=c:\app\opal-installer
set OPAL_INSTALLER_HOME_DIR=%PROJECT_ROOT%\opal-installer
set OPAL_INSTALLER_JAVA_ARGS=-Dlog4j.configurationFile=%OPAL_INSTALLER_HOME_DIR%\conf\log4j2.xml -Djava.util.logging.config.file=%OPAL_INSTALLER_HOME_DIR%\conf\log4j.properties

@REM DEBUG: for debugging, use these log file settings
@REM set OPAL_INSTALLER_JAVA_ARGS=-Dlog4j.configurationFile=%OPAL_INSTALLER_HOME_DIR%\conf\log4j2-debug.xml -Djava.util.logging.config.file=%OPAL_INSTALLER_HOME_DIR%\conf\log4j-debug.properties

set OPAL_INSTALLER_USER_CONFIG_DIR=c:\app\opal-installer-local
set OPAL_INSTALLER_SRC_SQL_DIR=%PROJECT_ROOT%\sql#
set OPAL_INSTALLER_PATCH_TEMPLATE_DIR=%PROJECT_ROOT%\patch-template
set OPAL_INSTALLER_PATCH_DIR=%PROJECT_ROOT%\patches

@REM --- typically unchanged ---
@REM Date Strings to be used in scripts
@REM DATE_STRING=YYYY-MM-DD
@REM YEAR=YYYY

FOR /f %%a in ('WMIC OS GET LocalDateTime ^| find "."') DO set DTS=%%a
set DATE_STRING=%DTS:~0,4%-%DTS:~4,2%-%DTS:~6,2%
set YEAR=%DTS:~0,4%
@REM echo DATE_STRING: %DATE_STRING%
@REM echo YEAR: %YEAR%

set PATH=%OPAL_INSTALLER_HOME_DIR%\bin;%PATH%
</pre>

# Usage

## 1. Initialize a new patch

*tbd*

## 2. Configure the opal-installer.json file

*tbd*

## 3. Put all files into the subdirectories of the patch directory

*tbd*

## 4. Execute the batch files in the patch directory 

*tbd*

# Configuration

## ``opal-installer.json`` configuration file

*tbd*

# Troubleshooting

### Warning message: ``Unable to get Charset 'cp65001' for property 'sun.stdout.encoding', using default windows-1250 and continuing``.

This message only indicates that the character set for the console output cannot be determined from the current shell environment. See more details and workarounds here: https://github.com/daust/opal-installer/issues/8. 

# Special use cases

### Using the installer with multiple developers on the same operating system

*tbd*

### Using the installer with multiple developers on different operating systems 

*tbd*
