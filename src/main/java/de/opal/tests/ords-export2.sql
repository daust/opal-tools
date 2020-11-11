-----------------------------------------------------------------------------
-- Sample ORDS export
-----------------------------------------------------------------------------

--prompt *** first save current sqlplus settings in file plusenv.sql 
-- approach currently not working in sqlcl
--STORE SET /private/tmp/project1/sql/plusenv.sql replace

prompt *** set sqlplus environment for spool
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
set serveroutput on size unlimited

prompt *** spool rest modules
spool my_rest_modules_export.sql

-- *** generating the export file
declare
    l_clob clob;

    procedure print_clob(p_clob in out nocopy clob) is
    l_buffer        varchar2(400 char);
    l_buffer_len    number := 400;
    l_buffer_offset pls_integer := 1;
    begin
    -- initialize buffer - first chunk
    l_buffer := dbms_lob.substr( p_clob, l_buffer_len, l_buffer_offset );

    while length(l_buffer) > 0 loop
        dbms_output.put_line(l_buffer);

        -- get next chunk
        l_buffer_offset := l_buffer_offset + l_buffer_len;
        l_buffer := dbms_lob.substr( p_clob, l_buffer_len, l_buffer_offset );
    end loop;
    end;
begin
    l_clob := ords_metadata.ords_export.export_schema();
    print_clob(l_clob);

    -- the trailing '/' symbol is needed to terminate the generated PL/SQL block
    dbms_output.put_line('/');
end;
/
spool off

--prompt *** restore sqlplus settings (currently not working in sqlcl)
--@/private/tmp/project1/sql/plusenv.sql

set feedback on
set heading on
set echo on
set flush on
set termout on
set pagesize 54
set long 80 longchunksize 80
set linesize 126
set trimspool off

