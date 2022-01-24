/**
 * Wrapper for all batch files to install the patch in the different variations
 */
package de.opal.installer.zip;

import java.util.ArrayList;
import java.util.TreeSet;

import de.opal.installer.config.ConfigManager;

/**
 * @author daust
 *
 */
public class BatchFileWrapper {

	private ConfigManager configManager = null;
	
	private ArrayList<SqlInstallFileWrapper> sqlInstallFiles = null;

	public enum OperatingSystem {
		Windows, Linux
	}

	public enum ScriptRunner {
		sqlplus, sqlcl
	}

	private TreeSet<String> connectionPoolsSet = null; // for the header information

	/**
	 * Constructor
	 */
	public BatchFileWrapper(ConfigManager configManager) {
		this.connectionPoolsSet = new TreeSet<String>();
		this.sqlInstallFiles = new ArrayList<SqlInstallFileWrapper>();
		this.configManager = configManager;
	}

	private String computeHeader(OperatingSystem os, ScriptRunner scriptRunner) {
		String headerString = "";

		if (os == OperatingSystem.Windows) {
			// first generic header
			headerString += "@echo off\n";
			
			if (scriptRunner == ScriptRunner.sqlcl)
				headerString += "set SCRIPTRUNNER=sql" + "\n";
			else
				headerString += "set SCRIPTRUNNER=sqlplus" + "\n";

			// now the logins
			for (String connPoolName : connectionPoolsSet) {
				if (scriptRunner == ScriptRunner.sqlcl)
					headerString += "SET /P LOGIN_" + connPoolName.toUpperCase()
							+ "=Login for SCHEMA1 (e.g. scott/tiger@localhost:1521:xe):" + "\n";
				else
					headerString += "SET /P LOGIN_" + connPoolName.toUpperCase()
							+ "=Login for SCHEMA1 (e.g. scott/tiger@localhost:1521/xe):" + "\n";
			}
			headerString += "\nFOR /f %%a in ('WMIC OS GET LocalDateTime ^| find \".\"') DO set DTS=%%a\n" + 
					"set DATE_STRING=%DTS:~0,4%-%DTS:~4,2%-%DTS:~6,2%-%DTS:~8,2%.%DTS:~10,2%.%DTS:~12,2%\n" + 
					"md logs\\%DATE_STRING%\n" + 
					"@echo set log folder to: %DATE_STRING%\n";
		} else { // Linux
			// first generic header
			if (scriptRunner == ScriptRunner.sqlcl)
				headerString += "export SCRIPTRUNNER=sql" + "\n";
			else // sqlplus
				headerString += "export SCRIPTRUNNER=sqlplus" + "\n";

			// now the logins
			for (String connPoolName : connectionPoolsSet) {
				if (scriptRunner == ScriptRunner.sqlcl)
					headerString += "read -p 'Enter login for " + connPoolName.toUpperCase()
							+ " (e.g. scott/tiger@localhost:1521:xe): ' LOGIN_" + connPoolName.toUpperCase()
							+ "\n";
				else
					headerString += "read -p 'Enter login for " + connPoolName.toUpperCase()
							+ " (e.g. scott/tiger@localhost:1521/xe): ' LOGIN_" + connPoolName.toUpperCase()
							+ "\n";
			}
			headerString += "\nexport DATE_STRING=`date +\"%Y-%m-%d-%H.%M.%S\"`\n" + "mkdir -p logs/$DATE_STRING\n"
					+ "";
		}

		return headerString;
	}

	private String computeContents(OperatingSystem os, ScriptRunner scriptRunner) {
		String contents = "";

		for (SqlInstallFileWrapper sqlInstallFile : sqlInstallFiles) {
			String nslLangEncoding = configManager.convertOSEncodingToNLS_LANG(sqlInstallFile.getEncoding());

			if (os == OperatingSystem.Windows) {
				if (scriptRunner == ScriptRunner.sqlcl)
					contents += "set JAVA_TOOL_OPTIONS=\"-Duser.language=en -Dfile.encoding="
							+ sqlInstallFile.getEncoding() + "\"" + "\n";
				else
					contents += "set NLS_LANG=" + nslLangEncoding + "\n";

				contents += "%SCRIPTRUNNER% %LOGIN_" + sqlInstallFile.getConnPoolName().toUpperCase() + "% @"
						+ sqlInstallFile.getFilename() + " " + sqlInstallFile.getFilename()
						+ " logs\\%DATE_STRING%" + "\n";
				contents += "IF %ERRORLEVEL% NEQ 0 goto :END\n";

			} else { // Linux
				if (scriptRunner == ScriptRunner.sqlcl)
					contents += "export JAVA_TOOL_OPTIONS=\"-Duser.language=en -Dfile.encoding="
							+ sqlInstallFile.getEncoding() + "\"" + "\n";
				else
					contents += "export NLS_LANG=" + nslLangEncoding + "\n";

				contents += "$SCRIPTRUNNER $LOGIN_" + sqlInstallFile.getConnPoolName().toUpperCase() + " @"
						+ sqlInstallFile.getFilename() + " " + sqlInstallFile.getFilename()
						+ " logs/$DATE_STRING" + "\n";
				contents += "(($? != 0)) && { printf '%s\\n' \"Script exited, ignore 'raise_application_error', it is there for technical reasons. \"; exit 1; }\n";
			}
			contents += "\n";
			
		}

		return contents;
	}

	private String computeFooter(OperatingSystem os, ScriptRunner scriptRunner) {
		String footer = "";

		if (os.equals(OperatingSystem.Windows)) {
			footer += ":END\n" + "@echo.\n" + 
					"@echo copy all log files into a single one: logs\\%DATE_STRING%\\_combined_logfile.log\n" + 
					"for /f \"tokens=*\" %%s in ('dir /b logs\\%DATE_STRING%\\script*.log ^| sort') do type logs\\%DATE_STRING%\\%%s >> logs\\%DATE_STRING%\\_combined_logfile.log\n" + 
					"\n" + 
					"cmd /k";
		} else { // Linux
			footer += "# copy all log files into a single one: logs/$DATE_STRING/_combined_logfile.log\n" + 
					"cat logs/$DATE_STRING/*.log > logs/$DATE_STRING/_combined_logfile.log\n" + 
					"";
		}

		return footer;
	}

	public String getContents(OperatingSystem os, ScriptRunner scriptRunner) {

		return computeHeader(os, scriptRunner) + "\n" + computeContents(os, scriptRunner)
				+ "\n" + computeFooter(os, scriptRunner);
	}

	public void addScript(SqlInstallFileWrapper sqlInstallFile) {
		this.sqlInstallFiles.add(sqlInstallFile);
		this.connectionPoolsSet.add(sqlInstallFile.getConnPoolName());
	}

}
