prompt **********************************************************************
prompt ** Recompile Schema
prompt **********************************************************************

begin
  dbms_utility.compile_schema(sys_context('USERENV', 'CURRENT_SCHEMA'), false);
end;
/

prompt ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
prompt ~~ any invalid objects?
prompt ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
COL object_name FORMAT a30
COL object_type FORMAT a30

SELECT   object_name, object_type
    FROM all_objects
   WHERE owner = SYS_CONTEXT ('USERENV', 'CURRENT_SCHEMA')
     AND status = 'INVALID'
ORDER BY 1;
