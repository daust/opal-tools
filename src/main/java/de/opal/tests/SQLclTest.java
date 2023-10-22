package de.opal.tests;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.ServiceLoader;

import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.extension.SQLCLService;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;

public class SQLclTest {

	public static void main(String[] args) throws SQLException, UnsupportedEncodingException {
		/* OLD approach
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
		CommandRegistry.addForAllStmtsListener(ApexCommand.class);
		CommandRegistry.addForAllStmtsListener(LbCommand.class);
		*/

        // enable all REST commands
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
		// enable all other extensions
		ServiceLoader<SQLCLService> loader=ServiceLoader.load(SQLCLService.class);
		Iterator<SQLCLService> commands = loader.iterator();
		while (commands.hasNext()) {
			SQLCLService s = commands.next();
			System.out.println("Enable " + s.getExtensionName() + " Class: " + s.getClass().getName() + " Version: "+ s.getExtensionVersion());
			CommandRegistry.addForAllStmtsListener(s.getCommandListener());
		}

		Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//win11:1521/orclpdb", "demo", "oracle1");
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
		//System.out.println("*** ctx.getOutputStream");
		//System.out.println(ctx.getOutputStream());

		// SQLclUtil.setWorkingDirectory("/private/tmp/project1/sql", sqlcl);

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);
		
		// # run a whole file
		// adjust the path as it needs to be absolute
		// sqlcl.setStmt("@/private/tmp/project1/sql/ords-export2.sql");
		// CommandRegistry. addForAllStmtsListener (ApexCommand.class);
		// CommandRegistry.addForAllStmtsListener(RESTCommand.class,
		// StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
		// sqlcl.setStmt("help");
		// sqlcl.setStmt("select sysdate from dual;");
		// sqlcl.setStmt("rest modules");
		//sqlcl.getScriptRunnerContext().putProperty("script.runner.commandlineconnect", Boolean.TRUE);
		
		
		sqlcl.setStmt("@/tmp/test.sql");
		sqlcl.run();

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");
		System.out.println(results);
	}

}
