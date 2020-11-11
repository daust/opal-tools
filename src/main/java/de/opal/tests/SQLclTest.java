package de.opal.tests;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.opal.db.SQLclUtil;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;

public class SQLclTest {

	
	public static void main(String[] args) throws SQLException, UnsupportedEncodingException {
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
	    Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//vm1:1521/XE", "ordstest", "oracle1");
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

	    // # run a whole file 
	    // adjust the path as it needs to be absolute
	    sqlcl.setStmt("@/private/tmp/project1/sql/ords-export2.sql");
	    sqlcl.run();


	    String results = bout.toString("UTF8");
	    results = results.replaceAll(" force_print\n", "");
	    System.out.println(results);
	  }



}
