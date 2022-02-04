-----------------------------------------------------------------------------
-- REST export
-----------------------------------------------------------------------------

prompt *** spool rest modules
spool my_rest_modules_export.sql

rest export
prompt /

spool off


/*
    To find the right command line switches, please try the commands in your locally installed SQLcl client first.

    If you have not yet installed it, you can download it from here: 
        - https://www.oracle.com/tools/downloads/sqlcl-downloads.html
    
    - start sqlcl 
        - (e.g. "sql user/pwd@localhost:1521/xe")

    -------------------
    -- REST EXPORT
    -------------------
    "help rest": help information on the rest commands

    - Typical export command: 

    prompt *** spool rest modules
    spool my_rest_modules_export.sql
    rest export
    prompt /
    spool off

*/

