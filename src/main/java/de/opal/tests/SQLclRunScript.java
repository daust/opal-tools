package de.opal.tests;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.datatypes.DataValue;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;

public class SQLclRunScript {

	public static void main(String[] args) throws SQLException, UnsupportedEncodingException {
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

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);

		// # run a whole file
		// adjust the path as it needs to be absolute
		sqlcl.setStmt("@/tmp/sqlcl/test.sql");

		// enable prompting for variables
		System.out.println("SubstitutionOn=" + ctx.getSubstitutionOn());
		ctx.setSubstitutionOn(true);
		System.out.println("SubstitutionOn=" + ctx.getSubstitutionOn());

		Map<String, List<DataValue>> myBatchVarMap = ctx.getBatchVarMap();
		Map<String, String> myMap = ctx.getMap();
		Map<String, String> mySubVarTypeMap = ctx.getSubVarTypeMap();
		
		ctx.getMap().put("LABEL", "Dietmar");
		
		sqlcl.run();

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");
		System.out.println(results);
	}

}
