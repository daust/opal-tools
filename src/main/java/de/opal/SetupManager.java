package de.opal;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import de.opal.installer.config.ConfigConnectionMapping;
import de.opal.installer.config.ConfigConnectionPool;
import de.opal.installer.config.ConfigData;
import de.opal.installer.config.ConfigEncodingMapping;
import de.opal.installer.config.ConfigManager;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;

public class SetupManager {

	public static final Logger log = LogManager.getLogger(SetupManager.class.getName());
	private String swDirectory = "";
	private String localConfigDirectory = "";
	private String dbSourceDirectory = "";
	private String templateDirectory = "";
	private String patchDirectory = "";
	private String setProjectEnvironmentScript = "";
	private String schemaListString = "";
	private String schemaListArr[] = null;
	private String environmentListString = "";
	private String environmentListArr[] = null;

	private String environmentColorListString = "";
	private String environmentColorListArr[] = null;
	
	private String environmentExportConnection=null;

	String localDir = System.getProperty("user.dir");

	String tmpSourceDir = "";
	String tmpTargetDir = "";
	String[] osScriptSuffixList = new String[] { getOsDependentScriptSuffix() };
	FileFilter osFileFilter = new FileFilter() {
		// Override accept method
		public boolean accept(File file) {

			// if the file extension is .log return true, else false
			if (file.getName().endsWith("." + getOsDependentScriptSuffix()) || file.getName().endsWith(".txt")) {
				return true;
			}
			return false;
		}
	};

	@Option(name = "-d", aliases = "--project-root-directory", usage = "sets the root directory for the installation. Can be changed by the user, everything is prompted.", metaVar = "<directory>")
	private String projectRootDir = ".";

	@Option(name = "-s", aliases = "--show-passwords", usage = "when prompted for passwords, they will be shown in clear text")
	private boolean showPasswords = false;

	@Option(name = "-h", aliases = "--help", usage = "display this help page")
	private boolean showHelp = false;

	public static void main(String[] args) throws IOException, InterruptedException {
		SetupManager config = new SetupManager();
		config.parseParameters(args);
		config.run();
	}

	public void parseParameters(String[] args) throws IOException {
		ParserProperties properties = ParserProperties.defaults();
		properties.withUsageWidth(130);
		properties.withOptionSorter(null);
		CmdLineParser parser = new CmdLineParser(this, properties);

		log.debug("start parsing");
		try {
			// parse the arguments.
			parser.parseArgument(args);

			if (showHelp) {
				Msg.println("");
				Msg.println("Configures the initial setup, copies the files into the right location.");
				Msg.println("");
				Msg.println("Options: ");
				Msg.println("");
				parser.printUsage(System.out);
				Msg.println("");

				// print option sample. This is useful some time
				// System.out.println(" Example: java de.opal.exporter.DBExporter" +
				// parser.printExample(ALL));

				System.exit(0);
			}

		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			System.err.println("\njava de.opal.SetupManager [options...]");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// print option sample. This is useful some time
			System.err.println("  Example: java de.opal.SetupManager" + parser.printExample(ALL));

			System.exit(1);
		}

	}

	private String promptForInput(Scanner kbd, String prompt, String defaultValue) {
		String input = "";

		// Scanner kbd = new Scanner(System.in); // Create a Scanner object
		Msg.print(prompt + " [" + defaultValue + "]: ");
		input = kbd.nextLine();
		if (input.isEmpty()) {
			input = defaultValue;
		}

		// kbd.close();
		return input;
	}

	private Boolean osIsWindows() {
		return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
	}

	private String getOsDependentScriptSuffix() {
		if (osIsWindows()) {
			return "cmd";
		} else {
			return "sh";
		}

	}

	private String getOsDependentProjectRootVariable() {
		if (osIsWindows()) {
			return "%PROJECT_ROOT%";
		} else {
			return "${PROJECT_ROOT}";
		}

	}

