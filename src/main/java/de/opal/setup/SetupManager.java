package de.opal.setup;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import de.opal.exporter.WellBehavedStringArrayOptionHandler;
import de.opal.installer.config.ConfigConnectionMapping;
import de.opal.installer.config.ConfigConnectionPool;
import de.opal.installer.config.ConfigDataConnectionPool;
import de.opal.installer.config.ConfigEncodingMapping;
import de.opal.installer.config.ConfigManager;
import de.opal.installer.config.ConfigManagerConnectionPool;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;
import de.opal.utils.FileIO;
import de.opal.utils.OSDetector;

public class SetupManager {

	public static final Logger log = LogManager.getLogger(SetupManager.class.getName());

	private String schemaListString;
	private String environmentListString;
	private String environmentColorListString;
	private List<String> connFilenameList = new ArrayList<String>();

	// target OS
	public static final String osWindows = "windows";
	public static final String osLinux = "linux";

	// setup Mode
	public static final String setupModeInstall = "install";
//	public static final String setupModeUpgrade = "upgrade";
	public static final String setupModeScripts = "scripts";

	// File encoding in Java, UTF8, Cp1252, etc.
	// See column "Canonical Name for java.io and java.lang API" here:
	// https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
	// both UTF8 and UTF-8 work, but UTF-8 seems to be the default - confusing,
	// doesn't match the documentation
	// it is different on Windows and Mac ... the value is not in the same column!
	private String utf8_default = StandardCharsets.UTF_8.toString();

	String localDir = System.getProperty("user.dir");
	String tmpSourceDir = "";
	String tmpTargetDir = "";
	String[] osScriptSuffixList = new String[] { getOsDependentScriptSuffix(), "sql" };
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

	@Option(name = "-s", aliases = "--show-passwords", usage = "when prompted for passwords, they will be shown in clear text")
	private boolean showPasswords;

	@Option(name = "-h", aliases = "--help", usage = "display this help page")
	private boolean showHelp = false;

	// setup-mode
	@Option(name = "--setup-mode", usage = "Setup mode ", metaVar = "[install|scripts]", required = false)
	private String setupMode;

	@Option(name = "--project-root-dir", usage = "Sets the root directory for the installation. Will be used to derive other parameters if not set explicitly. This directory is typically the target of a GIT or SVN export.", metaVar = "<directory>", required = false)
	private String projectRootDir;

	@Option(name = "--software-dir", usage = "SW install directory (contains bin and lib directories)\ne.g. ${PROJECT_ROOT}/opal-tools or %PROJECT_ROOT%\\opal-tools ", metaVar = "<directory>")
	private String swDirectory;

	@Option(name = "--template-dir", usage = "Patch template directory\ne.g. ${PROJECT_ROOT}/patch-template or %PROJECT_ROOT%\\patch-template", metaVar = "<directory>")
	private String templateDirectory;

	@Option(name = "--local-config-dir", usage = "Local configuration directory (connection pools, user dependent config), typically OUTSIDE of the git tree\ne.g. /local/conf-user or c:\\local\\conf-user", metaVar = "<directory>")
	private String localConfigDirectory;

	@Option(name = "--environment-script", usage = "Local script to initialize the user environment for this project\ne.g. /local/conf-user/setProjectEnvironment.sh or c:\\local\\conf-user\\setProjectEnvironment.cmd", metaVar = "<directory>")
	private String setProjectEnvironmentScript;

	@Option(name = "--source-dir", usage = "Source directory, will have subdirectories sql, apex, rest", metaVar = "<directory>")
	private String sourceDirectory;

	@Option(name = "--db-source-dir", usage = "Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.)\ne.g. ${PROJECT_ROOT}/src/sql or %PROJECT_ROOT%\\src\\sql", metaVar = "<directory>")
	private String dbSourceDirectory;

	@Option(name = "--patch-dir", usage = "Patch directory (patches, has subdirectories e.g. year/patch_name)\ne.g. ${PROJECT_ROOT}/patches or %PROJECT_ROOT%\\patches", metaVar = "<directory>")
	private String patchDirectory;

	@Option(name = "--schemas", handler = WellBehavedStringArrayOptionHandler.class, usage = "List of database schemas (blank-separated, e.g. schema1 schema2)\ne.g. schema1 schema2", metaVar = "<schema1> [<schema2>] [<schema3>] ...")
	private List<String> schemaListArr = new ArrayList<String>();

