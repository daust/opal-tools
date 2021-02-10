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
define VERSION=18.1.0.0
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

prompt **********************************************************************
prompt ** Foreign Keys
prompt **********************************************************************

prompt **********************************************************************
prompt ** Views
prompt **********************************************************************

prompt *** prst_erg_sta_umsetzung_aggr_v.sql
@@sql/^SOURCE_SCHEMA./prst_erg_sta_umsetzung_aggr_v.sql
@@lib/_pause

prompt *** prst_erg_sta_umsetzung_rl_v.sql
@@sql/^SOURCE_SCHEMA./prst_erg_sta_umsetzung_rl_v.sql
@@lib/_pause

prompt *** shdb_buchwerte_rep_v.sql
@@sql/^SOURCE_SCHEMA./shdb_buchwerte_rep_v.sql
@@lib/_pause

prompt **********************************************************************
prompt ** Procedures / Functions
prompt **********************************************************************

prompt **********************************************************************
prompt ** Package Headers
prompt **********************************************************************

set define '&'
prompt *** prst_jda_web_user_manager.pks
@@sql/&SOURCE_SCHEMA./prst_jda_web_user_manager.pks
@@lib/_pause
set define '^'

prompt *** shdb_application.pks
@@sql/^SOURCE_SCHEMA./shdb_application.pks
@@lib/_pause

prompt **********************************************************************
prompt ** Package Bodies
prompt **********************************************************************

set define '&'
prompt *** prst_jda_web_user_manager.pkb
@@sql/&SOURCE_SCHEMA./prst_jda_web_user_manager.pkb
@@lib/_pause
set define '^'

prompt *** shdb_application.pkb
@@sql/^SOURCE_SCHEMA./shdb_application.pkb
@@lib/_pause

prompt *** shdb_jda_web_user_ui.pkb
@@sql/^SOURCE_SCHEMA./shdb_jda_web_user_ui.pkb
@@lib/_pause


prompt *** shdb_massnahme_ui.pkb
@@sql/^SOURCE_SCHEMA./shdb_massnahme_ui.pkb
@@lib/_pause

prompt *** shdb_mietvertrag.pkb
@@sql/^SOURCE_SCHEMA./shdb_mietvertrag.pkb
@@lib/_pause

prompt *** shdb_standort_ui.pkb
@@sql/^SOURCE_SCHEMA./shdb_standort_ui.pkb
@@lib/_pause

prompt *** shdb_utl.pkb
@@sql/^SOURCE_SCHEMA./shdb_utl.pkb
@@lib/_pause

--prompt *** xlib_sec.pkb
--@@sql/^SOURCE_SCHEMA./xlib_sec.pkb
--@@lib/_pause

prompt **********************************************************************
prompt ** Trigger
prompt **********************************************************************

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

prompt *** xlib_conf_values_data.sql
@@sql/^SOURCE_SCHEMA./xlib_conf_values_data.sql
@@lib/_pause
set define '^'

prompt **********************************************************************
prompt ** Scripts
prompt **********************************************************************

prompt *** Install processes


prompt *** shdb_ui_privileges.sql
@@sql/^SOURCE_SCHEMA./shdb_ui_privileges.sql
@@lib/_pause

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
prompt ** Drop Skripte f�r alle Objekte
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
