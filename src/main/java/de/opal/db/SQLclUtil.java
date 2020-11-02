package de.opal.db;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;

import de.opal.installer.util.Msg;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;

public class SQLclUtil {
	/**
	 * 
	 * @param file
	 * @param sqlcl
	 * @throws SQLException
	 * @throws IOException
	 */
	public void executeFile(File file, ScriptExecutor sqlcl, String overrideEncoding)
			throws SQLException, IOException {

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);

		// only execute if flag is set in config file
		// sqlcl.setStmt(new FileInputStream(file));
		String fileContents = "";

		if (overrideEncoding!=null) {
			fileContents = FileUtils.readFileToString(file, System.getProperty("file.encoding"));
		} else {
			fileContents = FileUtils.readFileToString(file, overrideEncoding);
		}
		sqlcl.setStmt(fileContents);
		sqlcl.run();

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");
		Msg.println(results);

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

		sqlcl.setStmt(statement);
		sqlcl.run();

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");

		// suppress output when "name is already used by existing object
		if (!results.contains("ORA-00955")) {
			Msg.println(results);
		}
	}
}
