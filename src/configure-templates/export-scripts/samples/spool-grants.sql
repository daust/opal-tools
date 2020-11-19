-----------------------------------------------------------------------------
-- spool grants
-----------------------------------------------------------------------------

prompt *** spool grants into file: jri_test/grants/grants.sql
host mkdir jri_test/grants

set feedback off
set heading off
-- removes the leading newline before the report
set embedded on
set verify off	   
set termout off
set pagesize 0
set long 900000
set linesize 2000
set trimspool on
set echo off
set timing off

spool jri_test/grants/grants.sql

select dbms_metadata.get_dependent_ddl('OBJECT_GRANT', object_name , owner) 
  from all_objects
 where owner='JRI_TEST'
   and (owner, object_name) in (
    select distinct table_schema, table_name 
      from all_tab_privs
     where table_schema='JRI_TEST');

spool off;

-----------------------------------------------------------------------------
-- Sample ORDS export
-- the simple approach using rest export is currently not working (sqlcl 20.3)
-- when using the sqlcl libraries through Java programming
-----------------------------------------------------------------------------
/*
-- create new subdirectory sql/ords
host mkdir ords
cd ords

prompt *** spool rest modules
spool my_rest_modules_export.sql

rest export
prompt /

spool off

-- move up directory from sql/ords
cd ..

*/
