package de.opal.db;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import javax.sql.PooledConnection;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.db.DBUtils;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.extension.SQLCLService;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;

public class SQLclUtil {

	public static final Logger log = LogManager.getLogger(SQLclUtil.class.getName());

	private static PrintStream currentErr;
	private static ByteArrayOutputStream baos;
	private static PrintStream newErr;

	/**
	 * 
	 * @param file
	 * @param sqlcl
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void executeFile(File file, ScriptExecutor sqlcl, String overrideEncoding, boolean displayFeedback)
			throws SQLException, IOException {

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);
		SQLclUtil.redirectErrStreamToString();

        // enable all REST commands
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
		// enable all other extensions
		ServiceLoader<SQLCLService> loader=ServiceLoader.load(SQLCLService.class);
		Iterator<SQLCLService> commands = loader.iterator();
		while (commands.hasNext()) {
			SQLCLService s = commands.next();
			//System.out.println("Enable " + s.getExtensionName() + " Class: " + s.getClass().getName() + " Version: "+ s.getExtensionVersion());
			CommandRegistry.addForAllStmtsListener(s.getCommandListener());
		}

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

		// capture error stream and filter out "false" messages
		String newErrString = SQLclUtil.getErrMessage();
		// reset err
		SQLclUtil.resetErrStream();

		if (!newErrString.isEmpty())
			System.err.println(newErrString);

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

        // enable all REST commands
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
		// enable all other extensions
		ServiceLoader<SQLCLService> loader=ServiceLoader.load(SQLCLService.class);
		Iterator<SQLCLService> commands = loader.iterator();
		while (commands.hasNext()) {
			SQLCLService s = commands.next();
			//System.out.println("Enable " + s.getExtensionName() + " Class: " + s.getClass().getName() + " Version: "+ s.getExtensionVersion());
			CommandRegistry.addForAllStmtsListener(s.getCommandListener());
		}

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);

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

		// conn = openConnection(user, pwd, connectStr);
		conn = ConnectionUtility.getInstance().getConnection();

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

				SQLclUtil.executeFile(script, sqlcl, null, displayFeedback);
			}
		}

	}

	/*
	 * Replace error messages that can be ignored:
	 * 
	 * https://twitter.com/daust_de/status/1331865412984844289
	 * 
	 * java.lang.AssertionError: sqlplus comment at
	 * oracle.dbtools.parser.NekotRexel.tokenize(NekotRexel.java:128) at
	 * oracle.dbtools.parser.NekotRexel.parse(NekotRexel.java:314) at
	 * oracle.dbtools.parser.LexerToken.parse(LexerToken.java:527) at
	 * oracle.dbtools.parser.LexerToken.parse(LexerToken.java:482) at
	 * oracle.dbtools.parser.LexerToken.parse(LexerToken.java:475) at
	 * oracle.dbtools.parser.LexerToken.parse(LexerToken.java:459) at
	 * oracle.dbtools.parser.LexerToken.parse(LexerToken.java:425) at
	 * oracle.dbtools.parser.Lexer.parse(Lexer.java:11) at
	 * oracle.dbtools.raptor.newscriptrunner.ScriptRunner.runPLSQL(ScriptRunner.java
	 * :330) at
	 * oracle.dbtools.raptor.newscriptrunner.ScriptRunner.run(ScriptRunner.java:245)
	 * at
	 * oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:
	 * 344) at
	 * oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:
	 * 227) at
	 * de.opal.tests.SQLclTestParserError.main(SQLclTestParserError.java:144)
	 * 
	 */
	public static String ignoreFalseErrors(String content) {
		Pattern p = Pattern.compile(
				"java.lang.AssertionError: sqlplus comment.*?" + getMainClassName() + ".java:\\d+\\)",
				Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		return p.matcher(content).replaceAll("").trim();
	}

	/*
	 * work with the error stream
	 */
	public static void redirectErrStreamToString() throws UnsupportedEncodingException {
		currentErr = System.err;
		baos = new ByteArrayOutputStream();
		newErr = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		System.setErr(newErr);
	}

	public static String getErrMessage() throws UnsupportedEncodingException {
		String errMsg = baos.toString(StandardCharsets.UTF_8.name());

		errMsg = ignoreFalseErrors(errMsg);

		return errMsg;
	}

	public static void resetErrStream() {
		System.setErr(currentErr);
	}

	public static String getMainClassName() {
		String result = null;

		
			result = getMainClass().getSimpleName();
		
		
		return result; 

	}

	private static Class<?> mainClass = null;

	public static Class<?> getMainClass()
	{
	    if (mainClass == null)
	    {
	        Map<Thread, StackTraceElement[]> threadSet = Thread.getAllStackTraces();
	        for (Map.Entry<Thread, StackTraceElement[]> entry : threadSet.entrySet())
	        {
	            for (StackTraceElement stack : entry.getValue())
	            {
	                try
	                {
	                    String stackClass = stack.getClassName();
	                    if (stackClass != null && stackClass.indexOf("$") > 0)
	                    {
	                        stackClass = stackClass.substring(0, stackClass.lastIndexOf("$"));
	                    }
	                    Class<?> instance = Class.forName(stackClass);
	                    Method method = instance.getDeclaredMethod("main", new Class[]
	                    {
	                        String[].class
	                    });
	                    if (Modifier.isStatic(method.getModifiers()))
	                    {
	                        mainClass = instance;
	                        break;
	                    }
	                }
	                catch (Exception ex)
	                {
	                	
	                }
	            }
	        }
	        return mainClass;
	    }
		return mainClass;
	}

}
