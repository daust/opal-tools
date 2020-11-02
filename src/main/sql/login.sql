set sqlformat ansiconsole

prompt *** set dbms_metadata transform parameter
begin
 dbms_metadata.set_transform_param( dbms_metadata.session_transform, 'SQLTERMINATOR', TRUE );
 dbms_metadata.set_transform_param( dbms_metadata.session_transform, 'SEGMENT_ATTRIBUTES', false) ; --undocumented remove segement creation
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'EMIT_SCHEMA', false );        --undocumented remove schema
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_CREATION', false );  
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS_AS_ALTER', true );
 
 DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'PRETTY',true);
end;
/