	@Option(name = "--environments", handler = WellBehavedStringArrayOptionHandler.class, usage = "List of environments (blank-separated, e.g. dev test prod)\ne.g. dev test prod", metaVar = "<env1> [<env2>] [<env3>]...")
	private List<String> environmentListArr = new ArrayList<String>();

	@Option(name = "--environment-colors", handler = WellBehavedStringArrayOptionHandler.class, usage = "List of shell colors for the environments (blank-separated, e.g. green yellow red)\ne.g. green yellow red: ", metaVar = "<color1> [<color2>] [<color3>] ...")
	private List<String> environmentColorListArr = new ArrayList<String>();

	@Option(name = "--export-environment", usage = "Which is your designated developement environment? This is used for the export.\ne.g. dev", metaVar = "<environment>")
	private String environmentExportConnection;

	@Option(name = "--file-encoding", usage = "file encoding (e.g. UTF-8 or Cp1252, default is current system encoding)\ne.g. UTF-8", metaVar = "<file encoding>")
	private String fileEncoding;

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

			// join schemas into string
			if (this.schemaListArr.size() > 0)
				this.schemaListString = String.join(" ", this.schemaListArr);
			if (this.environmentListArr.size() > 0)
				this.environmentListString = String.join(" ", this.environmentListArr);
			if (this.environmentColorListArr.size() > 0)
				this.environmentColorListString = String.join(" ", this.environmentColorListArr);

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

			// targetOS - default and validation
			// set current operating system as the default
			// if (this.targetOS != null && !this.targetOS.equals(osLinux) &&
			// !this.targetOS.equals(osWindows))
			// throw new CmdLineException("Invalid option " + this.targetOS + " for
			// --target-os");

			// setupMode
			if (this.setupMode != null && !this.setupMode.equals(setupModeInstall)
					&&  !this.setupMode.equals(setupModeScripts))
				throw new CmdLineException("Invalid option " + this.setupMode + " for --setup-mode");

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
		input = kbd.nextLine().trim();
		if (input.isEmpty()) {
			input = defaultValue;
		}

		// kbd.close();
		return input;
	}

	// get the current os in a normalized fashion,
	// ONLY linux or windows
	// , not mac os or other
