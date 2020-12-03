/*=========================================================================
  $Id: _patch.sql 3079 2014-11-03 09:29:19Z ditzer.wolfram $

  Purpose  : ???
             
  $LastChangedDate: 2014-11-03 10:29:19 +0100 (Mo, 03 Nov 2014) $
  $LastChangedBy: ditzer.wolfram $ 
  
  Date        Author          Comment
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  10.11.2009  D. Aust         Initial creation

=========================================================================*/
set define '^'
set timing off
set pagesize 50000
set linesize 80 
set serveroutput on size unlimited
set sqlblanklines on

--#########################
define VERSION=16.2.0.12
-- TARGET_SCHEMA und SOURCE_SCHEMA immer KLEIN schreiben !!!! z.Bsp.: shdb_200, shdb_200_ta, shdb_jda, shdb_wws
define SOURCE_SCHEMA=shdb_200       -- shdb_200, shdb_jda, shdb_wws
define TARGET_SCHEMA=shdb_200       -- shdb_200, shdb_200_ta, ...
--#########################
spool _patch_^TARGET_SCHEMA._v^VERSION..log
@@lib/_require_user ^TARGET_SCHEMA.
@@lib/_patch_start
@@lib/_aq_stop
@@lib/_set_app_offline

-----------------------------------------------------------------------------

/*
prompt *** 
@@sql/^SOURCE_SCHEMA./.sql
@@lib/_pause
*/

prompt **********************************************************************
prompt ** Sequences
prompt **********************************************************************

prompt **********************************************************************
prompt ** Synonyms (consuming)
prompt **********************************************************************

--prompt *** 
--@@sql/^SOURCE_SCHEMA./_synonyms_consuming.sql
--@@lib/_pause

prompt **********************************************************************
prompt ** Types
prompt **********************************************************************

prompt **********************************************************************
prompt ** Tables
prompt **********************************************************************

/*
prompt *** 
@@sql/^SOURCE_SCHEMA./
@@lib/_pause
*/

prompt **********************************************************************
prompt ** Foreign Keys
prompt **********************************************************************

prompt **********************************************************************
prompt ** Views
prompt **********************************************************************

prompt **********************************************************************
prompt ** Procedures
prompt **********************************************************************

prompt **********************************************************************
prompt ** Functions
prompt **********************************************************************

prompt **********************************************************************
prompt ** Package Headers
prompt **********************************************************************

/*
prompt *** 
@@sql/^SOURCE_SCHEMA./
@@lib/_pause
*/

prompt **********************************************************************
prompt ** Package Bodies
prompt **********************************************************************

prompt **********************************************************************
prompt ** Trigger
prompt **********************************************************************

prompt *** shdb_sta_mas_his_afidu_trg.sql
@@sql/^SOURCE_SCHEMA./shdb_sta_mas_his_afidu_trg.sql
@@lib/_pause

prompt **********************************************************************
prompt ** Recompile Schema before running scripts
prompt **********************************************************************

prompt *** recompile objects
EXEC DBMS_UTILITY.compile_schema(schema => '^TARGET_SCHEMA.', compile_all => false);

prompt *** Invalid objects ***
column object_name format a30;
column object_type format a20;
select object_name, object_type from user_objects where status='INVALID'
/

@@lib/_pause

prompt **********************************************************************
prompt ** Data
prompt **********************************************************************

/*
prompt *** 
@@sql/^SOURCE_SCHEMA./
@@lib/_pause
set define '^'
*/

prompt **********************************************************************
prompt ** Scripts
prompt **********************************************************************

prompt *** Install processes


prompt **********************************************************************
prompt ** Grants (all in file grants.sql)
prompt **********************************************************************

--prompt *** 
--@@sql/^SOURCE_SCHEMA./_grants.sql
--@@lib/_pause

prompt **********************************************************************
prompt ** Synonyms (providing)
prompt **********************************************************************

--prompt *** 
--@@sql/^SOURCE_SCHEMA./_synonyms_providing.sql
--@@lib/_pause

prompt **********************************************************************
prompt ** Drop Skripte für alle Objekte
prompt **********************************************************************

-----------------------------------------------------------------------------
-- Hinweise nach der Installation (Post Installation Instruktionen)
-----------------------------------------------------------------------------

prompt **********************************************************************
prompt ** 
prompt **********************************************************************


-----------------------------------------------------------------------------

@@lib/_patch_end
@@lib/_aq_start

set define '^'
host find "ORA-" _patch_^TARGET_SCHEMA._v^VERSION..log
host find "SP2-" _patch_^TARGET_SCHEMA._v^VERSION..log

host grep "ORA-" _patch_^TARGET_SCHEMA._v^VERSION..log
host grep "SP2-" _patch_^TARGET_SCHEMA._v^VERSION..log

spool off
exit
