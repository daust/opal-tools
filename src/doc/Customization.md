* [Setup](#setup)
* [About Shell Scripting](#about-shell-scripting)
* [About File Encodings](#about-file-encodings)
* [About Java Regular Expressions](#about-java-regular-expressions)
* [opal-install](#opal-install)
  * [Command Line](#command-line)
  * [Database Log Tables](#database-log-tables)
* [opal-export](#opal-export)
  * [Command Line](#command-line-1)
  * [Shared Command Line Switches](#shared-command-line-switches)
* [Configuration Files](#configuration-files)
  * [Connection Pool Files](#connection-pool-files)
  * [opal-installer.json](#opal-installerjson)
  * [ReleaseNotes.txt](#releasenotestxt)
  * [SourceFilesCopy.conf](#sourcefilescopyconf)
  * [SourceFilesReference.conf](#sourcefilesreferenceconf)
* [Special Use Cases](#special-use-cases)

# Setup

Once downloaded and unzipped you run the command ``setup.sh`` or ``setup.cmd``. This will copy and customize the appropriate files. 

During the setup process you will be prompted to enter specific file locations (directories) so that you can fully customize the environment. For starters it is recommended to use the defaults and get familiar with it. 

The setup command comes with command line options, the minimum parameter is ``--project-root-dir``, but you can pass almost all parameters on the command line: 

```
 -s (--show-passwords)                             : when prompted for passwords, they will be shown in clear text (Vorgabe:
                                                     false)
 -h (--help)                                       : display this help page (Vorgabe: false)
 --project-root-dir <directory>                    : Sets the root directory for the installation. Will be used to derive other
                                                     parameters if not set explicitly. This directory is typically the target of
                                                     a GIT or SVN export.
 --software-dir <directory>                        : SW install directory (contains bin and lib directories)
                                                     e.g. ${PROJECT_ROOT}/opal-tools or %PROJECT_ROOT%\opal-tools 
 --template-dir <directory>                        : Patch template directory
                                                     e.g. ${PROJECT_ROOT}/patch-template or %PROJECT_ROOT%\patch-template
 --local-config-dir <directory>                    : Local configuration directory (connection pools, user dependent config),
                                                     typically OUTSIDE of the git tree
                                                     e.g. /local/conf-user or c:\local\conf-user
 --environment-script <directory>                  : Local script to initialize the user environment for this project
                                                     e.g. /local/conf-user/setProjectEnvironment.sh or
                                                     c:\local\conf-user\setProjectEnvironment.cmd
 --db-source-dir <directory>                       : Database source directory (sql, has subdirectories e.g.
                                                     sql/oracle_schema/tables, sql/oracle_schema/packages, etc.)
                                                     e.g. ${PROJECT_ROOT}/src/sql or %PROJECT_ROOT%\src\sql
 --patch-dir <directory>                           : Patch directory (patches, has subdirectories e.g. year/patch_name)
                                                     e.g. ${PROJECT_ROOT}/patches or %PROJECT_ROOT%\patches
 --schemas schema1 [schema2] [schema3] ...         : List of database schemas (blank-separated, e.g. schema1 schema2)
                                                     e.g. schema1 schema2
 --environments env1 [env2] [env3]...              : List of environments (blank-separated, e.g. dev test prod)
                                                     e.g. dev test prod
 --environment-colors color1 [color2] [color3] ... : List of shell colors for the environments (blank-separated, e.g. green
                                                     yellow red)
                                                     e.g. green yellow red: 
 --export-environment environment                  : Which is your designated developement environment? This is used for the
                                                     export.
                                                     e.g. dev
 --file-encoding file encoding                     : file encoding (e.g. UTF-8 or Cp1252, default is current system encoding)
                                                     e.g. UTF-8
```

The prompts are (defaults are shown in brackets [] and accepted by just pressing ``<enter>``): 

* ``Project root directory, typically the target of a GIT or SVN export``: 
    - In most cases you will use a central directory and all files for this project will be stored in subdirectories. 
    - Also, when using subversion, GIT or a similar tool you might have multiple exports of the same project in different locations. 
    - This directory will be set up as an environment variable and can be used for subsequent prompts, e.g. ${PROJECT_ROOT} or %PROJECT_ROOT%.
* ``SW install directory (contains bin and lib directories) [${PROJECT_ROOT}/opal-tools]``: 
    - Here we will install the actual software itself and create the subdirectories bin, conf and lib.
* ``Patch template directory [${PROJECT_ROOT}/patch-template]: ``: 
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
* ``Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.) [${PROJECT_ROOT}/sql]``:
    - in this directory you will store the sources for the project
* ``Patch directory (patches, has subdirectories e.g. year/patch_name) [${PROJECT_ROOT}/patches]``:
    - in this directory we will generate the new patch directories. The default layout is ``patches\<year>\<year-month-day>-<patch name>``, it can be changed in the file ``bin\initializePatch.cmd``
* ``List of database schemas (blank-separated, e.g. schema1 schema2) [schema1 schema2]``:
    - how many different schemas do we want to install into? 
    - This comma separated list will be used to generate the connection pool files. 
* ``List of environments (blank-separated, e.g. dev test prod) [dev test prod]``:
    - how many different environments do we want to install into? 
    - This comma separated list will be used to generate the connection pool files. 
* ``List of shell colors for the environments (blank-separated, e.g. green yellow red) [green yellow red]``:
    - The generated batch scripts for each environment can take on different foreground colors. 
    - This makes installing into a test environment (yellow) or production environment (red) more obvious.
* ``Which is your developement environment? This is used for the export:  [dev]``:
    - The designated development enviroment will be used in the export scripts as the source database connection.
* ``file encoding (e.g. UTF-8 or Cp1252, default is current system encoding):``:
    - The file encoding can be become very critical when using special characters like the German umlauts or others. 
    - This setting will be used to generate ``-Dfile.encoding=`` settings for the Java command lines. 
    - The APEX export tools will always generate UTF-8 encoded files. 
    - During installation the default from the operation system is picked up, this is for example UTF-8 on MacOS and Cp1252 on Windows. 

## Running the setup on Windows
```
setup.cmd --project-root-dir <project root directory>
e.g.
setup.cmd --project-root-dir c:\Projects\project1
```

## Running the setup on MacOS / Linux: 
```
./setup.sh --project-root-dir <project root directory>
e.g.
./setup.sh --project-root-dir /u01/project1
```

All environment variables are set up in the "**Local script to initialize the user environment**", e.g.: ``c:\opal-installer-local\setProjectEnvironment-project1.cmd``. 

# About Shell Scripting

The setup will pre-generate a number of shell scripts. Those shell scripts will leverage the command line options of the different tools, e.g.: 
```
cd "%OPAL_TOOLS_SRC_SQL_DIR%"

@call opal-export.cmd --config-file "%OPAL_TOOLS_HOME_DIR%\conf\opal-export.conf" ^
                      --output-dir "%OPAL_TOOLS_SRC_SQL_DIR%" ^
                      --pre-scripts "%OPAL_TOOLS_HOME_DIR%\export-scripts\opal-export-pre-script.sql" ^
                      --connection-pool-name #SCHEMA# ^
                      --connection-pool-file "%OPAL_TOOLS_USER_CONFIG_DIR%\connections-#ENV#.json"
```

We are using multi-line command lines. If you make changes, please be aware to use the proper line ending in shell scripts like ``\`` for bash and ``^`` for Windows as the last character on the line.

We also leverage all environment variables that are set up in the script ``user-conf/setProjectEnvironment.[sh|cmd]``, so that you don't have to hardcode any paths. 

# About File Encodings

It is important to be aware of different file encodings. Different operating systems will encode files differently by default, e.g. a German Windows system will use ``Cp1252`` by default whereas a Linux or MacOS system will typically use ``UTF-8``. 

When using special characters like German umlauts that can quickly become a problem. So, please make sure to define carefully what your default setting will be, you can set it during install. This will affect the parameter ``-Dfile.encoding=`` we will pass on the command line to the Java tools (you can see that in the ``setProjectEnvironment.[sh|cmd]`` script). 

Also, please make sure your SQL editors like Visual Studio Code, Notepad++, SQL Developer, Toad, etc. are configured by default to produce files in the encoding you want to use. 

If you have a team of developers using different operating systems, ``UTF-8`` seems to be the best choice. 

Your choice during setup will also generate the right mapping into the file [opal-installer.json](#opal-installerjson) in your patch-template directory (sample for Linux/MacOS): 
```
  "encodingMappings": [
    {
      "encoding": "UTF-8",
      "fileRegex": "/sql/.*apex.*/.*f*sql",
      "description": "encoding for APEX files is always UTF8"
    },
    {
      "encoding": "Cp1252",
      "fileRegex": "/sql/.*",
      "description": "all other files will get this explicit mapping"
    }
  ],
```
APEX files are always exported as UTF-8 by SQLcl or the APEXExport class utility, no matter what you define in ``-Dfile.encoding``. 

# About Java Regular Expressions

We are using Java Regular expressions in a number of places in the ``opal-installer.json`` file. The syntax can be found here: https://docs.oracle.com/javase/7/docs/api/java/util/regex/Pattern.html .

Special characters need to be escaped. And they need to be escaped again when reading the regular expression from a file in a string. 

Thus, when matching a file system path on Windows, e.g. ``\sql\`` the proper regular expression is ``\\\\sql\\\\`` because the ``\`` needs to be escaped **twice**:
* ``\sql\`` => ``\\sql\\`` for escaping the (regular expression) special character ``\``
* ``\\sql\\`` => ``\\\\sql\\\\`` for escaping the (Java) special character ``\`` in a Java String. 

# opal-install

## Command Line

The installer comes with a specific setup that will work in many cases. If you have other requirements, here is a description of the command line switches that you can use: 

```
 -h (--help)                                      : show this help page (Vorgabe: false)
 --connection-pool-file <file>                    : connection pool file
                                                    e.g.: connections-dev.json
 --config-file <file>                             : configuration file
                                                    e.g.: opal-installer.json
 --validate-only                                  : don't execute patch, just validate the files and connection pools (Vorgabe:
                                                    false)
 --mandatory-attributes <attr1> [<attr2>] ... [n] : list of attributes that must not be null,
                                                    e.g. patch author version
 --no-logging                                     : disable writing a logfile (Vorgabe: false)
 --source-list-file <filename>                    : source file name, e.g. SourceFilesReference.conf
 --source-dir <path>                              : path to the source directory, e.g. ../src/sql
 --silent                                         : disable all prompts, non-interactive mode (Vorgabe: false)
 --silent-execution                               : prompt after header information, execute all scripts without prompt.
```

## Database Log Tables

Logfiles are always generated automatically but the automatic logging into database tables is turned off initially. 

You can turn it on by configuring the parameter ``registryTargets`` in the configuration file [opal-installer.conf](#opal-installerjson). 

The simplest way is to define a connection pool to install the tables for the log registry. You pick a definition from your ``connections-<environment>.json`` file: 

```
  "registryTargets": [
      {
          "connectionPoolName": "schema1",
          "tablePrefix": "OPAL"
      }
  ],
```

This table will then be installed into **each** target environment that you install into. 

You can define multiple registryTargets, e.g.: 
```
  "registryTargets": [
      {
          "connectionPoolName": "schema1",
          "tablePrefix": "OPAL"
      },
      {
          "connectionPoolName": "global-registry",
          "tablePrefix": "OPAL"
      }
  ],
```

We are using it in this way so that we record each patch in the target schema and in our development environment **as well**. 

Therefore, we need an entry like this **in each** connection pool definition file pointing back to our development environment:
```
    {
      "name": "global-registry",
      "user": "schema1",
      "password": "pwd",
      "connectString": "127.0.0.1:1521:xe"
    }
```

You can query the tables:
```
SELECT *
  FROM opal_installer_patches
 ORDER BY pat_id DESC;
    
SELECT *
  FROM opal_installer_details
 ORDER BY det_id DESC;  
```

The tables are defined as follows: 
```
OPAL_INSTALLER_PATCHES

Name                   Null?    Typ                 
---------------------- -------- ------------------- 
PAT_ID                 NOT NULL NUMBER              
PAT_APPLICATION                 VARCHAR2(100 CHAR)  
PAT_NAME                        VARCHAR2(100 CHAR)  
PAT_REFERENCE_ID                VARCHAR2(100 CHAR)  
PAT_VERSION                     VARCHAR2(100 CHAR)  
PAT_AUTHOR                      VARCHAR2(100 CHAR)  
PAT_TARGET_SYSTEM               VARCHAR2(50 CHAR)   
PAT_EXTRA                       VARCHAR2(4000 CHAR)
PAT_STARTED_ON                  DATE                
PAT_ENDED_ON                    DATE                
PAT_DESCRIPTION                 VARCHAR2(4000 CHAR) 
PAT_CONFIG_FILENAME             VARCHAR2(4000 CHAR) 
PAT_CONN_POOL_FILENAME          VARCHAR2(4000 CHAR) 
```

```
OPAL_INSTALLER_DETAILS

Name             Null?    Typ                 
---------------- -------- ------------------- 
DET_ID           NOT NULL NUMBER              
DET_FILENAME              VARCHAR2(4000 CHAR) 
DET_INSTALLED_ON          DATE                
DET_PAT_ID                NUMBER              
```


# opal-export

## Command Line

The exporter comes with a specific setup that will work in many cases. If you have other requirements, here is a description of the command line switches that you can use: 

```
 -h (--help)                                                : show this help page (Vorgabe: false)
 -v (--version)                                             : show version information (Vorgabe: false)
 --url <jdbc url>                                           : database connection jdbc url,
                                                              e.g.: scott/tiger@localhost:1521:ORCL
 --connection-pool-file <file>                              : connection pool file
                                                              e.g.: connections-dev.json
 --connection-pool-name <connection pool name>              : connection pool name
                                                              e.g.: scott
 --output-dir <directory>                                   : output directory, e.g. '.' or '/u01/project/src/sql'
 --includes <filter1> [<filter2>] ... [n]                   : include filter, e.g.: %XLIB% or *XLIB*
 --include-types <type1> [<type2>] ... [n]                  : include types, e.g.: TABLE PACKAGE
 --excludes <type1> [<type2>] ... [n]                       : exclude filter, e.g.: %AQ$% %SYS_% or
 --exclude-types <type1> [<type2>] ... [n]                  : exclude types, e.g.: JOB
 --include-schemas <schema1> [<schema2>] ... [n]            : schemas to be included, only relevant when connecting as DBA
 --escape-char <escape character>                           : add escape() clause to like queries for selecting objects, e.g. \
                                                              or ~
 --dependent-objects <type>:<deptype1>,<deptype2> ... [n]   : dependent objects, e.g. TABLE:COMMENT,INDEX
 --skip-errors                                              : ORA- errors will not cause the program to abort (Vorgabe: false)
 --skip-export                                              : skip the export, this way only the pre- and post-scripts are run
                                                              (Vorgabe: false)
 --pre-scripts <script> [<script2>] ...                     : script (sqlplus/sqlcl) that is running to initialize the session,
                                                              similar to the login.sql file for sqlplus, e.g. ./login.sql or
                                                              ./init.sql
 --post-scripts <script> [<script2>] ...                    : script (sqlplus/sqlcl) that is running in the end to export custom
                                                              objects, e.g. ./apex.sql
 --silent                                                   : turns off prompts (Vorgabe: false)
 --filename-templates <definition 1> [<definition 2>] [...] : templates for constructing the filename per object type
                                                              e.g.: default:#schema#/#object_type#/#object_name#.sql
                                                              e.g.: package:#schema#/#object_type#/#object_name#.pks
                                                              e.g.: "package body:#schema#/packages/#object_name#.pkb"

                                                              #schema#             - schema name in lower case
                                                              #object_type#        - lower case type name: 'table'
                                                              #object_type_plural# - lower case type name in plural: 'tables'
                                                              #object_name#        - lower case object name
                                                              #SCHEMA#             - upper case schema name
                                                              #OBJECT_TYPE#        - upper case object type name: 'TABLE' or
                                                              'INDEX'
                                                              #OBJECT_TYPE_PLURAL# - upper case object type name in plural:
                                                              'TABLES'
                                                              #OBJECT_NAME#        - upper case object name
 --filename-replace-blanks                                  : replaces blanks in the filename with an _, e.g. PACKAGE
                                                              BODY=>PACKAGE_BODY (Vorgabe: true)
 --script-working-dir <directory>                           : working directory for running sqlcl scripts (-pre and -post), e.g.
                                                              '.' or '/u01/project/src/sql'. The default is the environment
                                                              variable OPAL_TOOLS_SRC_SQL_DIR
 --export-template-dir <directory>                          : directory for object templates, e.g. /u01/project/opal-tools/export-
                                                              templates
 --config-file <file>                                       : configuration file
                                                              e.g.: connections-dev.json
 --parallel-degree <level>                                  : the database statements are executed in parallel, e.g. 10 (Vorgabe:
                                                              1)
```

## Shared Command Line Switches

In addition to the command line switches in the shell scripts, opal-export can also centralize a number of configuration settings. By default, the shell scripts include command line switches from the file ``opal-tools/conf/opal-export.conf``: 
```
#
# - this file can contain parts of the command line for simplification
# - the code will replace all environment variables when they are 
#   specified ${env:name of the environment variable}, e.g. ${env:OPAL_TOOLS_HOME_DIR}
#   / will be replaced with \ on Windows automatically, but you can use \ as well
#
--dependent-objects table:comment,index,object_grant view:comment,object_grant,trigger "materialized view:comment,index,materialized_view_log,object_grant"
--skip-errors
--excludes SYS_YOID% SYS_PLSQL% AQ$%
--exclude-types LOB "TABLE_PARTITION" "INDEX PARTITION" "JAVA CLASS" JAVA "JAVA RESOURCE" INDEX
--filename-templates default:#schema#/#object_type_plural#/#object_name#.sql package:#schema#/packages/#object_name#.pks "package body:#schema#/packages/#object_name#.pkb"
--export-template-dir "${env:OPAL_TOOLS_HOME_DIR}/export-templates"
--parallel-degree 4
```

You have to be a bit careful about the syntax, it is **not** a shell script itself. Only a true shell script can deal with environment variables like ``$VAR`` or ``%VAR%``.

Therefore, we have two differences here: 
* each option is separated by a newline, you don't have to use the typical line ending in shell scripts like ``\`` for bash and ``^`` for Windows. 
* Use ``${env:VAR}`` to reference the environment variable ``VAR``. The syntax is the same on Windows **and** Linux/Mac.




# Configuration Files

## Connection Pool Files

The connection pool files are stored in the local user configuration folder. They are named ``connections-<environment>.json``. 

```
{
  "targetSystem": "test",
  "connectionPools": [
    {
      "name": "jri_test",
      "user": "jri_test",
      "password": "1:HcfzafJLBbo4b4sZiYDTrg==",
      "connectString": "127.0.0.1:1521:xe"
    }
  ] 
}
```

### ``"user"``
Consider a few use cases with regards to users: 
* ``"user": "schema1"``: connect as Oracle user ``schema1``
* ``"user": "daust[schema1]"``: connect as Oracle proxy user ``daust`` and switch to user ``schema1`` upon login
* ``"user": "sys as sysdba"``: connect as Oracle user ``SYS`` with role ``SYSDBA``

### ``"password"``
This is the password for the Oracle user as specified under ``"user"``. The password is encrypted and can be replaced in this file with a clear text password. The ``1:`` indicates that this password is already encrypted. When you use the connection pool for the next time, all unencrypted passwords will be encrypted automatically. 

When you start the script ``opal-tools/bin/validate-connections`` (on Windows you can just double-click it), the connection pools are all verified and the passwords are encrypted. 

### ``"connectString"``

Here are some links for specifying Oracle connect strings:
* [Oracle 19c JDBC urls](https://docs.oracle.com/en/database/oracle/oracle-database/19/jjdbc/data-sources-and-URLs.html#GUID-6F729E4D-064B-4FD9-AE92-1BD44B8BE5EF)
* [Oracle Cloud connect strings](https://medium.com/@FranckPachot/easy-oracle-cloud-wallet-location-in-the-jdbc-connection-string-c782d2011252)

Consider a few general use cases for connect strings: 
* Local database on port 1521 and SID orcl: ``"connectString": "127.0.0.1:1521:xe"``
* Local database on port 1521 and service name ``myService``: ``"connectString": "127.0.0.1:1521/myService"``
* Cloud database connect with tns names connect string ``db201909172340_high`` and the Oracle wallet in directory ``/oracle/wallet``: ``"connectString": "jdbc:oracle:thin:@db201909172340_high?TNS_ADMIN=/oracle/wallet"``


## ``opal-installer.json``

* ``application``: Name of the application, e.g. the project name
* ``patch``: Name of the patch. Accepts the placeholder ``#ENV_OPAL_TOOLS_USER_IDENTITY#`` for automatically replacing it with the current directory.
* ``author``: Name of the person who installs the patch. Accepts the placeholder ``#ENV_OPAL_TOOLS_USER_IDENTITY#`` for the environment variable ``OPAL_TOOLS_USER_IDENTITY``. 
* ``referenceId``: This is just a custom field to link this patch to other external tools you are using. It is a text string. 
* ``version``: Version of the patch, e.g. 1.0.0, 1.0, pre-release, ...
* ``extra``: This is a generic custom field that you can use any way you want. 
  ```
  This can be a plain string
    "extra": "stuff",
  or you might choose to embed a JSON string in it: 
    "extra": "{\"stuff\": \"value\"}",
  ```
* ``connectionMappings``: List of mappings with attributes: 
    * ``connectionPoolName``: Name of the connection pool to execute the current script
    * ``fileRegex``: Regular expression to map the file path (e.g. ``sql/<schema>/120_data/runme.sql``) to a specific connection pool.
* ``sqlFileRegex``: Regular expression to indicate which files should be executed and which not. For example, we want to ignore files *.txt, *.doc or others. By default the suffixes .sql, .pks, .pkb, .trg are executed. 
* ``registryTargets``: List of target database connections in which to register the patch tables (#PREFIX#_INSTALLER_PATCHES and #PREFIX_INSTALLER_DETAILS). In those tables the installer will register each execution of a patch. In most cases you will choose a connection pool from the current environment to put the registry tables there. But it also makes sense to have an additional connection pool to store each execution of ANY environment in that table, e.g. the development environment. Then you can have a consolidated view of all patches on all environments. 
    The registry targets have the following attributes: 
    * ``connectionPoolName``: Name of the connection pool to use for creating the tables. 
    * ``tablePrefix``: Prefix of the two generated tables so that they will fit into your local naming scheme of database objects, e.g. "OPAL". In this case the installer will generate the table OPAL_INSTALLER_PATCHES and OPAL_INSTALLER_DETAILS. 
* ``encodingMappings``: List of mappings with attributes: 
    * ``encoding``: File encoding, e.g. UTF-8 or Cp1252
    * ``fileRegex``: Regular expression to map the file path (e.g. ``sql/<schema>/120_data/runme.sql``) to a specific encoding.
    * ``description``: Description
* ``dependencies``: List of required patches. Before the patch can be installed, the required patches will be checked against the registry tables. If the patches don't exist on the target system, the patch cannot be installed. 
    They have the following attributes: 
    * ``application``: Name of the application
    * ``patch``: Name of the patch
    * ``referenceId``: This is just a custom field to link this patch to other external tools you are using. It is a text string. 
    * ``version``: Version of the patch

    This is the base query that will be used to determine whether the condition is satisfied: 
    ```
    select count(*) 
    from #PREFIX#_installer_patches 
    where (   pat_application=nvl(?,pat_application) 
            and pat_name=nvl(?,pat_name) 
            and pat_reference_id=nvl(?,pat_reference_id) 
            and pat_version=nvl(?,pat_version) 
            and pat_target_system=?) 
            and pat_ended_on is not null
    ```


### Windows example

```
{
  "application": "",
  "patch": "#PARENT_FOLDER_NAME#",
  "author": "#ENV_OPAL_TOOLS_USER_IDENTITY#",
  "referenceId": "External-Ref-1",
  "version": "",
  "extra": "{\"stuff\": \"value\"}",
  "connectionMappings": [
    {
      "connectionPoolName": "jri_test",
      "fileRegex": sql\\\\.*jri_test.*"
    }
  ],
  "sqlFileRegex": "\\.(sql|pks|pkb|trg)$",
  "registryTargets": [
      {
          "connectionPoolName": "jri_test",
          "tablePrefix": "OPAL"
      }
  ],
  "encodingMappings": [
    {
      "encoding": "UTF-8",
      "fileRegex": "\\\\sql\\\\.*apex.*\\\\.*f*sql",
      "description": "encoding for APEX files is always UTF8"
    },
    {
      "encoding": "Cp1252",
      "fileRegex": "\\\\sql\\\\.*",
      "description": "all other files will get this explicit mapping"
    }
  ],
  "dependencies": [
      {
          "patch": "2020-11-02-patch1"
      },
      {
          "application": "myApp",
          "version"    : "1.0.0"
      },
      {
        "application": "myApp",
        "referenceId": "REF-1"
      }
  ]
}
```

### MacOS / Linux example

```
{
  "application": "",
  "patch": "#PARENT_FOLDER_NAME#",
  "author": "#ENV_OPAL_TOOLS_USER_IDENTITY#",
  "referenceId": "External-Ref-1",
  "version": "",
  "extra": "{\"stuff\": \"value\"}",
  "connectionMappings": [
    {
      "connectionPoolName": "jri_test",
      "fileRegex": "/sql/.*jri_test.*"
    }
  ],
  "sqlFileRegex": "\\.(sql|pks|pkb|trg)$",
  "registryTargets": [
      {
          "connectionPoolName": "jri_test",
          "tablePrefix": "OPAL"
      }
  ],
  "encodingMappings": [
    {
      "encoding": "UTF-8",
      "fileRegex": "/sql/.*apex.*/.*f*sql",
      "description": "encoding for APEX files is always UTF8"
    },
    {
      "encoding": "Cp1252",
      "fileRegex": "/sql/.*",
      "description": "all other files will get this explicit mapping"
    }
  ],
  "dependencies": [
      {
          "patch": "2020-11-02-patch1"
      },
      {
          "application": "myApp",
          "version"    : "1.0.0"
      },
      {
        "application": "myApp",
        "referenceId": "REF-1"
      }
  ]
}
```

## ``ReleaseNotes.txt``

In the file ``ReleaseNotes.txt`` you can record all changes that are included in this patch. This file is special. If it is found in this directory, it will automatically be uploaded into the patch registry table into the column ``PAT_DESCRIPTION`` with the patch. 

## ``SourceFilesCopy.conf``

The file ``1.copy-source-files.cmd|sh`` is configured to copy files from the source directory ``sql`` to the target directory ``<patch name>/sql``. In the file ``SourceFilesCopy.conf`` you only configure, which files you want to have copied. 

E.g.: 
<pre style="overflow-x: auto; white-space: pre-wrap; white-space: -moz-pre-wrap; white-space: -pre-wrap; white-space: -o-pre-wrap; word-wrap: break-word;">
#----------------------------------------------------------
# Schema: jri_test 
#----------------------------------------------------------

# Preinstall => jri_test/010_preinstall

# Synonyms
jri_test/synonyms => jri_test/010_preinstall

# Sequences
jri_test/sequences => jri_test/020_sequences

# Types
jri_test/types => jri_test/030_types

# Tables
jri_test/tables => jri_test/040_tables
<b>xlib*.sql</b>

...
</pre>

Only tables (i.e. files) which match the wildcard ``xlib*.sql`` will be copied to the target directory ``<patch name>/sql/jri_test/040_tables``. 

The mappings have a predefined structure, so that the number of possible Oracle errors are minimized, e.g. we install the tables before the referential constraints, we install the package specifications before the package bodies and so forth. 

If you use a different layout, then you can easily modify the file ``SourceFilesCopy.conf`` in the patch template. The Java application will only create the directories when there are files to be copied. 

## ``SourceFilesReference.conf``

Sometimes you might prefer not to copy the files but only to *reference* the source files like packages, views, types, triggers, etc. from your source tree. 

In that case you have to register all files that you want in the patch in the file ``SourceFilesReference.conf``. They will not be copied to the target patch directory ... but used in the sort order as if they were copied. Their virtual target path will be used to determine the order of the execution of the file. But the actual file will reside in the source tree. 

E.g.: 
<pre style="overflow-x: auto; white-space: pre-wrap; white-space: -moz-pre-wrap; white-space: -pre-wrap; white-space: -o-pre-wrap; word-wrap: break-word;">
#----------------------------------------------------------
# Schema: jri_test 
#----------------------------------------------------------

# Preinstall => jri_test/010_preinstall

# Synonyms
jri_test/synonyms => jri_test/010_preinstall

# Sequences
jri_test/sequences => jri_test/020_sequences

# Types
jri_test/types => jri_test/030_types

# Tables
jri_test/tables => jri_test/040_tables
<b>xlib*.sql</b>

...
</pre>

Only tables (i.e. files) which match the wildcard ``xlib*.sql`` will be referenced from the source tree and treated as if they would actually reside in the target directory ``<patch name>/sql/jri_test/040_tables``. 

The mappings have a predefined structure, so that the number of possible Oracle errors are minimized, e.g. we install the tables before the referential constraints, we install the package specifications before the package bodies and so forth. 

If you use a different layout, then you can easily modify the file ``SourceFilesReference.conf`` in the patch template. The Java application will only create the directories when there are files to be copied. 

This file is picked up by the ``2.validate-<environment>.cmd|sh`` and ``3.install-<environment>.cmd|sh`` shell scripts. 

# Special Use Cases

### Using the installer with multiple developers on the same operating system

*tbd*

### Using the installer with multiple developers on different operating systems 

*tbd*

https://docs.oracle.com/cd/B19306_01/appdev.102/b14258/d_metada.htm#BGBHHHBG
https://ittutorial.org/dbms_metadata-get_ddl-get-ddl-create-script-of-any-object-in-oracle/