//	private String getCurrentOsNormalized() {
//		String os="";
//		
//		// check against current os and convert to standard naming
//		if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
//			os=osWindows;
//		else
//			os=osLinux;
//			
//		return os; 
//	}

	// is the current operating system a windows environment?
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

		Msg.println("\nprocess local conf directory in: " + tmpTargetDir);

		// loop over all environments, create a new file for each one
		// create CONNECTION POOL files
		for (String env : environmentListArr) {

			// create connection pool file
			String confFilename = tmpTargetDir + File.separator + "connections-" + env + ".json";
			connFilenameList.add("connections-" + env + ".json");
			Msg.println("\n  Process environment: " + env + " => " + confFilename);

			// first we create an empty file
			FileUtils.writeStringToFile(new File(confFilename), "{}", Charset.defaultCharset());

			// prompt for url for all connections in this file
			String envJDBCUrl = promptForInput(kbd,
					"    JDBC url for environment " + env + " (hostname:port:sid or hostname:port/servicename): ",
					"127.0.0.1:1521:xe");

			ConfigDataConnectionPool configData = new ConfigDataConnectionPool();
			configData.targetSystem = env;

			// loop over all schemas for the current environment
			for (String schema : schemaListArr) {
				String password = "";
				String user = "";

				Msg.println("");

				Console con = System.console();
				// hide password on real console
				// input password in Eclipse
				user = promptForInput(kbd, "    User to connect to schema " + schema + " in environment " + env + ": ",
						schema);
				if (con == null || showPasswords) {
					// input password in Eclipse
					password = promptForInput(kbd, "    Password : ", "");
				} else {
					System.out.println("    Password : ");
					char[] ch = con.readPassword();
					password = String.valueOf(ch);
				}
				ConfigConnectionPool conn = new ConfigConnectionPool(schema, user, password, envJDBCUrl);
				// add connection to configFile
				configData.connectionPools.add(conn);
			}

			ConfigManagerConnectionPool confMgr = new ConfigManagerConnectionPool(confFilename);
			confMgr.setConfigData(configData);
			confMgr.encryptPasswords(confMgr.getEncryptionKeyFilename(confFilename));

			confMgr.writeJSONConf();
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

		// first delete lib directory

		Msg.println("\n----------------------------------------------------------\n");
		Msg.println("copy sw files from: " + tmpSourceDir + "\n              to  : " + tmpTargetDir + "\n");
		try {
			File tmpTargetDirFile = new File(tmpTargetDir);

			if (tmpTargetDirFile.exists()) {
				Msg.println("First delete all jar files existing target lib directory");
				FileIO.deleteFiles(tmpTargetDirFile, new String[] {".*\\.jar"});
			}

			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		} catch (IOException e) {
			Msg.println("Files will NOT be copied because an error occured: " + e.getMessage() + "\n");
		}

		// export-scripts
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "export-scripts");
		tmpTargetDir = getFullPathResolveVariables(swDirectory + File.separatorChar + "export-scripts");

		Msg.println("\n----------------------------------------------------------\n");
		Msg.println("copy sw files from: " + tmpSourceDir + "\n              to  : " + tmpTargetDir + "\n");
		try {
			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		} catch (IOException e) {
			Msg.println("Files will NOT be copied because an error occured: " + e.getMessage() + "\n");
		}

		// export-templates
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "export-templates");
		tmpTargetDir = getFullPathResolveVariables(swDirectory + File.separatorChar + "export-templates");

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
				FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + "SourceFilesCopy.conf"),
						patchFileHeader + "\n" + patchFileContent, Charset.defaultCharset());
				FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + "SourceFilesReference.conf"),
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
		String fileContents = FileUtils.readFileToString(
				new File(tmpSourceDir + File.separator + "opal-installer.json"), Charset.defaultCharset());

		FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + "opal-installer.json"), fileContents,
				Charset.defaultCharset());
		ConfigManager confMgrInst = new ConfigManager(tmpTargetDir + File.separator + "opal-installer.json");

		// loop over all schemas for the current environment
		for (String schema : schemaListArr) {
			ConfigConnectionMapping map = null;

			map = new ConfigConnectionMapping(schema, null, "/sql/*" + schema + "*", null);

//			if (osIsWindows()) {
//				map = new ConfigConnectionMapping(schema, "\\\\sql\\\\.*" + schema + ".*", null, null);
//			} else {
//				map = new ConfigConnectionMapping(schema, "/sql/.*" + schema + ".*", null, null);
//			}

			// add connection to configFile
			confMgrInst.getConfigData().connectionMappings.add(map);
		}
		// add encoding mapping
		ConfigEncodingMapping map = null;

		map = new ConfigEncodingMapping(utf8_default, null, "/sql/*apex*/*f*sql",
				"encoding for APEX files is always UTF8");
		confMgrInst.getConfigData().encodingMappings.add(map);
		map = new ConfigEncodingMapping(this.fileEncoding, null, "/sql/*",
				"all other files will get this explicit mapping");
		confMgrInst.getConfigData().encodingMappings.add(map);

