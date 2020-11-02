/*
SQL> apex export	Verfügbare Optionen:
	-applicationid:    ID der zu exportierenden Anwendung
	-workspaceid:      Workspace-ID für alle zu exportierenden Anwendungen oder den zu exportierenden Workspace
	-instance:         Alle Anwendungen exportieren
	-expWorkspace:     Den durch -workspaceid identifizierten Workspace oder alle Workspaces exportieren, wenn -workspaceid nicht angegeben ist
	-expMinimal:       Nur Definition, Benutzer und Gruppen des Workspace exportieren
	-expFiles:         Alle durch -workspaceid identifizierten Workspace-Dateien exportieren
	-skipExportDate:   Exportdatum von Exportdateien der Anwendung ausschließen
	-expPubReports:    Alle vom Benutzer gespeicherten öffentlichen interaktiven Berichte exportieren
	-expSavedReports:  Alle vom Benutzer gespeicherten interaktiven Berichte exportieren
	-expIRNotif:       Alle interaktiven Berichtsbenachrichtigungen exportieren
	-expTranslations:  Die Übersetzungszuordnungen und den gesamten Text aus dem Übersetzungs-Repository exportieren
	-expFeedback:      Team Development-Feedback für alle Workspaces oder für die durch -workspaceid identifizierten Workspaces exportieren
	-expTeamdevdata:   Team Development-Daten für alle Workspaces oder für die durch -workspaceid identifizierten Workspaces exportieren
	-deploymentSystem: Deployment-System für exportiertes Feedback
	-expFeedbackSince: Team Development-Feedback ab dem angegebenen Datum im Format JJJJMMTT exportieren
	-expOriginalIds:   Beim Export werden die IDs mit dem Status beim Importieren der Anwendung ausgegeben
	-split:            Anwendungen in mehrere Dateien aufteilen

*/

------------------------------------------------------------------------------------
set feedback off
set echo off
set term off
set verify off
------------------------------------------------------------------------------------
begin
 dbms_metadata.set_transform_param( dbms_metadata.session_transform, 'SQLTERMINATOR', TRUE );
 dbms_metadata.set_transform_param( dbms_metadata.session_transform, 'SEGMENT_ATTRIBUTES', false) ; 
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'EMIT_SCHEMA', false );  
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'SEGMENT_CREATION', false );  
 DBMS_METADATA.SET_TRANSFORM_PARAM( DBMS_METADATA.SESSION_TRANSFORM, 'CONSTRAINTS_AS_ALTER', true );
end;
/
------------------------------------------------------------------------------------
spool spool/oehr_customers.sql
ddl oehr_customers;

--spool off

--spool spool/x.sql

SELECT dbms_metadata.get_dependent_ddl('REF_CONSTRAINT', table_name)
FROM user_tables t
WHERE table_name IN ('OEHR_ORDERS', 'OEHR_CUSTOMERS')
AND EXISTS (SELECT 1
FROM user_constraints
WHERE table_name = t.table_name
AND constraint_type = 'R');

spool off


------------------------------------------------------------------------------------
set term on
set feedback on
set verify on
set echo on



