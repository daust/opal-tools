package de.opal.tests;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

import de.opal.utils.FileIO;

public class JDBCStoredProc {

	// Define database connections.
	private static String host = "vm1";
	private static String port = "1521";
	private static String dbname = "xe";
	private static String userid = "test";
	private static String passwd = "oracle1";

	private String sqlStatement = "DECLARE\n" + "\n" + "    FUNCTION get_xml RETURN CLOB IS\n"
			+ "        l_ctx   dbms_xmlgen.ctxhandle;\n" + "        l_clob  CLOB;\n" + "    BEGIN\n"
			+ "        l_ctx := dbms_xmlgen.newcontext(q'[\n" + "    SELECT\n" + "                       CURSOR (\n"
			+ "                           SELECT\n" + "                               tables.owner,\n"
			+ "                               tables.table_name,\n"
			+ "                               tables.tablespace_name,\n"
			+ "                               tables.status\n" + "                           FROM\n"
			+ "                               all_tables tables\n" + "                           WHERE\n"
			+ "                               tables.owner = sys_context(\n"
			+ "                                   'USERENV', 'CURRENT_USER'\n" + "                               )\n"
			+ "                               and rownum<10000\n" + "                       ) \"Tables\",\n"
			+ "                       CURSOR (\n" + "                                   SELECT\n"
			+ "                                       col.owner,\n"
			+ "                                       col.table_name,\n"
			+ "                                       col.column_name,\n"
			+ "                                       col.data_type,\n"
			+ "                                       col.data_length,\n"
			+ "                                       col.data_precision,\n"
			+ "                                       col.nullable,\n"
			+ "                                       col.column_id\n" + "                                   FROM\n"
			+ "                                       all_tab_cols col                                       \n"
			+ "                                       WHERE col.owner = sys_context('USERENV', 'CURRENT_USER') and rownum < 10000\n"
			+ "                               ) \"Columns\" from dual\n" + "    ]');\n"
			+ "        dbms_xmlgen.setrowsettag(\n" + "            l_ctx,\n" + "            'DOCUMENT'\n"
			+ "        );\n" + "        dbms_xmlgen.setrowtag(\n" + "            l_ctx,\n" + "            'DATA'\n"
			+ "        );\n" + "        l_clob := dbms_xmlgen.getxml(l_ctx);\n"
			+ "        dbms_xmlgen.closecontext(l_ctx);\n" + "        \n" + "        return l_clob;\n"
			+ "    --dbms_output.put_line(v_clob);\n" + "    END;\n" + "\n" + "BEGIN\n" + "    :retVal := get_xml();\n"
			+ "END;";

	public static void main(String[] args) throws IOException, SQLException {

		JDBCStoredProc myClass = new JDBCStoredProc();

		String xml = "";
		xml = myClass.getQueryResult(host, port, dbname, userid, passwd);
		System.out.println("*** output: ***");
		System.out.println(xml);
		FileIO.stringToFile(xml, "generated-out.txt");
		System.out.println("*** end: ***");
	}

	private String getQueryResult(String host, String port, String dbname, String user, String pswd)
			throws SQLException, IOException {

		// Define method variables.
		String data = null;
		Connection conn = null;
		CallableStatement cs = null;

		try {
			// Load Oracle JDBC driver.
			DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());

			// Define and initialize a JDBC connection.
			conn = DriverManager.getConnection("jdbc:oracle:thin:@" + host + ":" + port + ":" + dbname, userid, passwd);
			// Define metadata object.
			// DatabaseMetaData dmd = conn.getMetaData();

			System.out.println("connected.");

			// CallableStatement cs = conn.prepareCall("declare l_clob CLOB; begin l_clob :=
			// '1222345'; :out := l_clob; end;");
			sqlStatement = FileIO.fileToString("/private/tmp/project1/opal-tools/export-templates/synonym.sql");
			cs = conn.prepareCall(sqlStatement);
			cs.setString(1, "TEST");
			cs.setString(2, "TABLE");
			cs.setString(3, "XLIB_LOGS");
			
			System.out.println("statement prepared.");

			cs.registerOutParameter(4, Types.CLOB);

			System.out.println("outparameter registered");

			cs.execute();
			
			System.out.println("statement executed");

			Clob cl = cs.getClob(4);
			BufferedReader br = null;
			StringBuilder sb = new StringBuilder();
			String line;
			try {
				br = new BufferedReader(new InputStreamReader(cl.getAsciiStream()));
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			} catch (IOException e) {
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
					}
				}
			}

			System.out.println("string buffer read");

			// Close resources.
			cs.close();
			conn.close();
			System.out.println("resources closed");

			// Return CLOB as a String data type.
			data = sb.toString();
			// data= "12345";
			return data;
		} catch (

		SQLException e) {
			if (e.getSQLState() == null) {
				System.out.println(new SQLException("Oracle Thin Client Net8 Connection Error.",
						"ORA-" + e.getErrorCode() + ": Incorrect Net8 thin client arguments:\n\n" + "  host name     ["
								+ host + "]\n" + "  port number   [" + port + "]\n" + "  database name [" + dbname
								+ "]\n",
						e.getErrorCode()).getSQLState());

				// Return an empty String on error.
				return data;
			} else {
				System.out.println(e.getMessage());
				return data;
			}
		} finally {
			if (data == null) {
				// Close resources.
				cs.close();
				conn.close();
				System.out.println("resources closed");

				System.exit(1);
			}
		}
	}

}
