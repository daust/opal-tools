-- used by the installer to export the files for manual installation
-- this will prompt the user before executing each script
-- 
-- the SET DEFINE OFF / ON is used because many people use SET DEFINE OFF in the prepatch script
-- this causes this approach to fail, because the ACCEPT statement does not get new values, e.g. a 'x' to 
-- exit out of the script

set define on
ACCEPT f PROMPT 'Continue with "<ENTER>" and exit with "x": ' 
whenever sqlerror exit sql.sqlcode
exec if '&f' = 'x' then raise_application_error( -20000, 'Goodbye' ); end if;
whenever sqlerror continue
set define off
