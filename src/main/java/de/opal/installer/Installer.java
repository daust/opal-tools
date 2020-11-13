package de.opal.installer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.config.ConfigConnectionMapping;
import de.opal.installer.config.ConfigData;
import de.opal.installer.config.ConfigManager;
import de.opal.installer.db.ConnectionManager;
import de.opal.installer.util.FileNode;
import de.opal.installer.util.Filesystem;
import de.opal.installer.util.Logfile;
import de.opal.installer.util.Utils;
import de.opal.utils.MsgLog;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.newscriptrunner.CommandRegistry;
import oracle.dbtools.raptor.newscriptrunner.SQLCommand.StmtSubType;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;
import oracle.dbtools.raptor.scriptrunner.commands.rest.RESTCommand;

public class Installer {
	public static final Logger log = LogManager.getLogger(Installer.class.getName());

	private String version = ""; // will be loaded from file version.txt which will be populated by the gradle
									// build process

	private ConfigManager configManager;
	private ConfigManager configManagerConnectionPools;
	private ConnectionManager connectionManager;

	private String configFileName;
	private String connectionPoolFileName;

	private HashMap<String, ScriptExecutor> sqlcls = new HashMap<String, ScriptExecutor>();
	private Logfile logfile;

	private PatchRegistry patchRegistry;

	private boolean validateOnly = false; // default is execute
	private String userIdentity;
	private List<String> mandatoryAttributes;

//	public enum RunMode {
//		  EXECUTE,
//		  VALIDATE_ONLY
//		}

	private void validateMandatoryAttributes() {
		ConfigData data = this.configManager.getConfigData();
		Class<?> configDataclass = data.getClass();

		for (String attr : mandatoryAttributes) {
			try {
				Field field = configDataclass.getDeclaredField(attr);

				String strValue = (String) field.get(data);

				if (strValue == null || strValue.isEmpty()) {
					throw new RuntimeException(
							"Attribute \"" + attr + "\" in file " + this.configFileName + " cannot be empty.");
				}

			} catch (Exception e) {
				// TODO: handle exception
				System.err.println(e.getLocalizedMessage());
				System.exit(1);
			}
		}

	}

