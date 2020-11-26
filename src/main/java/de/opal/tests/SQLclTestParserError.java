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
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
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

	public static void main(String[] args) throws SQLException, UnsupportedEncodingException, FileNotFoundException {
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

		/*
		 * capture current error stream
		 */
		
		SQLclUtil.redirectErrStreamToString();

		/*
		 * run statement
		 */
		Msg.println("run statement ...");

		/*
		 * 
		 * sqlcl.setStmt("begin\n" + "wwv_flow_api.create_list_of_values(\n" +
		 * " p_id=>wwv_flow_api.id(7215703205173048)\n" + ",p_lov_name=>'PROL_SK'\n" +
		 * ""); sqlcl.run();
		 */

		sqlcl.setStmt("begin\n" + "wwv_flow_api.create_list_of_values(\n" + " p_id=>wwv_flow_api.id(7215703205173048)\n"
				+ ",p_lov_name=>'PROL_SK'\n" + ",p_lov_query=>wwv_flow_string.join(wwv_flow_t_varchar2(\n"
				+ "'select prol_titel d, prol_sk r',\n" + "'    from ress_projektrolle',\n" + "'order by 1'))\n"
				+ ",p_source_type=>'SQL'\n" + ",p_location=>'LOCAL'\n" + ",p_return_column_name=>'R'\n"
				+ ",p_display_column_name=>'D'\n" + ",p_group_sort_direction=>'ASC'\n"
				+ ",p_default_sort_direction=>'ASC'\n" + ");\n" + "end;\n" + "/\n" + "rollback;\n" + "begin\n"
				+ "wwv_flow_api.create_list_of_values(\n" + " p_id=>wwv_flow_api.id(7215703205173048)\n"
				+ ",p_lov_name=>'PROL_SK'\n" + ",p_lov_query=>wwv_flow_string.join(wwv_flow_t_varchar2(\n"
				+ "'select prol_titel d, prol_sk r',\n" + "'    from ress_projektrolle',\n" + "'order by 1'))\n"
				+ ",p_source_type=>'SQL'\n" + ",p_location=>'LOCAL'\n" + ",p_return_column_name=>'R'\n"
				+ ",p_display_column_name=>'D'\n" + ",p_group_sort_direction=>'ASC'\n"
				+ ",p_default_sort_direction=>'ASC'\n" + ");\n" + "end;\n" + "/\n" + "");
		sqlcl.run();


		/*
		 * newErrString="java.lang.AssertionError: sqlplus comment\n" +
		 * "	at oracle.dbtools.parser.NekotRexel.tokenize(NekotRexel.java:128)\n" +
		 * "	at oracle.dbtools.parser.NekotRexel.parse(NekotRexel.java:314)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:527)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:482)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:475)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:459)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:425)\n" +
		 * "	at oracle.dbtools.parser.Lexer.parse(Lexer.java:11)\n" +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptRunner.runPLSQL(ScriptRunner.java:330)\n"
		 * +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptRunner.run(ScriptRunner.java:245)\n"
		 * +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:344)\n"
		 * +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:227)\n"
		 * +
		 * "	at de.opal.tests.SQLclTestParserError.main(SQLclTestParserError.java:146)\n"
		 * + " Hello world1\n" + "java.lang.AssertionError: sqlplus comment\n" +
		 * "	at oracle.dbtools.parser.NekotRexel.tokenize(NekotRexel.java:128)\n" +
		 * "	at oracle.dbtools.parser.NekotRexel.parse(NekotRexel.java:314)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:527)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:482)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:475)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:459)\n" +
		 * "	at oracle.dbtools.parser.LexerToken.parse(LexerToken.java:425)\n" +
		 * "	at oracle.dbtools.parser.Lexer.parse(Lexer.java:11)\n" +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptRunner.runPLSQL(ScriptRunner.java:330)\n"
		 * +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptRunner.run(ScriptRunner.java:245)\n"
		 * +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:344)\n"
		 * +
		 * "	at oracle.dbtools.raptor.newscriptrunner.ScriptExecutor.run(ScriptExecutor.java:227)\n"
		 * +
		 * "	at de.opal.tests.SQLclTestParserError.main(SQLclTestParserError.java:146)\n"
		 * + " Hello world2\n";
		 * 
		 */

		// output error message if not null
		String newErrString = SQLclUtil.getErrMessage();
		if (!newErrString.isEmpty())
			System.err.println(newErrString);
		
		Msg.println("AFTER: " + newErrString);

		// reset err
		SQLclUtil.resetErrStream();

		Msg.println("end run ...");

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");
		System.out.println(results);

		Msg.println("*** END");
	}

}
