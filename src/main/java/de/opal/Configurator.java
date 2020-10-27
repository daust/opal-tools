package de.opal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.config.ConfigConnectionMapping;
import de.opal.installer.config.ConfigConnectionPool;
import de.opal.installer.config.ConfigData;
import de.opal.installer.config.ConfigEncodingMapping;
import de.opal.installer.config.ConfigManager;
import de.opal.installer.util.Msg;
import de.opal.installer.util.Utils;

public class Configurator {

	public static final Logger log = LogManager.getLogger(Configurator.class.getName());
	private String baseDir = ".";

	public static void main(String[] args) {

	}

	/**
	 * Constructor
	 * 
	 * @param args - initialize with command line parameters
	 */
	public Configurator(String[] args) {
		readConfig(args);
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
		return System.getProperty("os.name").toLowerCase().indexOf("win") >=0;
	}

	private String getOsScriptSuffix() {
		if (osIsWindows()) {
			return "cmd";
		} else {
			return "sh";
		}
			
	}
	
	public void run() throws IOException, InterruptedException {
		String localDir = System.getProperty("user.dir");
		String[] osScriptSuffixList=new String[]{getOsScriptSuffix()};
		FileFilter osFileFilter=new FileFilter() 
		{
		      //Override accept method
		      public boolean accept(File file) {
		              
		             //if the file extension is .log return true, else false
		             if (file.getName().endsWith("."+getOsScriptSuffix())
		            		 ||
		            		 file.getName().endsWith(".txt") ) {
		                return true;
		             }
		             return false;
		      }
		};
				
		// String swDirectory="";
		// String swDirectoryDefault=""; //baseDir + File.pathSeparator + "";
		// String templateDirectory="";
		// String templateDirectoryDefault=baseDir + File.separatorChar +
		// "patch-template";
		// String localConfigDirectory="";

		log.info("running configure");

		Scanner kbd = new Scanner(System.in); // Create a Scanner object
		String swDirectory = promptForInput(kbd, "\nSW install directory (use '.' for local files)", baseDir);
		String templateDirectory = promptForInput(kbd, "Patch template directory",
				baseDir + File.separatorChar + "patch-template");
		String localConfigDirectory = promptForInput(kbd,
				"Local configuration directory (connection pools, user dependent config)",
				baseDir + File.separatorChar + "conf-user");
		String dbSourceDirectory = promptForInput(kbd,
				"Database source directory (sql, has subdirectories e.g. sql/oracle_schema/tables, sql/oracle_schema/packages, etc.)",
				baseDir + File.separatorChar + "sql");
		String patchDirectory = promptForInput(kbd,
				"Patch directory (patches, has subdirectories e.g. year/patch_name)",
				baseDir + File.separatorChar + "patches");
		String schemaListString = promptForInput(kbd, "List of database schemas (comma-separated, e.g. HR,SCOTT)",
				"HR,SCOTT");
		String environmentListString = promptForInput(kbd, "List of environments (comma-separated, e.g. DEV,INT,PROD)",
				"DEV,INT,PROD");
		//String environmentColorListString = promptForInput(kbd, "List of shell colors for environments (comma-separated, e.g. green,yellow,red)",
		//		"green,yellow,red");		

		log.debug("***");
		log.debug("SW install directory: " + swDirectory);
		log.debug("Patch template directory: " + templateDirectory);
		log.debug("Local configuration directory: " + localConfigDirectory);
		log.debug("Database source directory: " + dbSourceDirectory);
		log.debug("Patch directory: " + patchDirectory);
		log.debug("List of database schemas: " + schemaListString);
		log.debug("List of environments: " + environmentListString);
		log.debug("***");

		// ----------------------------------------------------------
		// transform directories from relative paths to absolute paths
		// ----------------------------------------------------------
		swDirectory = new File(swDirectory).getCanonicalPath();
		templateDirectory = new File(templateDirectory).getCanonicalPath();
		localConfigDirectory = new File(localConfigDirectory).getCanonicalPath();
		dbSourceDirectory = new File(dbSourceDirectory).getCanonicalPath();
		patchDirectory = new File(patchDirectory).getCanonicalPath();
		
		
		log.debug("\nLocal configuration directory (after transformation to absolute path): " + localConfigDirectory);
		log.debug("***");
		log.debug("SW install directory: " + swDirectory);
		log.debug("Patch template directory: " + templateDirectory);
		log.debug("Local configuration directory: " + localConfigDirectory);
		log.debug("Database source directory: " + dbSourceDirectory);
		log.debug("Patch directory: " + patchDirectory);
		log.debug("***");
		
		
		Utils.waitForEnter("Please press <enter> to proceed ...");

		// ----------------------------------------------------------
		// software installation
		// ----------------------------------------------------------
		Msg.println("\n----------------------------------------------------------\n");
		Msg.println("copy sw files from: " + localDir + File.separatorChar + "lib" + "\n              to  : "
				+ swDirectory + File.separatorChar + "lib" + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		//Boolean isIdentical=new File(localDir).getAbsoluteFile().compareTo(new File(swDirectory).getAbsoluteFile())==0;
		/*
		Boolean isIdentical=Paths.get(localDir).toRealPath().compareTo(Paths.get(swDirectory).toRealPath())==0;

		// DON't copy the ./lib directory onto itself, when the src and target path are identical
		// all other parts can simply be overwritten.
		if (!isIdentical) {
			FileUtils.copyDirectory(new File(localDir + File.separatorChar + "lib"),
					new File(swDirectory + File.separatorChar + "lib"));			
		}
		*/
		try {
			FileUtils.copyDirectory(new File(localDir + File.separatorChar + "lib"),
				new File(swDirectory + File.separatorChar + "lib"));
		} catch (IOException e){
			Msg.println("Files will NOT be copied because an error occured: " + e.getMessage() + "\n");
		}

		// ----------------------------------------------------------
		// conf directory
		// ----------------------------------------------------------
		Msg.println("\n----------------------------------------------------------\n");
		Msg.println("copy sw files from :" + localDir + File.separatorChar + "conf" + "\n              to  : "
				+ swDirectory + File.separatorChar + "conf" + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		try {
			FileUtils.copyDirectory(new File(localDir + File.separatorChar + "conf"),
				new File(swDirectory + File.separatorChar + "conf"));
		} catch (IOException e){
			Msg.println("Files will NOT be copied because an error occured: " + e.getMessage() + "\n");
		}

		// ----------------------------------------------------------
		// patch template directory
		// ----------------------------------------------------------
		Msg.println("copy template directory from: " + localDir + File.separatorChar + "configure-templates"
				+ File.separatorChar + "patch-template" + "\n                        to  : " + templateDirectory + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		// FileUtils.copyDirectory(new
		// File(localDir+File.separatorChar+"configure-templates"+File.separatorChar+"patch-template"),
		// new File(templateDirectory));
		FileUtils.forceMkdir(new File(templateDirectory));

		// loop over all schemas to create sql subdirectories
		for (String schema : schemaListString.split(",")) {
			FileUtils.copyDirectory(
					new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar
							+ "patch-template-sql"),
					new File(templateDirectory + File.separator + "sql" + File.separator + schema));
		}

		// create new patch-install files for each environment
		// copy / replace all files but the #VAR#... files
		String patchFileHeader="";
		String patchFileContent="";
		
		Iterator<File> it = FileUtils.iterateFiles(
				new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template"),
				null, false);
		int i = 2; // start counter for #NO# files with 3
		while (it.hasNext()) {
			File f = (File) it.next();
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();

			// filter for operating system
			// on *nix process .sh files, on Windows process *.cmd files
			if (filename.endsWith(getOsScriptSuffix())
					|| (!filename.endsWith(".cmd")&&!filename.endsWith(".sh"))) {

				Msg.println("  process file " + filename);

				String contents = FileUtils.readFileToString(f, Charset.defaultCharset());
				contents = contents.replace("#OPAL_INSTALLER_USER_CONFIG_DIR#", localConfigDirectory);
				contents = contents.replace("#OPAL_INSTALLER_HOME_DIR#", swDirectory);
				contents = contents.replace("#OPAL_SQL_SOURCE_DIR", dbSourceDirectory);
				
				if (filename.contains("#")) {
					// do nothing ... will be processed later
				} else if (filename.startsWith("PatchFiles-header.txt")){
					// add the header to the beginning of the content
					patchFileHeader = FileUtils.readFileToString(new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template" + File.separator + path.getFileName()), Charset.defaultCharset());
				} else if (filename.startsWith("PatchFiles-body.txt")){
					String templateMapping = FileUtils.readFileToString(new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template" + File.separator + path.getFileName()), Charset.defaultCharset());
					// loop over all schemas and create a mapping for each one
					for (String schema : schemaListString.split(",")) {
						patchFileContent+=templateMapping.replace("#SCHEMA#",schema);
					}

				} else {
					// nothing special here
					FileUtils.writeStringToFile(new File(templateDirectory + File.separator + path.getFileName()), contents,
							Charset.defaultCharset());
				}
				// write the patchFile.txt 
				FileUtils.writeStringToFile(new File(templateDirectory + File.separator + "PatchFiles.txt"), patchFileHeader + "\n" + patchFileContent,
						Charset.defaultCharset());
			}			
		}
		// process validation files and installation files "#var#..."
		// special handling for each environment
		i=2;
		for (String env : environmentListString.split(",")) {
			File f = new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template"+ File.separatorChar + "3.install-patch-#ENV#."+getOsScriptSuffix());
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();
			String contents = FileUtils.readFileToString(f, Charset.defaultCharset());

			String newFilename = filename.replace("#NO#", "" + i++).replace("#ENV#", env);
			contents = contents.replace("#ENV#", env);
			contents = contents.replace("#OPAL_INSTALLER_HOME_DIR#", swDirectory);
			FileUtils.writeStringToFile(new File(templateDirectory + File.separator + newFilename), contents,
					Charset.defaultCharset());
		}
		// process installation files and installation files "#NO#..."
		// special handling for each environment
		i=2;
		for (String env : environmentListString.split(",")) {
			File f = new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "patch-template"+ File.separatorChar + "2.validate-patch-#ENV#."+getOsScriptSuffix());
			Path path = Paths.get(f.getName());
			String filename = path.getFileName().toString();
			String contents = FileUtils.readFileToString(f, Charset.defaultCharset());

			String newFilename = filename.replace("#NO#", "" + i++).replace("#ENV#", env);
			contents = contents.replace("#ENV#", env);
			contents = contents.replace("#OPAL_INSTALLER_HOME_DIR#", swDirectory);
			FileUtils.writeStringToFile(new File(templateDirectory + File.separator + newFilename), contents,
					Charset.defaultCharset());
		}

		// add connection pool mappings to file system paths
		// read opal-installer.json file
		//ConfigData configDataInst = new ConfigData();
		//configDataInst.clearDefaults();
		String fileContents="{\n" + 
				"	\"application\": \"\",\n" + 
				"    \"patch\": \"\",\n" + 
				"    \"author\": \"\",\n" + 
				"    \"version\": \"\",\n" + 
				"    \"connectionMappings\": [],\n" + 
				"	 \"waitAfterEachStatement\": \"true\"," +
				"    \"sqlFileRegEx\": \"\\\\.(sql|pks|pkb|trg)$\",\n" + 
				"    \"registryTargets\": [],\n" + 
				"    \"encodingMappings\": [ ]	\n" + 
				"}";
		FileUtils.writeStringToFile(new File(templateDirectory + File.separator + "opal-installer.json"), fileContents, Charset.defaultCharset());
		ConfigManager confMgrInst = new ConfigManager(templateDirectory + File.separator + "opal-installer.json");
		
		// loop over all schemas for the current environment
		for (String schema : schemaListString.split(",")) {
			ConfigConnectionMapping map=null;
			if (osIsWindows()) {
				map=new ConfigConnectionMapping(schema, "\\\\sql\\\\.*"+schema+".*");
			} else {
				map=new ConfigConnectionMapping(schema, "/sql/.*"+schema+".*");
			}
			
			// add connection to configFile
			confMgrInst.getConfigData().connectionMappings.add(map);
		}
		// add encoding mapping
		ConfigEncodingMapping map=null;
		if (osIsWindows()) {
			map=new ConfigEncodingMapping("UTF8", "\\\\sql\\\\.*apex.*/.*f*sql");
		} else {
			map=new ConfigEncodingMapping("UTF8", "/sql/.*apex.*/.*f*sql");
		}
		confMgrInst.getConfigData().encodingMappings.add(map);
		
		// write opal-installer.json file
		confMgrInst.writeJSONConfInitFile();

		// ----------------------------------------------------------
		// conf-user directory
		// ----------------------------------------------------------
		Msg.println("\nprocess local conf directory in: " + localConfigDirectory + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		FileUtils.copyDirectory(
				new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "conf-user"),
				new File(localConfigDirectory), osFileFilter);

		// loop over all environments, create a new file for each one
		for (String env : environmentListString.split(",")) {

			// create connection pool file
			String confFilename = localConfigDirectory + File.separator + "connections-" + env + ".json";
			Msg.println("  Process environment: " + env + " => " + confFilename);

			// FileUtils.copyFile(new
			// File(localDir+File.separator+"conf-user"+File.separator+"connections-#ENV#.json"),
			// new File(localConfigDirectory+File.separator+"connections-"+env+".json"));
			// first we create an empty file
			FileUtils.writeStringToFile(new File(confFilename), "{}", Charset.defaultCharset());

			// prompt for url for all connections in this file
			String envJDBCUrl = promptForInput(kbd, "    JDBC url for environment " + env + ": ",
					"jdbc:oracle:thin:@127.0.0.1:1521:xe");

			ConfigData configData = new ConfigData();
			configData.clearDefaults();
			configData.targetSystem = env;

			// loop over all schemas for the current environment
			for (String schema : schemaListString.split(",")) {
				String password = promptForInput(kbd,
						"    Password for schema " + schema + " in environment " + env + ": ", "");

				ConfigConnectionPool conn = new ConfigConnectionPool(schema, schema, password, envJDBCUrl);
				// add connection to configFile
				configData.connectionPools.add(conn);
			}

			ConfigManager confMgr = new ConfigManager(confFilename);
			confMgr.setConfigData(configData);

			confMgr.writeJSONConfPool();
		}
		Msg.println("");

		// ----------------------------------------------------------
		// db source directory
		// ----------------------------------------------------------
		Msg.println("db source directory from: " + localDir + File.separatorChar + "configure-templates"
				+ File.separatorChar + "src-sql" + "\n                    to  : " + dbSourceDirectory + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		// FileUtils.copyDirectory(new File(localDir+File.separatorChar+"src"), new
		// File(dbSourceDirectory));

		// loop over all schemas
		for (String schema : schemaListString.split(",")) {
			FileUtils.copyDirectory(
					new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "src-sql"),
					new File(dbSourceDirectory + File.separator + schema));
		}

		// ----------------------------------------------------------
		// patch directory
		// ----------------------------------------------------------
		Msg.println("patch directory from: " + localDir + File.separatorChar + "patches" + "\n                to  : "
				+ patchDirectory + "\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");
		FileUtils.forceMkdir(new File(patchDirectory));

		// ----------------------------------------------------------
		// bin directory
		// ----------------------------------------------------------
		Msg.println("process bin directory\n");
		// Utils.waitForEnter("Please press <enter> to proceed ...");

		// copy / replace all files in the bin directory
		Iterator<File> it1 = FileUtils.iterateFiles(
				new File(localDir + File.separatorChar + "configure-templates" + File.separatorChar + "bin"), osScriptSuffixList,
				false);
		while (it1.hasNext()) {
			File f = (File) it1.next();
			Path path = Paths.get(f.getName());
			Msg.println("  process file " + path.getFileName());

			String contents = FileUtils.readFileToString(f, Charset.defaultCharset());
			contents = contents.replace("#OPAL_INSTALLER_USER_CONFIG_DIR#", localConfigDirectory);
			contents = contents.replace("#OPAL_INSTALLER_HOME_DIR#", swDirectory);
			contents = contents.replace("#OPAL_INSTALLER_PATCH_TEMPLATE_DIR#", templateDirectory);
			contents = contents.replace("#OPAL_INSTALLER_SRC_SQL_DIR#", dbSourceDirectory);
			contents = contents.replace("#OPAL_INSTALLER_PATCH_DIR#", patchDirectory);
			
			FileUtils.writeStringToFile(
					new File(swDirectory + File.separator + "bin" + File.separator + path.getFileName()), contents,
					Charset.defaultCharset());
		}
		
		// ----------------------------------------------------------
		// make shell scripts executable again, got lost during file copy
		// ----------------------------------------------------------
		if (osIsWindows()) {
		    //builder.command("cmd.exe", "/c", "dir");
			// nothing to do here, privileges are working
		} else {
			Msg.println("\nset privileges for *.sh files\n");
			ProcessBuilder builder = new ProcessBuilder();
			builder.command("bash", "-c", "find " + localConfigDirectory + " " + swDirectory + " " + templateDirectory + " -type f -iname \"*.sh\" -exec chmod +x {} \\;");			
			try {
				Process process = builder.start();

	            BufferedReader reader =
	                    new BufferedReader(new InputStreamReader(process.getInputStream()));

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

		// close keyboard input scanner
		kbd.close();
	}

	/**
	 * Read the values from the command line
	 * 
	 * @param args
	 */
	private void readConfig(String[] args) {
		// read command line parameters and exit if no know command found
		if (args.length == 0 || args.length == 1) {
			if (args.length == 1) {
				this.baseDir = args[0];
			}
//			
//			this.sourcePathName = args[1];
//			this.patchFilesName = args[2];
		} else {
			showUsage();

			System.exit(1);
		}

	}

	private static void showUsage() {
		Msg.println("");
		Msg.println("Configures the initial setup, copies the files into the right location.");
		Msg.println("");
		Msg.println("Usage: ");
		Msg.println("");
		Msg.println("java -jar installer.jar configure [base directory]");
		Msg.println("");
		Msg.println("	The base directory will be used as a default for the additional directories. ");
		Msg.println("   But all of them can be changed.");
		Msg.println("");
	}

}