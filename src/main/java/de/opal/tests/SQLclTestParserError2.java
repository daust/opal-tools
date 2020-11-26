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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import de.opal.installer.util.Msg;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.ISQLCommand;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.newscriptrunner.ScriptParser;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunner;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;

public class SQLclTestParserError2 {

	public static void main(String[] args) throws SQLException, IOException {
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);
		Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//vm1:1521/XE", "jri_test", "oracle1");
		conn.setAutoCommit(false);

		Msg.println("*** START");

		FileInputStream fin = new FileInputStream(new File("/private/tmp/project1/patches/2020/2020-11-25-test-bwi/sql/training/apex/02_f20160617-KapaPlaner.sql"));
		ScriptParser parser = new ScriptParser(fin);

		ISQLCommand cmd;
		// #setup the context
		ScriptRunnerContext ctx = new ScriptRunnerContext();
		ctx.setBaseConnection(conn);

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);

		ScriptRunner sr = new ScriptRunner(conn, buf, ctx);
		while ((cmd = parser.next()) != null) {
			// do something fancy based on a cmd
			sr.run(cmd);
			// check success/failure of the command

			String errMsg = (String) ctx.getProperty(ScriptRunnerContext.ERR_MESSAGE);
			if (errMsg != null) {
				// react to a failure
				System.out.println("**FAILURE**" + errMsg);
			}
		}

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");
		System.out.println(results);

		Msg.println("*** END");
	}

}
