-----------------------------------------------------------------------------
-- APEX export
-----------------------------------------------------------------------------

prompt *** Please configure the sql file for export
prompt *** Currently, nothing is exported. 


/*
    To find the right command line switches, please try the commands in your locally installed SQLcl client first.

    If you have not yet installed it, you can download it from here: 
        - https://www.oracle.com/tools/downloads/sqlcl-downloads.html
    
    - start sqlcl 
        - (e.g. "sql user/pwd@localhost:1521/xe")

    -------------------
    -- APEX EXPORT
    -------------------
    "help apex": help information on the apex commands
    "apex export": lists the command line options that are available
    "apex list": lists all applications, including their ids, the workspaces and ids

    Examples (always make sure you are connected with a schema that access to your workspace)
    - Basic export of application 100:
        - "apex export -applicationid 100"
    - Typical command that we often use
        - "apex export -applicationid 100 -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate"
        when you have translations: 
        - "apex export -applicationid 100 -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate -expTranslations"

    - Splitting the application into multiple files for seeing differences using version control: 
        - "apex export -applicationid 100 -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate -split"

    - Export of all applications in the current workspace
        - "apex export -workspaceid 9999999"
        - "apex export -workspaceid 9999999 -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate"

    - Export of an application in differerent formats
        - Comma delimited list of export types: APPLICATION_SOURCE (default), READABLE_YAML, READABLE_JSON, EMBEDDED_CODE, CHECKSUM-SH1 or CHECKSUM-SH256
        - apex export -applicationid 201 -expType READABLE_YAML
        - see: https://docs.oracle.com/en/database/oracle/apex/22.1/aeadm/exporting-one-or-more-applications.html#GUID-EC03A6A5-9C99-491F-9EFA-A00AAA1E8F9F
        - Requirement: APEX 22.1 and above installed

*/
