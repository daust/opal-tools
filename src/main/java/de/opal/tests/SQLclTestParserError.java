package de.opal.tests;


/*

Testcase: https://twitter.com/daust_de/status/1331865412984844289

begin
wwv_flow_api.create_list_of_values(
 p_id=>wwv_flow_api.id(7215703205173048)
,p_lov_name=>'PROL_SK'
,p_lov_query=>wwv_flow_string.join(wwv_flow_t_varchar2(
'select prol_titel d, prol_sk r',
'    from ress_projektrolle',
'order by 1'))
,p_source_type=>'SQL'
,p_location=>'LOCAL'
,p_return_column_name=>'R'
,p_display_column_name=>'D'
,p_group_sort_direction=>'ASC'
,p_default_sort_direction=>'ASC'
);
end;
/
rollback;


 */

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.opal.db.SQLclUtil;
import de.opal.installer.util.Msg;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;

public class SQLclTestParserError {

	
	public static void main(String[] args) throws SQLException, UnsupportedEncodingException {
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
	    Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//vm1:1521/XE", "jri_test", "oracle1");
	    conn.setAutoCommit(false);

	    // #create sqlcl
	    ScriptExecutor sqlcl = new ScriptExecutor(conn);

	    // #setup the context
	    ScriptRunnerContext ctx = new ScriptRunnerContext();
	    	    
	    // set the output max rows
			ResultSetFormatter.setMaxRows(10000);
	    // #set the context
	    sqlcl.setScriptRunnerContext(ctx);
	    ctx.setBaseConnection(conn);

		SQLclUtil.setWorkingDirectory("/private/tmp/project1/sql", sqlcl);
	    	    
	    // Capture the results without this it goes to STDOUT
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    BufferedOutputStream buf = new BufferedOutputStream(bout);
	    sqlcl.setOut(buf);
	    
	    Msg.println("run statement ...");

	    // # run a whole file 
	    // adjust the path as it needs to be absolute
	    //sqlcl.setStmt("@/private/tmp/project1/sql/ords-export2.sql");
	    sqlcl.setStmt("begin\n" + 
	    		"wwv_flow_api.create_list_of_values(\n" + 
	    		" p_id=>wwv_flow_api.id(7215703205173048)\n" + 
	    		",p_lov_name=>'PROL_SK'\n" + 
	    		"");
	    sqlcl.run();
	    
	    Msg.println("end run ...");


	    String results = bout.toString("UTF8");
	    results = results.replaceAll(" force_print\n", "");
	    System.out.println(results);
	    
	    Msg.println("*** END");
	  }



}
