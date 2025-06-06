/*
  Custom DDL for Synonym
  The remove of emit_schema is not working correctly. 
  Thus, after the DDL is generated, the schema name is removed using replace(). 
*/ 
declare
  function get_ddl return clob is
    l_schema varchar2(100) := :schema_name;
    l_object_type varchar2(100) := :object_type;
    l_object_name varchar2(100) := :object_name;
    
    l_clob clob;
  begin
    DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'EMIT_SCHEMA', TRUE); --undocumented remove schema

    select dbms_metadata.get_ddl(object_type => l_object_type,
                                 name => l_object_name,
                                 schema => l_schema) into l_clob from dual;

    DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'EMIT_SCHEMA', FALSE); --undocumented remove schema

    l_clob := replace(l_clob, '"'||l_schema||'".', '');

    return l_clob;
  end;
BEGIN
    :retval := get_ddl();
END;
