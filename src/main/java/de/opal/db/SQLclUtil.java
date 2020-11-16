package de.opal.db;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.PooledConnection;

import org.apache.commons.io.FileUtils;

import de.opal.installer.db.DBUtils;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;

public class SQLclUtil {
	
		/**
	 * 
	 * @param file
	 * @param sqlcl
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void executeFile(File file, ScriptExecutor sqlcl, String overrideEncoding, boolean displayFeedback )
			throws SQLException, IOException {

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);

		// enable all REST commands
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);

		// only execute if flag is set in config file
		// sqlcl.setStmt(new FileInputStream(file));
		String fileContents = "";

		if (overrideEncoding != null) {
			fileContents = FileUtils.readFileToString(file, System.getProperty("file.encoding"));
		} else {
			fileContents = FileUtils.readFileToString(file, overrideEncoding);
		}
		sqlcl.setStmt(fileContents);
		sqlcl.run();
		
		if (displayFeedback) {
			String results = bout.toString("UTF8");
			results = results.replaceAll(" force_print\n", "");
			Msg.println(results);			
		}
	}

	/**
	 * 
	 * @param file
	 * @param sqlcl
	 * @throws SQLException
	 * @throws IOException
	 */
	public void executeStatement(String statement, ScriptExecutor sqlcl) throws SQLException, IOException {

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);

		// enable all REST commands
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);

		sqlcl.setStmt(statement);
		sqlcl.run();

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");

		// suppress output when "name is already used by existing object
		if (!results.contains("ORA-00955")) {
			Msg.println(results);
		}
	}

	// see: https://twitter.com/krisrice/status/1324020253865725952
	public static void setWorkingDirectory(String directory, ScriptExecutor sqlcl) {
		sqlcl.setStmt("cd \"" + directory + "\"");
		sqlcl.run();
	}

	/**
	 * Retrieves or creates the corresponding ScriptExecutor based on the filename
	 * 
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	public static ScriptExecutor getScriptExecutor(String user, String pwd, String connectStr) throws SQLException {
		Connection conn;
		ScriptExecutor sqlcl;
		ScriptRunnerContext ctx;

		//conn = openConnection(user, pwd, connectStr);
		conn=ConnectionUtility.getInstance().getConnection();

		// then create ScriptRunner Context
		// create sqlcl
		sqlcl = new ScriptExecutor(conn);
		// set up context
		ctx = new ScriptRunnerContext();
		// set the output max rows
		ResultSetFormatter.setMaxRows(10000);
		// set the context
		sqlcl.setScriptRunnerContext(ctx);
		ctx.setBaseConnection(conn);

		return sqlcl;
	}

	public static Connection openConnection(String user, String pwd, String connectStr) {
		Connection conn = null;

		OracleConnectionPoolDataSource ocpds;
		PooledConnection pc;

		try {

			// set cache properties
			java.util.Properties prop = new java.util.Properties();
			prop.setProperty("InitialLimit", "1");
			prop.setProperty("MinLimit", "1");
			prop.setProperty("MaxLimit", "1");

			ocpds = new OracleConnectionPoolDataSource();

			ocpds.setURL(ConnectionUtility.transformJDBCConnectString(connectStr));
			ocpds.setUser(user);
			ocpds.setPassword(pwd);

			// set connection parameters
			ocpds.setConnectionProperties(prop);

			pc = ocpds.getPooledConnection();
			conn = pc.getConnection();
			conn.setAutoCommit(false);

		} catch (SQLException e) {
			Utils.throwRuntimeException("Could not connect via JDBC: " + e.getMessage());
		}

		return conn;
	}

	public static void closeConnection(Connection conn) {
		DBUtils.closeQuietly(conn);
	}

	public static void executeScripts(ScriptExecutor sqlcl, List<File> scripts, String workingDirectorySQLcl,
			boolean displayFeedback) throws SQLException, IOException {
		if (scripts.size() > 0) {
			for (File script : scripts) {
				if (displayFeedback)
					Msg.println("*** run script: " + script + "\n");
				
				if (workingDirectorySQLcl != null)
					SQLclUtil.setWorkingDirectory(workingDirectorySQLcl, sqlcl);

				SQLclUtil.executeFile(script, sqlcl, null, false);
			}
		}

	}
	
}
