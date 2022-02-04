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
        - "apex export -applicationid 100 -expOriginalIds -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate"
        when you have translations: 
        - "apex export -applicationid 100 -expOriginalIds -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate -expTranslations"

    - Splitting the application into multiple files for seeing differences using version control: 
        - "apex export -applicationid 100 -expOriginalIds -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate -split"

    - Export of all applications in the current workspace
        - "apex export -workspaceid 9999999"
        - "apex export -workspaceid 9999999 -expOriginalIds -expComments -expSupportingObjects Y -expACLAssignments -skipExportDate"

*/