	public Installer(boolean validateOnly, String configFileName, String connectionPoolFileName, String userIdentity,
			List<String> mandatoryAttributes) throws IOException {
		this.validateOnly = validateOnly;
		this.configFileName = configFileName;
		this.connectionPoolFileName = connectionPoolFileName;
		this.userIdentity = userIdentity;
		this.mandatoryAttributes = mandatoryAttributes;

		this.readVersionFromFile();
		this.configManager = new ConfigManager(this.configFileName);

		// replace placeholders in opal-installer.json file
		// only replace them during installer, not setup
		this.configManager.replacePlaceholders();

		// validate the mandatory attributes in the config file
		validateMandatoryAttributes();

		// after the config parameters have been initialized and read from the config
		// file,
		// the runMode from the command line can be set
		if (this.validateOnly)
			this.configManager.getConfigData().runMode = "VALIDATE_ONLY";
		else
			this.configManager.getConfigData().runMode = "EXECUTE";

		// now read the connection pools from a different file
		// both have the same structure
		this.configManagerConnectionPools = new ConfigManager(this.connectionPoolFileName);
		// encrypt passwords if required
		if (this.configManagerConnectionPools.hasUnencryptedPasswords()) {
			this.configManagerConnectionPools.encryptPasswords(
					this.configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFileName));
			// dump JSON file
			this.configManagerConnectionPools.writeJSONConfPool();
		}
		// now decrypt the passwords so that they can be used internally in the program
		this.configManagerConnectionPools.decryptPasswords(
				this.configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFileName));

		this.connectionManager = ConnectionManager.getInstance();
		// store definitions but don't create connections
		this.connectionManager.initialize(this.configManagerConnectionPools.getConfigData().connectionPools);

	}

	// empty constructor
	public Installer() {

	}

	private void readVersionFromFile() {
		Properties prop = new Properties();
		String result = "";

		try (InputStream inputStream = getClass().getResourceAsStream("version.properties")) {

			prop.load(inputStream);
			result = prop.getProperty("version");

		} catch (IOException e) {
			e.printStackTrace();
		}

		this.version = result;
	}

	private String generateLogFileName(String logFileDir, String runMode, String env) {
		String logFileName = "";
		String runModeString = "";

		switch (runMode) {
		case "EXECUTE":
			runModeString = "install";
			break;
		case "VALIDATE_ONLY":
			runModeString = "validate";
			break;

		default:
			runModeString = "run";
			break;
		}
		logFileName = logFileDir + File.separator + env + "-" + runModeString + "-"
				+ new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".log";

		return logFileName;
	}

	/**
	 * run()
	 * 
	 * - initialize connection pool - run stuff - close connection pool
	 * 
	 * @throws Exception
	 * 
	 * @throws SQLException
	 */
	public void run() throws Exception {
		long startTime = System.currentTimeMillis();
		List<FileNode> fsTree, fsTreeFull;

		String logFileDir = this.configManager.getPackageDir().getAbsolutePath() + File.separator + "logs";
		String logfileName = generateLogFileName(logFileDir, this.configManager.getConfigData().runMode,
				this.configManagerConnectionPools.getConfigData().targetSystem);

		MsgLog.createLogDirectory(logFileDir);
		MsgLog.createLogfile(logfileName);

		// run application
		log.debug("run()");
		try {
			Filesystem fs = new Filesystem(configManager.getSqlDir());

			MsgLog.println("OPAL Installer version " + this.version);
			MsgLog.println("*************************");
			MsgLog.println("** Application           : " + this.configManager.getConfigData().application);
			MsgLog.println("** Patch                 : " + this.configManager.getConfigData().patch);
			MsgLog.println("** Version               : " + this.configManager.getConfigData().version);
			MsgLog.println("** Author                : " + this.configManager.getConfigData().author);
			MsgLog.println("**");
			MsgLog.println(
					"** Target system         : " + this.configManagerConnectionPools.getConfigData().targetSystem);
			MsgLog.println("** Run mode              : " + this.configManager.getConfigData().runMode);
			MsgLog.println("**");
			MsgLog.println("** Config File           : " + this.configManager.getConfigFileName());
			MsgLog.println("** SQL directory         : " + this.configManager.getSqlDir());
			MsgLog.println("** Connection Pool File  : " + this.configManagerConnectionPools.getConfigFileName());
			MsgLog.println("**");
			MsgLog.println("** File Encoding (System): " + System.getProperty("file.encoding"));
			MsgLog.println("** Current User          : " + this.userIdentity);
			MsgLog.println("*************************\n");

			Utils.waitForEnter("Please press <enter> to list the files to be installed ...");

			// scan all files in tree and store in TreeFull
			fsTreeFull = fs.scanTree();

			// different traversal strategies, generate ordered list of files to process
			if (configManager.getTraversalType() == ConfigManager.TraversalType.STATIC_FILES) {
				fsTree = fs.filterTreeStaticFiles(fsTreeFull, configManager.getConfigData().staticFiles);
			} else {
				fsTree = fs.filterTreeInorder(fsTreeFull, configManager.getConfigData().sqlFileRegEx, logfile,
						configManager);
			}
			Utils.waitForEnter("\nPlease press <enter> to start the process ("
					+ this.configManager.getConfigData().runMode + ") ...");
			MsgLog.logfilePrintln("");

			/*
			 * now process all files one by one
			 */
			// initialize registry targets if defined in config file and EXECUTE-mode, not
			// during VALIDATE_ONLY
			if (this.configManager.getConfigData().runMode.equals("EXECUTE")) {

				if (this.configManager.getConfigData().registryTargets != null) {
					log.debug("registryTargets:" + this.configManager.getConfigData().registryTargets.toString());

					// read releaseNotes file if exists
					String releaseNotesFilename = this.configManager.getPackageDir().getAbsolutePath() + File.separator
							+ "ReleaseNotes.txt";
					File releaseNotesFile = new File(releaseNotesFilename);
					String releaseNotesContents = "";
					if (releaseNotesFile != null) {
						releaseNotesContents = FileUtils.readFileToString(releaseNotesFile);
						log.debug("ReleaseNotes.txt : \n");
					}

					// initialize patch registry and register patch
					this.patchRegistry = new PatchRegistry(this.configManager.getConfigData().registryTargets, this);
					this.patchRegistry.registerPatch(configManager.getConfigData().application,
							configManager.getConfigData().patch, configManager.getConfigData().version,
							configManager.getConfigData().author, configManager.getConfigFileName(),
							configManagerConnectionPools.getConfigFileName(), releaseNotesContents,
							configManagerConnectionPools.getConfigData().targetSystem);
				} else {
					log.debug("*** no registry targets defined ");
				}
			}

			processTree(fsTree);

			/*
			 * Finalize patch, update column pat_ended_on
			 */
			if (this.configManager.getConfigData().runMode.equals("EXECUTE")) {
				if (this.configManager.getConfigData().registryTargets != null) {
					patchRegistry.finalizePatch();
				}
			}

			displayStatsFooter(fsTree.size(), startTime);
			MsgLog.logfilePrintln("\ndone.");

		} catch (Exception e) {
			log.error(e.getMessage());
			this.connectionManager.closeAllConnections();
			MsgLog.closeLogfile();

			// reraise exception
			throw (e);
		} finally {
			this.connectionManager.closeAllConnections();
			MsgLog.closeLogfile();
		}
	}

	private void displayStatsFooter(int totalObjectCnt, long startTime) throws IOException {
		long finish = System.currentTimeMillis();
		long timeElapsed = finish - startTime;
		int minutes = (int) (timeElapsed / (60 * 1000));
		int seconds = (int) ((timeElapsed / 1000) % 60);
		String timeElapsedString = String.format("%d:%02d", minutes, seconds);

		MsgLog.println("\n*** " + totalObjectCnt + " files were processed in " + timeElapsedString + " [mm:ss].");
	}

	public ScriptExecutor getScriptExecutorByDsName(String dsName) throws SQLException {
		Connection conn;
		ScriptExecutor sqlcl;
		ScriptRunnerContext ctx;

		log.debug("getScriptExecutor for dsName: " + dsName);

		// look for existing ScriptExecutor for this dataSource
		sqlcl = this.sqlcls.get(dsName);
		if (sqlcl == null) {
			log.debug("sqlcl executor not in HashMap, create new for dsName: " + dsName);

			// not found, create a new one
			// then look up dataSourceConnection from Connection Pool
			conn = this.connectionManager.getConnection(dsName);

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

			this.sqlcls.put(dsName, sqlcl);
		}

		return sqlcl;
	}

	/**
	 * Retrieves or creates the corresponding ScriptExecutor based on the filename
	 * 
	 * @param filename
	 * @return
	 * @throws SQLException
	 */
	private ScriptExecutor getScriptExecutor(String filename) throws SQLException {
		Connection conn;
		ScriptExecutor sqlcl;
		ScriptRunnerContext ctx;
		ArrayList<ConfigConnectionMapping> connectionMappings;
		String dsName = "";

		log.debug("getScriptExecutor for file: " + filename);

		// first find matching dataSource
		connectionMappings = this.configManager.getConfigData().connectionMappings;
		for (ConfigConnectionMapping configConnectionMapping : connectionMappings) {
			Pattern p = Pattern.compile(configConnectionMapping.matchRegEx);

			log.debug("test mapping: " + configConnectionMapping.connectionPoolName + " with "
					+ configConnectionMapping.matchRegEx);
			if (p.matcher(filename).find()) {
				dsName = configConnectionMapping.connectionPoolName;
				log.debug("process file " + filename + " with dataSource: " + dsName);

				break;
			} else {
				log.debug("  no match with regex: " + configConnectionMapping.matchRegEx);
			}
		}
		if (dsName.isEmpty()) {
			throw new RuntimeException("no match found for path: " + filename);

		}

		// look for existing ScriptExecutor for this dataSource
		sqlcl = this.sqlcls.get(dsName);
		if (sqlcl == null) {
			log.debug("sqlcl executor not in HashMap, create new for dsName: " + dsName);

			// not found, create a new one
			// then look up dataSourceConnection from Connection Pool
			conn = this.connectionManager.getConnection(dsName);

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

			this.sqlcls.put(dsName, sqlcl);
		}

		return sqlcl;
	}

	/**
	 * processes a tree in the order it is given
	 * 
	 * @param tree
	 * @param traversalType
	 * @throws SQLException
	 * @throws IOException
	 */
	private void processTree(List<FileNode> tree) throws SQLException, IOException {
		log.debug("*** processTree");

		for (FileNode fileNode : tree) {
			log.debug("process file: " + fileNode.getFile().getAbsolutePath());
			ScriptExecutor sqlcl = this.getScriptExecutor(fileNode.getFile().getAbsolutePath());
			executeFile(fileNode.getFile(), sqlcl);
		}
	}

	/**
	 * 
	 * @param file
	 * @param sqlcl
	 * @throws SQLException
	 * @throws IOException
	 */
	private void executeFile(File file, ScriptExecutor sqlcl) throws SQLException, IOException {

		// Capture the results without this it goes to STDOUT
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		BufferedOutputStream buf = new BufferedOutputStream(bout);
		sqlcl.setOut(buf);

		String baseDirectoryName = configManager.getPackageDirName();
		String relativeFilename = file.getAbsolutePath().replace(baseDirectoryName, "");

		// enable all REST commands
		CommandRegistry.addForAllStmtsListener(RESTCommand.class, StmtSubType.G_S_FORALLSTMTS_STMTSUBTYPE);

		// register patch file if registry targets are defined in config file and
		// EXECUTE-mode, not
		// SIMULATE
		if (this.configManager.getConfigData().runMode.equals("EXECUTE")) {
			// register file in patch registry
			if (this.patchRegistry != null) {
				this.patchRegistry.registerFile(relativeFilename);
			}
		}

		String overrideEncoding = configManager.getEncoding(relativeFilename);
		if (overrideEncoding.isEmpty()) {
			MsgLog.print("\n*** User:" + sqlcl.getConn().getSchema() + "; " + relativeFilename);
		} else {
			MsgLog.print("*** Override encoding: " + overrideEncoding + "; User:" + sqlcl.getConn().getSchema() + "; "
					+ relativeFilename);
		}
		// only execute if flag is set in config file
		if (this.configManager.getConfigData().runMode.equals("EXECUTE")) {
			// sqlcl.setStmt(new FileInputStream(file));
			String fileContents = "";

			if (overrideEncoding.isEmpty()) {
				fileContents = FileUtils.readFileToString(file, System.getProperty("file.encoding"));
			} else {
				fileContents = FileUtils.readFileToString(file, overrideEncoding);
			}
			sqlcl.setStmt(fileContents);
			sqlcl.run();
		}

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");
		MsgLog.println(results);

		if (this.configManager.getConfigData().waitAfterEachStatement.equals("true")
				&& this.configManager.getConfigData().runMode.equals("EXECUTE")) {
			Utils.waitForEnter("Please press <enter> to continue ...");
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

		// only execute if flag is set in config file
		if (this.configManager.getConfigData().runMode.equals("EXECUTE")) {
			// sqlcl.setStmt(new FileInputStream(file));
			sqlcl.setStmt(statement);
			sqlcl.run();
		}

		String results = bout.toString("UTF8");
		results = results.replaceAll(" force_print\n", "");

		// suppress output when "name is already used by existing object
		if (!results.contains("ORA-00955")) {
			MsgLog.println(results);
		}
	}

}
