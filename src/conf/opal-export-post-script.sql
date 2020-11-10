
prompt display version of sqlcl
version

prompt show current working directory
pwd

prompt *** exporting apex applications
prompt *** configure opal-export-post-script.sql if required
/*
host mkdir /tmp/opal-exporter/apex
cd /tmp/opal-exporter/apex

apex export -applicationid 344
apex export -applicationid 201
*/


prompt *** exporting ORDS applications
prompt *** configure opal-export-post-script.sql if required
/*
https://dsavenko.me/apex-and-ords-deployments-automation/

set timing on
timing start TIMER_REST_EXPORT

-- SQL*Plus environment settings
-- they are crucial to retrieve a consistent export file
set feedback off
set heading off
set echo off
set flush off
set termout off
set pagesize 0
set long 100000000 longchunksize 32767
column output format a4000
set linesize 4000
set trimspool on

-- variable for storing the export data
variable contents clob

-- generating the export file
begin
    :contents := ords_metadata.ords_export.export_schema();
end;
/

-- you can specify any file name here
spool my_rest_modules_export.sql

print contents
-- the trailing '/' symbol is needed to terminate the generated PL/SQL block
prompt /

spool off

timing stop TIMER_REST_EXPORT


*/


