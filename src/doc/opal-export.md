Command line: 
```
java de.opal.exporter.DBExporter [options...] arguments...
 -url <jdbc url>                                            : database connection jdbc url, e.g.:
                                                              scott/tiger@localhost:1521:ORCL
 -o (--output-dir) <directory>                              : output directory, e.g. '.' or '/u01/project/src/sql'
 -i (--include) <filter1> [<filter2>] ... [n]               : include filter, e.g.: %XLIB%
 -it (--include-types) <type1> [<type2>] ... [n]            : include types, e.g.: TABLE PACKAGE
 -e (--exclude) <type1> [<type2>] ... [n]                   : exclude filter, e.g.: %AQ$% %SYS_%
 -et (--exclude-types) <type1> [<type2>] ... [n]            : exclude types, e.g.: JOB
 -s (--schemas) <schema1> [<schema2>] ... [n]               : schemas to be included, only relevant when connecting as
                                                              DBA
 -d (--dependent-objects) <type>:<deptype1>,<deptype2> ...  : dependent objects, e.g. TABLE:COMMENTS,INDEXES
 [n]                                                           
 -se (--skip-errors)                                        : ORA- errors will not cause the program to abort (Vorgabe:
                                                              false)
 -cf (--custom-file) <sql script>                           : custom export file, e.g. ./apex.sql
 -if (--init-file) <sql script>                             : sql initialization file that is executed when the db
                                                              session is initialized, similar to the login.sql file for
                                                              sqlplus, e.g. ./login.sql or ./init.sql
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
    --init-file ./init.sql 

# specify dependent objects
# http://download.oracle.com/docs/cd/B19306_01/appdev.102/b14258/d_metada.htm#BGBIEDIA
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --include XLIB% \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" \
    --init-file ./init.sql 

# samples from scheme2ddl: 
<util:map id="dependencies">
        <entry key="TABLE"> 
            <set>
                <value>COMMENT</value>
                <value>INDEX</value>
                <value>OBJECT_GRANT</value>
                <value>TRIGGER</value>
            </set>
        </entry>
        <entry key="VIEW">
            <set>
                <value>COMMENT</value>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
        <entry key="MATERIALIZED VIEW">
            <set>
                <value>COMMENT</value>
                <value>INDEX</value>
                <value>MATERIALIZED_VIEW_LOG</value>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
        <entry key="FUNCTION">
            <set>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
        <entry key="PROCEDURE">
            <set>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
        <entry key="PACKAGE BODY">
            <set>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
        <entry key="SYNONYM">
            <set>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
        <entry key="TYPE">
            <set>
                <value>OBJECT_GRANT</value>
            </set>
        </entry>
    </util:map>


# run custom apex export 
# specify dependent objects
# initialize environment with init.sql
# run custom apex export
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --include XLIB% \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" \
    --init-file ./init.sql \
    --custom-file ./apex-export.sql

# restrict export to VIEWs only
opal-export.sh \
    -url jri_test/oracle1@vm1:1521:xe \
    --output-dir . \
    --dependent-objects table:comment,index,object_grant,trigger view:comment,object_grant "materialized view:comment,index,materialized_view_log,object_grant" \
    --init-file ./init.sql \
    --custom-file ./apex-export.sql \
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
    --dependent-objects table:comment,index,object_grant,trigger \
    --init-file ./init.sql \
    --custom-file ./apex-export.sql \
    --schemas training test jri_test \
    --include-types table

file: apex-export.sql:

prompt *** exporting apex applications

host mkdir ./apex
cd ./apex

apex export -applicationid 344
apex export -applicationid 201

file: init.sql

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