	private String getFullPathResolveVariables(String path) {
		String newPath = path;

		// replace variables
		if (osIsWindows()) {
			newPath = newPath.replace("%PROJECT_ROOT%", this.projectRootDir);
		} else {
			newPath = newPath.replace("${PROJECT_ROOT}", this.projectRootDir);
		}
		// resolve path and make it absolute
		try {
			newPath = new File(newPath).getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return newPath;
	}

	private void processUserConfDir(Scanner kbd) throws IOException {
		String tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "conf-user");
		String tmpTargetDir = getFullPathResolveVariables(localConfigDirectory);

		Msg.println("\nprocess local conf directory in: " + tmpTargetDir + "\n");

		// loop over all environments, create a new file for each one
		// create CONNECTION POOL files
		for (String env : environmentListArr) {

			// create connection pool file
			String confFilename = tmpTargetDir + File.separator + "connections-" + env + ".json";
			Msg.println("  Process environment: " + env + " => " + confFilename);

			// first we create an empty file
			FileUtils.writeStringToFile(new File(confFilename), "{}", Charset.defaultCharset());

			// prompt for url for all connections in this file
			String envJDBCUrl = promptForInput(kbd, "    JDBC url for environment " + env + ": ",
					"jdbc:oracle:thin:@127.0.0.1:1521:xe");

			ConfigData configData = new ConfigData();
			configData.clearDefaults();
			configData.targetSystem = env;

			// loop over all schemas for the current environment
			for (String schema : schemaListArr) {
				String password = "";

				Console con = System.console();
				// hide password on real console
				if (con == null || showPasswords) {
					// input password in Eclipse
					password = promptForInput(kbd,
							"    Password for schema " + schema + " in environment " + env + ": ", "");
				} else {
					System.out.println("    Password for schema " + schema + " in environment " + env + ": ");
					char[] ch = con.readPassword();
					password = String.valueOf(ch);
				}
				ConfigConnectionPool conn = new ConfigConnectionPool(schema, schema, password, envJDBCUrl);
				// add connection to configFile
				configData.connectionPools.add(conn);
			}

			ConfigManager confMgr = new ConfigManager(confFilename);
			confMgr.setConfigData(configData);
			confMgr.encryptPasswords();

			confMgr.writeJSONConfPool();
		}
		// process SET PROJECT ENVIRONMENT script
		// replace contents in setProjectEnvironment script
		File f = new File(getFullPathResolveVariables(
				tmpSourceDir + File.separator + "setProjectEnvironment." + getOsDependentScriptSuffix()));

		Path path = Paths.get(f.getName());
		path.getFileName().toString();
		String contents = FileUtils.readFileToString(f, Charset.defaultCharset());
		contents = replaceAllVariables(contents);

		FileUtils.writeStringToFile(new File(getFullPathResolveVariables(this.setProjectEnvironmentScript)), contents,
				Charset.defaultCharset());

		Msg.println("");
	}

	private void processSoftwareInstallation(Scanner kbd) throws IOException {

		tmpSourceDir = getFullPathResolveVariables(localDir + File.separatorChar + "lib");
		tmpTargetDir = getFullPathResolveVariables(swDirectory + File.separatorChar + "lib");

		Msg.println("\n----------------------------------------------------------\n");
		Msg.println("copy sw files from: " + tmpSourceDir + "\n              to  : " + tmpTargetDir + "\n");
		try {
			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		} catch (IOException e) {
			Msg.println("Files will NOT be copied because an error occured: " + e.getMessage() + "\n");
		}
	}

	private void processConfDirectory(Scanner kbd) {
		tmpSourceDir = getFullPathResolveVariables(localDir + File.separatorChar + "conf");
		tmpTargetDir = getFullPathResolveVariables(swDirectory + File.separatorChar + "conf");

		Msg.println("\n----------------------------------------------------------\n");
		Msg.println("copy sw files from :" + tmpSourceDir + "\n              to  : " + tmpTargetDir + "\n");
		try {
			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		} catch (IOException e) {
			Msg.println("Files will NOT be copied because an error occured: " + e.getMessage() + "\n");
		}

	}

