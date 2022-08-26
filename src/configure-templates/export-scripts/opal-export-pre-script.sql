set sqlformat ansiconsole

prompt *** set dbms_metadata transform parameter

BEGIN
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'SQLTERMINATOR', TRUE);
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_ATTRIBUTES', FALSE); --undocumented remove segement creation
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'EMIT_SCHEMA', FALSE); --undocumented remove schema
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_CREATION', FALSE);
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'PRETTY', TRUE);

   -- #111 Default settings for the export, table definitions will have ddl for primary key and unique index, causes error when installing table
   -- can only be done when the unique index is created BEFORE the constraints
   -- => exlude constraints by default, then add constraints through conf/opal-export.conf file
   --DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS_AS_ALTER', TRUE);
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS', FALSE);

   -- don't export ref_constraints with table, should be separate in 
   -- directory ref_constraints
   DBMS_METADATA.SET_TRANSFORM_PARAM (DBMS_METADATA.SESSION_TRANSFORM, 'REF_CONSTRAINTS', FALSE);
END;
/
