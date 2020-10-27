------------------------------------------------------------------------------
-- Before running data scripts we recompile the schema
------------------------------------------------------------------------------

--select sys_context( 'userenv', 'current_schema') from dual;

BEGIN
    DBMS_UTILITY.compile_schema (schema        => SYS_CONTEXT ('userenv', 'current_schema'),
                                 compile_all   => FALSE);
END;
/

PROMPT *** Invalid objects ***
COLUMN object_name FORMAT a30;
COLUMN object_type FORMAT a20;

SELECT object_name, object_type
  FROM user_objects
 WHERE status = 'INVALID'
/

