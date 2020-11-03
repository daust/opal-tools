Command line: 
```
java de.opal.exporter.DBExporter [options...]
 -url <jdbc url>                                               : database connection jdbc url, 
                                                                 e.g.: scott/tiger@localhost:1521:ORCL
 -o (--output-dir) <directory>                                 : output directory, e.g. '.' or '/u01/project/src/sql'
 -i (--include) <filter1> [<filter2>] ... [n]                  : include filter, e.g.: %XLIB%
 -it (--include-types) <type1> [<type2>] ... [n]               : include types, e.g.: TABLE PACKAGE
 -e (--exclude) <type1> [<type2>] ... [n]                      : exclude filter, e.g.: %AQ$% %SYS_%
 -et (--exclude-types) <type1> [<type2>] ... [n]               : exclude types, e.g.: JOB
 -s (--schemas) <schema1> [<schema2>] ... [n]                  : schemas to be included, only relevant when connecting as DBA
 -d (--dependent-objects) <type>:<deptype1>,<deptype2> ... [n] : dependent objects, e.g. TABLE:COMMENT,INDEX
 -em (--extension-map) <map1> [<map2>] ... [n]                 : mapping of object types to filename suffixes, e.g.: DEFAULT:sql
                                                                 PACKAGE:pks
 -dm (--directory-map) <map1> [<map2>] ... [n]                 : mapping of object types to directories, e.g.: PACKAGE:package
                                                                 "package body:package"
 -se (--skip-errors)                                           : ORA- errors will not cause the program to abort (Vorgabe: false)
 -pre (--pre-script) <sqlplus/sqlcl script>                    : script (sqlplus/sqlcl) that is running to initialize the
                                                                 session, similar to the login.sql file for sqlplus, e.g.
                                                                 ./login.sql or ./init.sql
 -post (--post-script) <sqlplus/sqlcl script>                  : script (sqlplus/sqlcl) that is running in the end to export
                                                                 custom objects, e.g. ./apex.sql
 --silent                                                      : turns off prompts (Vorgabe: false)
 -ft (--filename-template) <template structure>                : template for constructing the filename
                                                                 e.g.: schema/object_type/object_name.ext
                                                                  
                                                                 schema       - schema name in lower case
                                                                 type         - lower case type name: 'table'
                                                                 ext          - lower case extension: 'sql' or 'pks'
                                                                 SCHEMA       - upper case schema name
                                                                 TYPE         - upper case object type name: 'TABLE' or 'INDEX'
                                                                 OBJECT_NAME  - upper case object name
                                                                 EXT          - upper case extension: 'SQL' or 'PKS' (Vorgabe:
                                                                 schema/object_type/object_name.ext)
 --filename-replace-blanks                                     : replaces blanks in the filename with an _, e.g. PACKAGE
                                                                 BODY=>PACKAGE_BODY (Vorgabe: true)
```
cd /tmp/opal-installer/sql

-- cleanup 
rm -rf apex jri_test

# calling it without parameters will show usage
opal-export.sh

# export all everything 
# will raise errors, no specific dbms_metadata parameters set
# the bare default
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    -o . \

# skip errors 
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    -o . \
    --skip-errors

# exclude all SYS_% objects
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    -o . \
    --skip-errors \
    -e SYS_%

# export all xlib entries into the current directory and subdirectories
# table files also contain comments and indexes
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    -o . \
    -i XLIB% \
    -d table:comment,index

# initialize environment with init.sql
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --include XLIB% \
    --pre-script ./init.sql 

# specify dependent objects
# http://download.oracle.com/docs/cd/B19306_01/appdev.102/b14258/d_metada.htm#BGBIEDIA
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --include XLIB% \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" function:object_grant "package body:object_grant" synonym:object_grant type=object_grant \
    --pre-script ./init.sql 

# run custom apex export 
# specify dependent objects
# initialize environment with init.sql
# run custom apex export
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --include XLIB% \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" function:object_grant "package body:object_grant" synonym:object_grant type=object_grant \
    --pre-script ./init.sql \
    --post-script ./apex-export.sql

# restrict export to VIEWs only
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" function:object_grant "package body:object_grant" synonym:object_grant type=object_grant \
    --pre-script ./init.sql \
    --post-script ./apex-export.sql \
    --include-types VIEW

# export all xlib entries from multiple schemas
# table files also contain comments and indexes
# run custom apex export 
# MUST CONNECT AS DBA
# Filter must change from XLIB% => %XLIB% because we are now comparing <schema>.<object_name> with <include filter>
opal-export.sh \
    -url system/oracle1@vm1:1521:xe \
    --output-dir . \
    --include %XLIB_LOG% \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" function:object_grant "package body:object_grant" synonym:object_grant type=object_grant \
    --pre-script ./init.sql \
    --post-script ./apex-export.sql \
    --schemas training test jri_test \
    --include-types table

file: post-script.sql:
------------------------
prompt *** exporting apex applications

/*
host mkdir ./apex
cd ./apex

-- export specific application
apex export -applicationid 344
apex export -applicationid 201

-- export worksapce
...

*/

file: pre-script.sql
------------------------
set sqlformat ansiconsole

prompt *** set dbms_metadata transform parameter
begin
 dbms_metadata.set_transform_param( dbms_metadata.session_transform, 'SQLTERMINATOR', TRUE );
 dbms_metadata.set_transform_param( dbms_metadata.session_transform, 'SEGMENT_ATTRIBUTES', false) ; --undocumented remove segement creation
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'EMIT_SCHEMA', false );        --undocumented remove schema
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_CREATION', false );  
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS_AS_ALTER', true );
 
 DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'PRETTY',true);
end;
/
```
