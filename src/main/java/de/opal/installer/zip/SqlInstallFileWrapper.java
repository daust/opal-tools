package de.opal.installer.zip;

public class SqlInstallFileWrapper {
	private String filename = "";
	private String contents = "";

	private String connPoolName = "";
	private String encoding = "";

	private Boolean isSilent = true;

	/**
	 * Constructor
	 */
	public SqlInstallFileWrapper() {
	}

	/**
	 * Constructor
	 * 
	 * @param filename
	 * @param contents
	 */
	public SqlInstallFileWrapper(String filename, String contents, Boolean isSilent) {
		this.filename = filename;
		this.contents = contents;
		this.isSilent = isSilent;
	}

	public SqlInstallFileWrapper(String filename, String contents, String connPoolName, String encoding,
			Boolean isSilent) {
		this.filename = filename;
		this.contents = contents;
		this.isSilent = isSilent;
		this.connPoolName = connPoolName;
		this.encoding = encoding;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getContents() {
		return getHeader() + contents + getFooter();
	}

	public void setContents(String contents) {
		this.contents = contents;
	}

	public void addLine(String line) {
		this.contents += line + "\n";
	}

	public void addScript(SqlFileWrapper sqlFile) {
		this.contents += "prompt *** Execute " + sqlFile.filenameZipFile + "\n";
		if (!this.isSilent)
			//this.contents += "accept input char prompt \"Press [Enter] to continue : \"\n";
//			this.contents += "ACCEPT FEEDBACK PROMPT 'Continue with \"<ENTER>\" and exit with \"x\": '\n" + 
//					"whenever sqlerror exit\n" + 
//					"exec if '&&FEEDBACK' = 'x' then raise_application_error( -20000, 'Goodbye' ); end if;\n" + 
//					"whenever sqlerror continue\n" + 
//					"";
			this.contents += "@prompt.sql" + "\n";
		this.contents += "@\"" + sqlFile.filenameZipFile + "\"\n\n";
	}

	private String getFooter() {
		return "\n" + "exit;" + "\n";
	}

	private String getHeader() {
		String header = "";

		header = "define SCRIPT_NAME=&&1\n" + "define LOG_DIRECTORY=&&2\n" + "\n"
				+ "spool &&LOG_DIRECTORY/&&SCRIPT_NAME..log\n"
				+ "prompt *********************************************************\n"
				+ "prompt *** Running script &&SCRIPT_NAME.\n"
				+ "prompt *********************************************************\n"
				+ "select to_char(sysdate, 'dd.mm.yyyy hh24:mi:ss') as current_date_time from dual;\n" + "\n";

		return header;
	}

	public String toString() {
		return this.filename + ": " + this.contents;
	}

	public String getConnPoolName() {
		return connPoolName;
	}

	public void setConnPoolName(String connPoolName) {
		this.connPoolName = connPoolName;
	}

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

}