//		if (osIsWindows()) {
//			map = new ConfigEncodingMapping(utf8_default, "\\\\sql\\\\.*apex.*\\\\.*f.*sql",
//					"encoding for APEX files is always UTF8");
//			confMgrInst.getConfigData().encodingMappings.add(map);
//			map = new ConfigEncodingMapping(this.fileEncoding, "\\\\sql\\\\.*",
//					"all other files will get this explicit mapping");
//			confMgrInst.getConfigData().encodingMappings.add(map);
//		} else {
//			map = new ConfigEncodingMapping(utf8_default, "/sql/.*apex.*/.*f.*sql",
//					"encoding for APEX files is always UTF8");
//			confMgrInst.getConfigData().encodingMappings.add(map);
//			map = new ConfigEncodingMapping(this.fileEncoding, "/sql/.*",
//					"all other files will get this explicit mapping");
//			confMgrInst.getConfigData().encodingMappings.add(map);
//		}
		// write opal-installer.json file
		confMgrInst.writeJSONConfInitFile();
	}

	private void processDBSourceDirectory(Scanner kbd) throws IOException {
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "src-sql");

		Msg.println(
				"Source directory from: " + tmpSourceDir + "\n                    to  : " + dbSourceDirectory + "\n");

		// loop over all schemas
		for (String schema : schemaListArr) {
			tmpTargetDir = getFullPathResolveVariables(dbSourceDirectory + File.separator + schema);

			FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
		}
	}

	private void processSourceDirectory(Scanner kbd) throws IOException {
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + "src");

		Msg.println("Source directory from: " + tmpSourceDir + "\n                    to  : " + sourceDirectory + "\n");
		tmpTargetDir = getFullPathResolveVariables(sourceDirectory);
		FileUtils.copyDirectory(new File(tmpSourceDir), new File(tmpTargetDir));
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
		processBinDirectoryGeneric(kbd, "bin", "bin");
		processBinDirectoryGeneric(kbd, "bin"+ File.separatorChar + "internal", "bin"+ File.separatorChar +"internal");
	}

	private void processBinDirectoryGeneric(Scanner kbd, String sourceDirectory, String targetDirectory)
			throws IOException {
		tmpSourceDir = getFullPathResolveVariables(
				localDir + File.separatorChar + "configure-templates" + File.separatorChar + sourceDirectory);
		tmpTargetDir = getFullPathResolveVariables(swDirectory + File.separator + targetDirectory);

		Msg.println("process bin directory\n");

		// copy / replace all files in the bin directory
		Iterator<File> it1 = FileUtils.iterateFiles(new File(tmpSourceDir), osScriptSuffixList, false);
		while (it1.hasNext()) {
			File f = (File) it1.next();
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();
			Msg.println("  process file " + filename);
			String templateContents = FileUtils.readFileToString(f, Charset.defaultCharset());

			if (filename.contains("#ENV#")) {
				// iterate over all environments
				for (String env : environmentListArr) {
					String newFilename = filename.replace("#ENV#", env);
					String contents = replaceAllVariables(templateContents, env, null);
					FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + newFilename), contents,
							Charset.defaultCharset());
				}
			} else if (filename.contains("#SCHEMA#")) {
				// iterate over all schemas
				for (String schema : schemaListArr) {
					String newFilename = filename.replace("#SCHEMA#", schema);
					String contents = replaceAllVariables(templateContents, this.environmentExportConnection, schema);
					FileUtils.writeStringToFile(new File(tmpTargetDir + File.separator + newFilename), contents,
							Charset.defaultCharset());
				}

			} else {
				// write the single file back and replace placeholders
				String contents = replaceAllVariables(templateContents);
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
			builder.command("bash", "-c",
					"find \"" + getFullPathResolveVariables(localConfigDirectory) + "\" \""
							+ getFullPathResolveVariables(swDirectory) + "\" \""
							+ getFullPathResolveVariables(templateDirectory)
							+ "\" -type f -iname \"*.sh\" -exec chmod +x {} \\;");
			try {
				Process process = builder.start();

				BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line);
				}

				@SuppressWarnings("unused")
				int exitCode = process.waitFor();

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
		for (int i = 0; i < this.environmentListArr.size(); i++) {
			if (environmentListArr.get(i).contentEquals(env)) {
				if (environmentColorListArr.size() > i) {
					color = environmentColorListArr.get(i);
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
		newContents = newContents.replace("#OPAL_TOOLS_SRC_DIR#", this.sourceDirectory);
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
					@SuppressWarnings("serial")
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
					@SuppressWarnings("serial")
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

			} else {
				// no color wanted
				colorCommand = "";
			}

			newContents = newContents.replace("#OPAL_TOOLS_SET_COLOR_COMMAND#", colorCommand);

		}

		newContents = newContents.replace("#FILE.ENCODING#", this.fileEncoding);
		newContents = newContents.replace("#OPAL_TOOLS_USER_IDENTITY#", System.getProperty("user.name"));

		String connFileListString = "";
		for (String filename : connFilenameList) {
			if (osIsWindows())
				connFileListString += "\"" + "%OPAL_TOOLS_USER_CONFIG_DIR%" + File.separator + filename + "\" ";
			else
				connFileListString += "\"" + "${OPAL_TOOLS_USER_CONFIG_DIR}" + File.separator + filename + "\" ";
		}
		newContents = newContents.replace("#OPAL_CONNECTION_FILE_LIST#", connFileListString);

		return newContents;
	}

	private String replaceAllVariables(String contents) {
		return replaceAllVariables(contents, null, null);
	}

	public void run() throws IOException, InterruptedException {

		log.info("running configure");

		Scanner kbd = new Scanner(System.in); // Create a Scanner object

		// setupMode
		if (this.setupMode == null)
			this.setupMode = promptForInput(kbd, "\nSetup mode (install=full install, scripts=Only scripts for multi-OS installation): ", setupModeInstall);
		else
			Msg.print("\nSetup mode: " + this.setupMode);

		if (projectRootDir == null)
			projectRootDir = promptForInput(kbd,
					"\nProject root directory, typically the target of a GIT or SVN export", "");
		else
			Msg.println("\nProject root directory, typically the target of a GIT or SVN export: " + projectRootDir);

		if (swDirectory == null)
			swDirectory = getOsDependentProjectRootVariable() + File.separatorChar + "opal-tools";
//			swDirectory = promptForInput(kbd, "SW install directory (contains bin and lib directories)",
//					getOsDependentProjectRootVariable() + File.separatorChar + "opal-tools");
		else
			Msg.println("SW install directory (contains bin and lib directories): " + swDirectory);

		if (templateDirectory == null)
			templateDirectory = getOsDependentProjectRootVariable() + File.separatorChar + "patch-template";
//			templateDirectory = promptForInput(kbd, "Patch template directory",
//					getOsDependentProjectRootVariable() + File.separatorChar + "patch-template");
		else
			Msg.println("Patch template directory: " + templateDirectory);

		if (localConfigDirectory == null)
			localConfigDirectory = promptForInput(kbd,
					"Local configuration directory (connection pools, user dependent config)",
					projectRootDir + File.separatorChar + "conf-user");
		else
			Msg.println(
					"Local configuration directory (connection pools, user dependent config): " + localConfigDirectory);

		if (setProjectEnvironmentScript == null)
			setProjectEnvironmentScript = promptForInput(kbd,
					"Local script to initialize the user environment for this project", localConfigDirectory
							+ File.separatorChar + "setProjectEnvironment." + getOsDependentScriptSuffix());
		else
			Msg.println(
					"Local script to initialize the user environment for this project: " + setProjectEnvironmentScript);

		if (sourceDirectory == null)
			sourceDirectory = getOsDependentProjectRootVariable() + File.separatorChar + "src";
//			sourceDirectory = promptForInput(kbd,
//					"Source directory",	getOsDependentProjectRootVariable() + File.separatorChar + "src");
		else
			Msg.println("Database source directory: " + dbSourceDirectory);

		if (dbSourceDirectory == null)
			dbSourceDirectory = getOsDependentProjectRootVariable() + File.separatorChar + "src" + File.separatorChar
					+ "sql";
//			dbSourceDirectory = promptForInput(kbd,
//					"Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.)",
//					getOsDependentProjectRootVariable() + File.separatorChar + "sql");
		else
			Msg.println("Database source directory: " + dbSourceDirectory);

		if (patchDirectory == null)
			patchDirectory = getOsDependentProjectRootVariable() + File.separatorChar + "patches";
//			patchDirectory = promptForInput(kbd, "Patch directory (patches, has subdirectories e.g. year/patch_name)",
//					getOsDependentProjectRootVariable() + File.separatorChar + "patches");
		else
			Msg.println("Patch directory: " + patchDirectory);

		if (schemaListString == null)
			schemaListString = promptForInput(kbd, "List of database schemas (blank-separated, e.g. schema1 schema2)",
					"schema1 schema2");
		else
			Msg.println("List of database schemas: " + schemaListString);

		if (environmentListString == null)
			environmentListString = promptForInput(kbd, "List of environments (blank-separated, e.g. dev test prod)",
					"dev test prod");
		else
			Msg.println("List of environments: " + environmentListString);

		if (environmentColorListString == null)
			environmentColorListString = promptForInput(kbd,
					"List of shell colors for the environments (blank-separated, e.g. green yellow red)",
					"green yellow red");
		else
			Msg.println("List of shell colors for the environments: " + environmentColorListString);

		if (environmentExportConnection == null)
			environmentExportConnection = promptForInput(kbd,
					"Which is your developement environment? This is used for the export: ",
					environmentListString.split(" ")[0].trim());
		else
			Msg.println("Which is your developement environment: " + environmentExportConnection);

		if (fileEncoding == null)
			fileEncoding = promptForInput(kbd,
					"file encoding (e.g. UTF-8 or Cp1252, default is current system encoding): ",
					System.getProperty("file.encoding"));
		else
			Msg.println("file encoding: " + fileEncoding);

		log.debug("***");
		log.debug("Project root directory, typically the target of a GIT or SVN export: " + projectRootDir);
		log.debug("SW install directory (contains bin and lib directories): " + swDirectory);
		log.debug("Patch template directory: " + templateDirectory);
		log.debug("Local configuration directory: " + localConfigDirectory);
		log.debug("Local script to initialize the user environment for this project: " + setProjectEnvironmentScript);
		log.debug("Source directory: " + sourceDirectory);
		log.debug("Database source directory: " + dbSourceDirectory);
		log.debug("Patch directory: " + patchDirectory);
		log.debug("List of database schemas: " + schemaListString);
		log.debug("List of environments: " + environmentListString);
		log.debug("List of shell colors: " + environmentColorListString);
		log.debug("Environment export connection: " + environmentExportConnection);
		log.debug("File encoding: " + fileEncoding);
		log.debug("***");

		Msg.println("\n\n*** Installation Summary:\n");
		Msg.println("Project root directory, typically the target of a GIT or SVN export: " + projectRootDir);
		Msg.println("SW install directory (contains bin and lib directories): " + swDirectory);
		Msg.println("Patch template directory: " + templateDirectory);
		Msg.println("Local configuration directory: " + localConfigDirectory);
		Msg.println("Local script to initialize the user environment for this project: " + setProjectEnvironmentScript);
		Msg.println("Source directory: " + sourceDirectory);
		Msg.println("Database source directory: " + dbSourceDirectory);
		Msg.println("Patch directory: " + patchDirectory);
		Msg.println("List of database schemas: " + schemaListString);
		Msg.println("List of environments: " + environmentListString);
		Msg.println("List of shell colors: " + environmentColorListString);
		Msg.println("Environment export connection: " + environmentExportConnection);
		Msg.println("File encoding: " + fileEncoding);
		Msg.println("\n***\n");

		Utils.waitForEnter("Please press <enter> to proceed ...");

		environmentListArr = Arrays.asList(environmentListString.split(" "));
		for (int i = 0; i < environmentListArr.size(); i++) {
			environmentListArr.set(i, environmentListArr.get(i).trim());
		}
		schemaListArr = Arrays.asList(schemaListString.split(" "));
		for (int i = 0; i < schemaListArr.size(); i++) {
			schemaListArr.set(i, schemaListArr.get(i).trim());
		}
		environmentColorListArr = Arrays.asList(environmentColorListString.split(" "));
		for (int i = 0; i < environmentColorListArr.size(); i++) {
			environmentColorListArr.set(i, environmentColorListArr.get(i).trim());
		}

		// ----------------------------------------------------------
		// conf-user directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall) || this.setupMode.equals(setupModeScripts))
			processUserConfDir(kbd);

		// ----------------------------------------------------------
		// software installation
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall) )
			processSoftwareInstallation(kbd);

		// ----------------------------------------------------------
		// conf directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall) )
			processConfDirectory(kbd);

		// ----------------------------------------------------------
		// patch template directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall) )
			processPatchTemplateDirectory(kbd);

		// ----------------------------------------------------------
		// source directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall))
			processSourceDirectory(kbd);

		// ----------------------------------------------------------
		// db source directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall))
			processDBSourceDirectory(kbd);

		// ----------------------------------------------------------
		// patch directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall))
			processPatchDirectory(kbd);

		// ----------------------------------------------------------
		// bin directory
		// ----------------------------------------------------------
		if (this.setupMode.equals(setupModeInstall) || this.setupMode.equals(setupModeScripts))
			processBinDirectory(kbd);

		// close keyboard input scanner
		kbd.close();

		// ----------------------------------------------------------
		// open folder $PROJECT_ROOT
		// ----------------------------------------------------------
		Msg.println("\nOpen PROJECT_ROOT: " + projectRootDir + "...\n");
		OSDetector.open(new File(projectRootDir));
	}

}