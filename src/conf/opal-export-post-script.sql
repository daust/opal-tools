-- per default, the working directory for scripts is the directory stored 
-- in the environment variable: OPAL_TOOLS_SRC_SQL_DIR
-- it can be changed at runtime using the switch: --script-working-directory <directory>
-- for the opal-export.sh call

-----------------------------------------------------------------------------
-- Sample APEX export
-----------------------------------------------------------------------------
/*
prompt *** exporting apex applications
prompt *** configure opal-export-post-script.sql if required

-- create new subdirectory sql/apex
host mkdir apex
cd apex

-- export applications
apex export -applicationid 344
apex export -applicationid 201

-- move up directory from sql/apex
cd ..
*/

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