	private void processPatchTemplateDirectory(Scanner kbd) throws IOException {
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template");
		tmpTargetDir = getFullPathResolveVariables(templateDirectory);

		Msg.println("copy template directory from: " + tmpSourceDir + "\n                        to  : " + tmpTargetDir
				+ "\n");
		FileUtils.forceMkdir(new File(tmpTargetDir));

		// loop over all schemas to create sql subdirectories
		for (String schema : schemaListArr) {
			tmpSourceDir = getFullPathResolveVariables(
					localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template-sql");
			tmpTargetDir = getFullPathResolveVariables(
					templateDirectory + File.separator + "sql" + File.separator + schema);

			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		}

		// create new patch-install files for each environment
		// copy / replace all files but the #VAR#... files
		String patchFileHeader = "";
		String patchFileContent = "";

		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template");
		tmpTargetDir = getFullPathResolveVariables(templateDirectory);

		Iterator<File> it = FileUtils.iterateFiles(new File(tmpSourceDir), null, false);
		int i = 2; // start counter for #NO# files with 3
		while (it.hasNext()) {
			File f = (File) it.next();
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();

			// filter for operating system
			// on *nix process .sh files, on Windows process *.cmd files
			if (filename.endsWith(getOsDependentScriptSuffix())
					|| (!filename.endsWith(".cmd") && !filename.endsWith(".sh"))) {

				Msg.println("  process file " + filename);

				String contents = FileUtils.readFileToString(f, Charset.defaultCharset());
				contents = replaceAllVariables(contents);

				if (filename.contains("#")) {
					// do nothing ... will be processed later
				} else if (filename.startsWith("PatchFiles-header.txt")) {
					// add the header to the beginning of the content
					patchFileHeader = FileUtils.readFileToString(
							new File(tmpSourceDir + File.separator + path.getFileName()), Charset.defaultCharset());
				} else if (filename.startsWith("PatchFiles-body.txt")) {
					String templateMapping = FileUtils.readFileToString(
							new File(tmpSourceDir + File.separator + path.getFileName()), Charset.defaultCharset());
					// loop over all schemas and create a mapping for each one
					for (String schema : schemaListArr) {
						patchFileContent += templateMapping.replace("#SCHEMA#", schema);
					}

				} else {
					// nothing special here
					FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + path.getFileName()), contents,
							Charset.defaultCharset());
				}
				// write the patchFile.txt
				FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + "PatchFiles.txt"),
						patchFileHeader + "\n" + patchFileContent, Charset.defaultCharset());
			}
		}
		// process validation files and installation files "#var#..."
		// special handling for each environment
		i = 2;
		for (String env : environmentListArr) {
			File f = new File(
					tmpSourceDir + File.separatorChar + "3.install-patch-#ENV#." + getOsDependentScriptSuffix());
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();
			String contents = FileUtils.readFileToString(f, Charset.defaultCharset());

			String newFilename = filename.replace("#NO#", "" + i++).replace("#ENV#", env);
			contents = contents.replace("#ENV#", env);
			contents = replaceAllVariables(contents, env, null);

			FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + newFilename), contents,
					Charset.defaultCharset());
		}
		// process installation files and installation files "#NO#..."
		// special handling for each environment
		i = 2;
		for (String env : environmentListArr) {
			File f = new File(
					tmpSourceDir + File.separatorChar + "2.validate-patch-#ENV#." + getOsDependentScriptSuffix());
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();
			String contents = FileUtils.readFileToString(f, Charset.defaultCharset());

			String newFilename = filename.replace("#NO#", "" + i++).replace("#ENV#", env);
			contents = contents.replace("#ENV#", env);
			contents = replaceAllVariables(contents, env, null);
			FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + newFilename), contents,
					Charset.defaultCharset());
		}

		// add connection pool mappings to file system paths
		// read opal-installer.json file
		// ConfigData configDataInst = new ConfigData();
		// configDataInst.clearDefaults();
		String fileContents = "{\n" + "	\"application\": \"\",\n" + "    \"patch\": \"\",\n" + "    \"author\": \"\",\n"
				+ "    \"version\": \"\",\n" + "    \"connectionMappings\": [],\n"
				+ "	 \"waitAfterEachStatement\": \"true\"," + "    \"sqlFileRegEx\": \"\\\\.(sql|pks|pkb|trg)$\",\n"
				+ "    \"registryTargets\": [],\n" + "    \"encodingMappings\": [ ]	\n" + "}";
		FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + "opal-installer.json"), fileContents,
				Charset.defaultCharset());
		ConfigManager confMgrInst = new ConfigManager(tmpTargetDir + File.separator + "opal-installer.json");

		// loop over all schemas for the current environment
		for (String schema : schemaListArr) {
			ConfigConnectionMapping map = null;
			if (osIsWindows()) {
				map = new ConfigConnectionMapping(schema, "\\\\sql\\\\.*" + schema + ".*");
			} else {
				map = new ConfigConnectionMapping(schema, "/sql/.*" + schema + ".*");
			}

			// add connection to configFile
			confMgrInst.getConfigData().connectionMappings.add(map);
		}
		// add encoding mapping
		ConfigEncodingMapping map = null;
		if (osIsWindows()) {
			map = new ConfigEncodingMapping("UTF8", "\\\\sql\\\\.*apex.*\\\\.*f*sql");
		} else {
			map = new ConfigEncodingMapping("UTF8", "/sql/.*apex.*/.*f*sql");
		}
		confMgrInst.getConfigData().encodingMappings.add(map);

		// write opal-installer.json file
		confMgrInst.writeJSONConfInitFile();

	}

	private void processDBSourceDirectory(Scanner kbd) throws IOException {
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "src-sql");

		Msg.println("db source directory from: " + tmpSourceDir + "\n                    to  : " + dbSourceDirectory
				+ "\n");

		// loop over all schemas
		for (String schema : schemaListArr) {
			tmpTargetDir = getFullPathResolveVariables(dbSourceDirectory + File.separator + schema);

			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		}
	}

	private void processPatchDirectory(Scanner kbd) throws IOException {
		tmpSourceDir = getFullPathResolveVariables(localDir + File.separatorChar + "patches");
		tmpTargetDir = getFullPathResolveVariables(patchDirectory);

		Msg.println("patch directory from: " + localDir + File.separatorChar + "patches" + "\n                to  : "
				+ tmpTargetDir + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		FileUtils.forceMkdir(new File(tmpTargetDir));

	}

	private void processBinDirectory(Scanner kbd) throws IOException {
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "bin");
		tmpTargetDir = getFullPathResolveVariables(swDirectory + File.separator + "bin");

		Msg.println("process bin directory\n");

		// copy / replace all files in the bin directory
		Iterator<File> it1 = FileUtils.iterateFiles(new File(tmpSourceDir), osScriptSuffixList, false);
		while (it1.hasNext()) {
			File f = (File) it1.next();
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();
			Msg.println("  process file " + filename);
			String contents = FileUtils.readFileToString(f, Charset.defaultCharset());

			if (filename.contains("#ENV#")) {
				// iterate over all environments
				for (String env : environmentListArr) {
					String newFilename = filename.replace("#ENV#", env);
					contents = replaceAllVariables(contents, env, null);
					FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + newFilename), contents,
							Charset.defaultCharset());
				}
			} else if (filename.contains("#SCHEMA#")) {
				// iterate over all schemas
				for (String schema : schemaListArr) {
					String newFilename = filename.replace("#SCHEMA#", schema);
					contents = replaceAllVariables(contents, this.environmentExportConnection, schema);
					FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + newFilename), contents,
							Charset.defaultCharset());
				}

			} else {
				// write the single file back and replace placeholders
				contents = replaceAllVariables(contents);
				FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + path.getFileName()), contents,
						Charset.defaultCharset());
			}
		}

		// ----------------------------------------------------------
		// make shell scripts executable again, got lost during file copy
		// ----------------------------------------------------------
		if (osIsWindows()) {
			// builder.command("cmd.exe", "/c", "dir");
			// nothing to do here, privileges are working
		} else {
			Msg.println("\nset privileges for *.sh files\n");
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("bash", "-c", "find " + getFullPathResolveVariables(localConfigDirectory) + " "
					+ getFullPathResolveVariables(swDirectory) + " " + getFullPathResolveVariables(templateDirectory)
					+ " -type f -iname \"*.sh\" -exec chmod +x {} \\;");
			try {
				Process process = builder.start();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}

				int exitCode = process.waitFor();
				System.out.println("\nExited with error code : " + exitCode);

			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private String getColor(String env) {
		String color = null;

		// determine index of environment list
		// and get the corresponding entry from the environment color list based on the
		// index
		// if there is no entry, return null;
		for (int i = 0; i < this.environmentListArr.length; i++) {
			if (environmentListArr[i].contentEquals(env)) {
				if (environmentColorListArr.length > i) {
					color = environmentColorListArr[i];
				}
			}
		}

		return color;
	}

	/*
	 * replace all placeholders in a file/string. Sometimes it is dependent on the
	 * environment, sometimes on the schema
	 */
	private String replaceAllVariables(String contents, String env, String schema) {
		String newContents = contents;

		newContents = newContents.replace("#PROJECT_ROOT#", this.projectRootDir);
		newContents = newContents.replace("#OPAL_TOOLS_USER_CONFIG_DIR#", this.localConfigDirectory);
		newContents = newContents.replace("#OPAL_TOOLS_USER_ENV_SCRIPT#", this.setProjectEnvironmentScript);

		newContents = newContents.replace("#OPAL_TOOLS_HOME_DIR#", this.swDirectory);
		newContents = newContents.replace("#OPAL_TOOLS_SRC_SQL_DIR#", this.dbSourceDirectory);
		newContents = newContents.replace("#OPAL_TOOLS_PATCH_TEMPLATE_DIR#", templateDirectory);
		newContents = newContents.replace("#OPAL_TOOLS_PATCH_DIR#", patchDirectory);

		if (schema != null)
			newContents = newContents.replace("#SCHEMA#", schema);
		if (env != null)
			newContents = newContents.replace("#ENV#", env);
		

		// only relevant if environment was passed
		if (env != null) {
			String colorCommand = "";
			String color = getColor(env);

			if (color != null) {
				if (osIsWindows()) {
					Map<String, String> colorMap = new HashMap<String, String>() {
						{
							put("green", "0A");
							put("yellow", "0E");
							put("red", "0C");
						}
					};

					if (colorMap.containsKey(color)) {
						colorCommand = "color " + colorMap.get(color);
					} else {
						colorCommand = "color " + color;
					}

				} else {
					Map<String, String> colorMap = new HashMap<String, String>() {
						{
							put("green", "2");
							put("yellow", "3");
							put("red", "1");
						}
					};

					if (colorMap.containsKey(color)) {
						colorCommand = "tput setaf " + colorMap.get(color);
					} else {
						colorCommand = "tput setaf " + color;
					}
				}

				/*
				 * # Pick shell tput setaf using command "tput setaf color" # e.g. green
				 * foreground: "tput setaf 2" # # green : "tput setaf 2" # yellow:
				 * "tput setaf 3" # red : "tput setaf 1"
				 * 
				 * @REM Pick shell color using command "color <background><foreground>"
				 * 
				 * @REM e.g. black background with green foreground: "color 0A"
				 * 
				 * @REM call help for details "color ?"
				 * 
				 * @REM
				 * 
				 * @REM green : "color 0A"
				 * 
				 * @REM yellow: "color 0E"
				 * 
				 * @REM red : "color 0C"
				 * 
				 */
			} else {
				// no color wanted
				colorCommand = "";
			}

			newContents = newContents.replace("#OPAL_TOOLS_SET_COLOR_COMMAND#", colorCommand);

		}

		return newContents;
	}

	private String replaceAllVariables(String contents) {
		return replaceAllVariables(contents, null, null);
	}

	public void run() throws IOException, InterruptedException {

		log.info("running configure");

		Scanner kbd = new Scanner(System.in); // Create a Scanner object

		projectRootDir = promptForInput(kbd, "\nProject root directory, typically the target of a GIT or SVN export",
				projectRootDir);
		swDirectory = promptForInput(kbd,
				"SW install directory (contains bin and lib directories, use '.' for local files)",
				getOsDependentProjectRootVariable() + File.separatorChar + "opal-tools");
		templateDirectory = promptForInput(kbd, "Patch template directory",
				getOsDependentProjectRootVariable() + File.separatorChar + "patch-template");
		localConfigDirectory = promptForInput(kbd,
				"Local configuration directory (connection pools, user dependent config)",
				projectRootDir + File.separatorChar + "conf-user");
		setProjectEnvironmentScript = promptForInput(kbd,
				"Local script to initialize the user environment for this project",
				localConfigDirectory + File.separatorChar + "setProjectEnvironment." + getOsDependentScriptSuffix());
		dbSourceDirectory = promptForInput(kbd,
				"Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.)",
				getOsDependentProjectRootVariable() + File.separatorChar + "sql");
		patchDirectory = promptForInput(kbd, "Patch directory (patches, has subdirectories e.g. year/patch_name)",
				getOsDependentProjectRootVariable() + File.separatorChar + "patches");
		schemaListString = promptForInput(kbd, "List of database schemas (comma-separated, e.g. hr,scott)", "hr,scott");
		environmentListString = promptForInput(kbd, "List of environments (comma-separated, e.g. dev,test,prod)",
				"dev,test,prod");
		environmentColorListString = promptForInput(kbd,
				"List of shell colors for the environments (comma-separated, e.g. green,yellow,red)",
				"green,yellow,red");
		environmentExportConnection = promptForInput(kbd, "which is your developement environment? This is used for the export: ", environmentListString.split(",")[0]);

		log.debug("***");
		log.debug("Project root directory, typically the target of a GIT or SVN export: " + projectRootDir);
		log.debug("SW install directory (contains bin and lib directories): " + swDirectory);
		log.debug("Patch template directory: " + templateDirectory);
		log.debug("Local configuration directory: " + localConfigDirectory);
		log.debug("Local script to initialize the user environment for this project: " + setProjectEnvironmentScript);
		log.debug("Database source directory: " + dbSourceDirectory);
		log.debug("Patch directory: " + patchDirectory);
		log.debug("List of database schemas: " + schemaListString);
		log.debug("List of environments: " + environmentListString);
		log.debug("List of shell colors: " + environmentColorListString);
		log.debug("Environment export connection: " + environmentExportConnection);
		log.debug("***");

		Utils.waitForEnter("Please press <enter> to proceed ...");

		// Split comma separated list into arrays
		environmentListArr = environmentListString.split(",");
		schemaListArr = schemaListString.split(",");
		environmentColorListArr = environmentColorListString.split(",");

		// ----------------------------------------------------------
		// software installation
		// ----------------------------------------------------------
		processSoftwareInstallation(kbd);

		// ----------------------------------------------------------
		// conf directory
		// ----------------------------------------------------------
		processConfDirectory(kbd);

		// ----------------------------------------------------------
		// patch template directory
		// ----------------------------------------------------------
		processPatchTemplateDirectory(kbd);

		// ----------------------------------------------------------
		// conf-user directory
		// ----------------------------------------------------------
		processUserConfDir(kbd);

		// ----------------------------------------------------------
		// db source directory
		// ----------------------------------------------------------
		processDBSourceDirectory(kbd);

		// ----------------------------------------------------------
		// patch directory
		// ----------------------------------------------------------
		processPatchDirectory(kbd);

		// ----------------------------------------------------------
		// bin directory
		// ----------------------------------------------------------
		processBinDirectory(kbd);

		// close keyboard input scanner
		kbd.close();
	}

}