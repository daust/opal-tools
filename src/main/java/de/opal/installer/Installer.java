package de.opal.installer;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import de.opal.installer.config.ConfigManager;
import de.opal.installer.db.ConnectionManager;
import de.opal.installer.util.FileNode;
import de.opal.installer.util.Filesystem;
import de.opal.installer.util.Logfile;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import oracle.dbtools.db.ResultSetFormatter;
import oracle.dbtools.raptor.newscriptrunner.ScriptExecutor;
import oracle.dbtools.raptor.newscriptrunner.ScriptRunnerContext;

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

//	public enum RunMode {
//		  EXECUTE,
//		  VALIDATE_ONLY
//		}

	public Installer(boolean validateOnly, String configFileName, String connectionPoolFileName) throws IOException {
		this.validateOnly = validateOnly;
		this.configFileName = configFileName;
		this.connectionPoolFileName = connectionPoolFileName;
		
		this.readVersionFromFile();		
		this.configManager = new ConfigManager(this.configFileName);

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
			this.configManagerConnectionPools
					.encryptPasswords(this.configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFileName));
			// dump JSON file
			this.configManagerConnectionPools.writeJSONConfPool();
		}
		// now decrypt the passwords so that they can be used internally in the program
		this.configManagerConnectionPools
				.decryptPasswords(this.configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFileName));
		
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
		List<FileNode> fsTree, fsTreeFull;

		String logFileDir = this.configManager.getPackageDir().getAbsolutePath() + File.separator + "logs";
		String logfileName = generateLogFileName(logFileDir, this.configManager.getConfigData().runMode,
				this.configManagerConnectionPools.getConfigData().targetSystem);

		// create logfile directory if it does not exist
		File logFileDirFile = new File(logFileDir);
		if (!logFileDirFile.exists()) {
			logFileDirFile.mkdir();
			log.debug("create log directory: " + logFileDir);
		}
		log.debug("logfile: " + logfileName);
		logfile = Logfile.getInstance();
		logfile.open(logfileName);

		// run application
		log.debug("run()");
		try {
			Filesystem fs = new Filesystem(configManager.getSqlDir());

			Msg.println("OPAL Installer version " + this.version);
			Msg.println("*************************");
			Msg.println("** Application           : " + this.configManager.getConfigData().application);
			Msg.println("** Patch                 : " + this.configManager.getConfigData().patch);
			Msg.println("** Version               : " + this.configManager.getConfigData().version);
			Msg.println("** Author                : " + this.configManager.getConfigData().author);
			Msg.println("**");
			Msg.println("** Target system         : " + this.configManagerConnectionPools.getConfigData().targetSystem);
			Msg.println("** Run mode              : " + this.configManager.getConfigData().runMode);
			Msg.println("**");
			Msg.println("** Config File           : " + this.configManager.getConfigFileName());
			Msg.println("** Connection Pool File  : " + this.configManagerConnectionPools.getConfigFileName());
			Msg.println("**");
			Msg.println("** File Encoding (System): " + System.getProperty("file.encoding"));
			Msg.println("** Current User          : " + System.getProperty("user.name"));
			Msg.println("*************************\n");

			logfile.appendln("OPAL Installer version " + this.version);
			logfile.appendln("*************************");
			logfile.appendln("** Application           : " + this.configManager.getConfigData().application);
			logfile.appendln("** Patch                 : " + this.configManager.getConfigData().patch);
			logfile.appendln("** Version               : " + this.configManager.getConfigData().version);
			logfile.appendln("** Author                : " + this.configManager.getConfigData().author);
			logfile.appendln("**");
			logfile.appendln(
					"** Target system         : " + this.configManagerConnectionPools.getConfigData().targetSystem);
			logfile.appendln("** Run mode              : " + this.configManager.getConfigData().runMode);
			logfile.appendln("**");
			logfile.appendln("** Config File           : " + this.configManager.getConfigFileName());
			logfile.appendln("** Connection Pool File  : " + this.configManagerConnectionPools.getConfigFileName());
			logfile.appendln("**");
			logfile.appendln("** File Encoding (System): " + System.getProperty("file.encoding"));
			logfile.appendln("** Current User          : " + System.getProperty("user.name"));
			logfile.appendln("*************************\n");

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
			logfile.append("\n");
			Utils.waitForEnter("\nPlease press <enter> to start the process ("
					+ this.configManager.getConfigData().runMode + ") ...");

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

			/*
			 * close connection pool
			 */
			this.connectionManager.closeAllConnections();

			logfile.close();
			log.debug("*** end");
			Msg.println("*** end");

		} catch (Exception e) {
			log.error(e.getMessage());
			this.connectionManager.closeAllConnections();
			logfile.close();

			// reraise exception
			throw (e);
		} finally {
			this.connectionManager.closeAllConnections();
		}
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

		// register patch file if registry targets are defined in config file and
		// EXECUTE-mode, not
		// SIMULATE
		if (this.configManager.getConfigData().runMode.equals("EXECUTE")) {
			// register file in patch registry
			if (this.patchRegistry != null) {
				this.patchRegistry.registerFile(file);
			}
		}

		String overrideEncoding = configManager.getEncoding(file.toString());

		// # run a whole file
		// String cmd = "@" + file.getAbsolutePath();
		this.logfile.append("\n***");
		if (overrideEncoding.isEmpty()) {
			this.logfile.append("\n*** User:" + sqlcl.getConn().getSchema() + "; " + file.getAbsolutePath());
		} else {
			this.logfile.append("\n*** Override encoding: " + overrideEncoding + "; User:" + sqlcl.getConn().getSchema()
					+ "; " + file.getAbsolutePath());
		}
		this.logfile.append("\n***\n\n");

		Msg.print("\n***");
		if (overrideEncoding.isEmpty()) {
			Msg.print("\n*** User:" + sqlcl.getConn().getSchema() + "; " + file.getAbsolutePath());
		} else {
			Msg.print("\n*** Override encoding: " + overrideEncoding + "; User:" + sqlcl.getConn().getSchema() + "; "
					+ file.getAbsolutePath());
		}
		Msg.print("\n***\n\n");

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
		this.logfile.append(results);
		Msg.println(results);

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
			this.logfile.append(results);
			Msg.println(results);
		}
	}

}
