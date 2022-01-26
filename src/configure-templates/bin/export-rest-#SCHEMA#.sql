-----------------------------------------------------------------------------
-- REST export
-----------------------------------------------------------------------------

prompt *** spool rest modules
spool my_rest_modules_export.sql

rest export
prompt /

spool off
