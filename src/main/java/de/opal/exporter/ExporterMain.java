package de.opal.exporter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import de.opal.installer.config.ConfigConnectionPool;
import de.opal.installer.config.ConfigManagerConnectionPool;
import de.opal.installer.util.Msg;
import de.opal.utils.FileIO;
import de.opal.utils.VersionInfo;

public class ExporterMain {

	/*--------------------------------------------------------------------------------------
	 * Other variables
	 */
	public static final Logger log = LogManager.getLogger(ExporterMain.class.getName());
	private String user;
	private String pwd;
	private String connectStr;
	private HashMap<String, ArrayList<String>> dependentObjectsMap = new HashMap<String, ArrayList<String>>();
	// private HashMap<String, String> extensionMappingsMap = new HashMap<String,
	// String>();
	// private HashMap<String, String> directoryMappingsMap = new HashMap<String,
	// String>();
	private HashMap<String, String> filenameTemplatesMap = new HashMap<String, String>();

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "-v", aliases = "--version", usage = "show version information", help = true)
	private boolean showVersion;

	@Option(name = "--url", usage = "database connection jdbc url, \ne.g.: scott/tiger@localhost:1521:ORCL", metaVar = "<jdbc url>", forbids = {
			"--connection-pool-file", "--connection-pool-name" })
	private String jdbcURL;

	@Option(name = "--connection-pool-file", usage = "connection pool file\ne.g.: connections-dev.json", metaVar = "<file>", forbids = {
			"--url" })
	private String connectionPoolFile;

	@Option(name = "--connection-pool-name", usage = "connection pool name\ne.g.: scott", metaVar = "<connection pool name>", forbids = {
			"--url" })
	private String connectionPoolName;

	@Option(name = "--output-dir", usage = "output directory, e.g. '.' or '/u01/project/src/sql'", metaVar = "<directory>", required = true)
	private String outputDir;

	// passing in multiple parameters: -p p1 p2 p3
	// https://stackoverflow.com/questions/23800070/multiple-args-with-arg4j
	// @Option(name = "-p", handler=WellBehavedStringArrayOptionHandler.class)
	// private List<String> pList=new ArrayList<String>();

	@Option(name = "--includes", handler = WellBehavedStringArrayOptionHandler.class, usage = "include filter, e.g.: %XLIB% or *XLIB*", metaVar = "<filter1> [<filter2>] ... [n]")
	private List<String> includeFilters = new ArrayList<String>();

	@Option(name = "--include-types", handler = WellBehavedStringArrayOptionHandler.class, usage = "include types, e.g.: TABLE PACKAGE", metaVar = "<type1> [<type2>] ... [n]")
	private List<String> includeTypes = new ArrayList<String>();

	@Option(name = "--excludes", handler = WellBehavedStringArrayOptionHandler.class, usage = "exclude filter, e.g.: %AQ$% %SYS_% or ", metaVar = "<type1> [<type2>] ... [n]")
	private List<String> excludeFilters = new ArrayList<String>();

	@Option(name = "--exclude-types", handler = WellBehavedStringArrayOptionHandler.class, usage = "exclude types, e.g.: JOB", metaVar = "<type1> [<type2>] ... [n]")
	private List<String> excludeTypes = new ArrayList<String>();

	@Option(name = "--include-schemas", handler = WellBehavedStringArrayOptionHandler.class, usage = "schemas to be included, only relevant when connecting as DBA", metaVar = "<schema1> [<schema2>] ... [n]")
	private List<String> schemas = new ArrayList<String>();

	@Option(name = "--dependent-objects", handler = WellBehavedStringArrayOptionHandler.class, usage = "dependent objects, e.g. TABLE:COMMENT,INDEX", metaVar = "<type>:<deptype1>,<deptype2> ... [n]")
	private List<String> dependentObjects = new ArrayList<String>();

//	@Option(name = "--extension-mappings", handler = WellBehavedStringArrayOptionHandler.class, usage = "mapping of object types to filename suffixes, e.g.: DEFAULT:sql PACKAGE:pks", metaVar = "<map1> [<map2>] ... [n]")
//	private List<String> extensionMappings = new ArrayList<String>();

//	@Option(name = "--directory-mappings", handler = WellBehavedStringArrayOptionHandler.class, usage = "mapping of object types to directories, e.g.: PACKAGE:package \"package body:package\"", metaVar = "<map1> [<map2>] ... [n]")
//	private List<String> directoryMappings = new ArrayList<String>();

	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	// @Option(name = "-a", aliases = "--all_objects", usage = "by default, the
	// objects are selected from user_objects. With this switch it will be selected
	// from all_objects")
	// private boolean selectFromAllObjects = false;

	@Option(name = "--skip-errors", usage = "ORA- errors will not cause the program to abort")
	private boolean skipErrors = false;

	@Option(name = "--skip-export", usage = "skip the export, this way only the pre- and post-scripts are run")
	private boolean skipExport = false;

	@Option(name = "--pre-scripts", usage = "script (sqlplus/sqlcl) that is running to initialize the session, similar to the login.sql file for sqlplus, e.g. ./login.sql or ./init.sql", metaVar = "<script> [<script2>] ...")
	private List<File> preScripts = new ArrayList<File>();

	@Option(name = "--post-scripts", usage = "script (sqlplus/sqlcl) that is running in the end to export custom objects, e.g. ./apex.sql", metaVar = "<script> [<script2>] ...")
	private List<File> postScripts = new ArrayList<File>();

	@Option(name = "--silent", usage = "turns off prompts")
	private boolean isSilent = false;

	/*
	 * @Option(name = "--filename-template", usage =
	 * "template for constructing the filename\n" +
	 * "e.g.: #schema#/#object_type#/#object_name#.#ext#\n\n" +
	 * "#schema#             - schema name in lower case\n" +
	 * "#object_type#        - lower case type name: 'table'\n" +
	 * "#object_type_plural# - lower case type name in plural: 'tables'\n" +
	 * "#object_name#        - lower case object name\n" +
	 * "#ext#                - lower case extension: 'sql' or 'pks'\n" +
	 * "#SCHEMA#             - upper case schema name\n" +
	 * "#OBJECT_TYPE#        - upper case object type name: 'TABLE' or 'INDEX'\n" +
	 * "#OBJECT_TYPE_PLURAL# - upper case object type name in plural: 'TABLES'\n" +
	 * "#OBJECT_NAME#        - upper case object name\n" +
	 * "#EXT#                - upper case extension: 'SQL' or 'PKS'", metaVar =
	 * "<template structure>", required = false) private String filenameTemplate =
	 * "#schema#/#object_type_plural#/#object_name#.#ext#";
	 */

	@Option(name = "--filename-templates", handler = WellBehavedStringArrayOptionHandler.class, usage = "templates for constructing the filename per object type\n"
			+ "e.g.: #schema#/#object_type#/#object_name#.#ext#\n\n"
			+ "#schema#             - schema name in lower case\n"
			+ "#object_type#        - lower case type name: 'table'\n"
			+ "#object_type_plural# - lower case type name in plural: 'tables'\n"
			+ "#object_name#        - lower case object name\n"
			+ "#ext#                - lower case extension: 'sql' or 'pks'\n"
			+ "#SCHEMA#             - upper case schema name\n"
			+ "#OBJECT_TYPE#        - upper case object type name: 'TABLE' or 'INDEX'\n"
			+ "#OBJECT_TYPE_PLURAL# - upper case object type name in plural: 'TABLES'\n"
			+ "#OBJECT_NAME#        - upper case object name\n"
			+ "#EXT#                - upper case extension: 'SQL' or 'PKS'", metaVar = "<template structure>", required = false)
	private List<String> filenameTemplates = new ArrayList<String>();
	// default will be: "default:#schema#/#object_type_plural#/#object_name#.#ext#";

	@Option(name = "--filename-replace-blanks", usage = "replaces blanks in the filename with an _, e.g. PACKAGE BODY=>PACKAGE_BODY")
	private boolean filenameReplaceBlanks = true;

	@Option(name = "--script-working-directory", usage = "working directory for running sqlcl scripts (-pre and -post), e.g. '.' or '/u01/project/src/sql'. The default is the environment variable OPAL_TOOLS_SRC_SQL_DIR", metaVar = "<directory>", required = false)
	private String workingDirectorySQLcl;

	@Option(name = "--export-template-dir", usage = "directory for object templates, e.g. /u01/project/opal-tools/export-templates", metaVar = "<directory>", required = false)
	private String exportTemplateDir;

	@Option(name = "--config-file", usage = "configuration file\ne.g.: connections-dev.json", metaVar = "<file>")
	private String configFileName;

	@Option(name = "--parallel-degree", usage = "the database statements are executed in parallel, e.g. 10", metaVar = "<level>")
	private int parallelThreads=1;

	/**
	 * Main entry point to the DB Exporter
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.debug("*** start ***");

		// we need second instance because in the first parsing
		// for the switch --config-file all variables are already initialized
		// and we need to reset them ... or create a new instance
		ExporterMain configFileExporter = new ExporterMain();
		String[] configFileArgs = configFileExporter.parseConfigFileArgs(args);

		// now initialize the real class
		ExporterMain dbExporter = new ExporterMain();

		// merge the argument lists so that the command line args can OVERRIDE the
		// ones from the config file
		dbExporter.parseParameters(dbExporter.mergeArgs(configFileArgs, args));
		dbExporter.transformParams();
		dbExporter.dumpParameters();
		dbExporter.showHeaderInfo();

		Exporter exporter = new Exporter(dbExporter.user, dbExporter.pwd, dbExporter.connectStr, dbExporter.outputDir,
				dbExporter.skipErrors, dbExporter.dependentObjectsMap, dbExporter.isSilent,
				/*
				 * dbExporter.extensionMappingsMap, dbExporter.directoryMappingsMap,
				 * dbExporter.filenameTemplate,
				 */
				dbExporter.filenameReplaceBlanks, dbExporter.workingDirectorySQLcl, dbExporter.skipExport,
				dbExporter.filenameTemplatesMap, dbExporter.exportTemplateDir, dbExporter.parallelThreads);
		exporter.export(dbExporter.preScripts, dbExporter.postScripts, dbExporter.includeFilters,
				dbExporter.excludeFilters, dbExporter.schemas, dbExporter.includeTypes, dbExporter.excludeTypes);

		Msg.println("\n*** done.\n");

		log.debug("*** end ***");
	}

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.exporter.DBExporter [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println("  Example: java de.opal.exporter.DBExporter" + parser.printExample(OptionHandlerFilter.PUBLIC));
	}

	private String[] readArgsFromConfigFile(String configFilename) {
		ArrayList<String> args = new ArrayList<String>();

		try {
			String textContent = FileIO.fileToString(configFilename);
			BufferedReader bufReader = new BufferedReader(new StringReader(textContent));

			String line = null;
			while ((line = bufReader.readLine()) != null) {
				line = line.trim();

				if (!line.startsWith("#") && !line.isEmpty()) {
					// https://stackoverflow.com/questions/7804335/split-string-on-spaces-in-java-except-if-between-quotes-i-e-treat-hello-wor
					// split string on blanks but not within quotes
					Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
					while (m.find()) {
						String arg = m.group(1);

						// https://commons.apache.org/proper/commons-text/apidocs/org/apache/commons/text/StringSubstitutor.html
						final StringSubstitutor interpolator = StringSubstitutor.createInterpolator();
						interpolator.setEnableSubstitutionInVariables(true); // Allows for nested $'s.
						arg = interpolator.replace(arg);

						// remove surrounding quotes
						arg = arg.replace("\"", "");

						args.add(arg);
					}
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * try { props.load(new FileInputStream(configFilename));
		 * 
		 * Set<String> keys = props.stringPropertyNames(); for (String key : keys) {
		 * args.add(key); args.add(props.getProperty(key)); } } catch (IOException e) {
		 * // TODO Auto-generated catch block e.printStackTrace(); }
		 */

		return args.stream().toArray(String[]::new);
	}

	private String[] mergeArgs(String[] args1, String[] args2) {
		if (args1 == null)
			return args2;
		if (args2 == null)
			return args1;

		List<String> list = new ArrayList<String>(Arrays.asList(args1));
		list.addAll(Arrays.asList(args2));
		String[] args3 = list.stream().toArray(String[]::new);

		return args3;
	}

	public String[] parseConfigFileArgs(String[] args) throws IOException {
		CmdLineParser parser = new CmdLineParser(this);
		String[] configFileArgs = null;

		try {
			// first parse to get config file name
			parser.parseArgument(args);

		} catch (Exception e) {
			// suppress first exception
			// it could be that required fields are in the config file
		}

		if (this.configFileName != null && !this.configFileName.isEmpty()) {
			// read optional properties file and put it in the args[] array
			configFileArgs = readArgsFromConfigFile(this.configFileName);
		}
		return configFileArgs;
	}

	/**
	 * doMain is actually doing the work
	 * 
	 * @param args
	 * @throws IOException
	 */
	public void parseParameters(String[] args) throws IOException {
		ParserProperties properties = ParserProperties.defaults();
		properties.withUsageWidth(130);
		properties.withOptionSorter(null);
		CmdLineParser parser = new CmdLineParser(this, properties);

		log.debug("start parsing");
		try {

			// parse the arguments.
			parser.parseArgument(args);

			// help can be displayed without -url being given
			// else -url is required
			/*
			 * if (showHelp) { System.out.
			 * println("\njava de.opal.exporter.DBExporter [options...] arguments..."); //
			 * print the list of available options parser.printUsage(System.out);
			 * System.out.println();
			 * 
			 * // print option sample. This is useful some time
			 * System.out.println("  Example: java de.opal.exporter.DBExporter" +
			 * parser.printExample(ALL));
			 * 
			 * System.exit(0);
			 * 
			 * } else { if (this.jdbcURL == null) { throw new CmdLineException(parser,
			 * "parameter url is required"); } if (this.outputDir == null) { throw new
			 * CmdLineException(parser, "parameter output directory is required"); } }
			 */
			// you can parse additional arguments if you want.
			// parser.parseArgument("more","args");

			// after parsing arguments, you should check
			// if enough arguments are given.
			// if (arguments.isEmpty())
			// throw new CmdLineException(parser, "No argument is given");

			if (this.showVersion) {
				VersionInfo.showVersionInfo(this.getClass(), "OPAL Installer", true);
			}

			// check whether jdbcURL OR connection pool is specified correctly
			if (this.showHelp) {
				showUsage(System.out, parser);
				System.exit(0);
			}

			// check more complex parameters
			if (this.jdbcURL != null || (this.connectionPoolFile != null && this.connectionPoolName != null)) {
				// ok
			} else {
				throw new CmdLineException(parser,
						"Specify either --url or (--connection-pool-file and --connection-pool-name)");
			}

			// jdbcURL
			if (this.jdbcURL != null) {
				// extract parts of jdbcURL into separate variables
				Pattern p = Pattern.compile("^(.+)/(.+)@(.*)$");
				Matcher m = p.matcher(this.jdbcURL);

				// if an occurrence if a pattern was found in a given string...
				if (m.find()) {
					// ...then you can use group() methods.
					// System.out.println(m.group(0)); // whole matched expression
					this.user = m.group(1);
					this.pwd = m.group(2);
					this.connectStr = m.group(3);
				}
			} else {
				// conn pool
				// both have the same structure
				if (!new File(this.connectionPoolFile).exists()) {
					throw new CmdLineException(parser,
							"connection pool file " + this.connectionPoolFile + " not found");
				}

				ConfigManagerConnectionPool configManagerConnectionPools = new ConfigManagerConnectionPool(
						this.connectionPoolFile);

				// encrypt passwords if required
				if (configManagerConnectionPools.hasUnencryptedPasswords()) {
					configManagerConnectionPools.encryptPasswords(
							configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFile));
					// dump JSON file
					configManagerConnectionPools.writeJSONConf();
				}
				// now decrypt the passwords so that they can be used internally in the program
				configManagerConnectionPools.decryptPasswords(
						configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFile));

				for (ConfigConnectionPool pool : configManagerConnectionPools
						.getConfigDataConnectionPool().connectionPools) {
					if (pool.name.toUpperCase().contentEquals(this.connectionPoolName.toUpperCase())) {
						this.user = pool.user;
						this.pwd = pool.password;
						this.connectStr = pool.connectString;
					}
				}
				if (this.user == null)
					throw new CmdLineException(parser, "connection pool " + this.connectionPoolName
							+ " could not be found in file " + this.connectionPoolFile);

				log.debug("get connection " + this.connectionPoolName + " from " + this.connectionPoolFile);
				log.debug("user: " + this.user);
				log.debug("pwd: " + this.pwd);
				log.debug("connectStr: " + this.connectStr);

			}

		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			showUsage(System.err, parser);

			System.exit(1);
		}

	}

	public void transformParams() {
		this.user = this.user.trim().toUpperCase();

		// if no filter is given, everything should be exported
		if (includeFilters.isEmpty()) {
			includeFilters.add("%");
		}

		// use user as first entry in empty schemas list
		if (schemas.isEmpty()) {
			schemas.add(user);
		}
		// make schemas uppercase
		for (int i = 0; i < schemas.size(); i++) {
			schemas.set(i, schemas.get(i).trim().toUpperCase());
		}

		// make filter uppercase
		// replace *=>% for each filter
		for (int i = 0; i < includeFilters.size(); i++) {
			includeFilters.set(i, includeFilters.get(i).trim().toUpperCase().replace("*", "%"));
		}
		for (int i = 0; i < excludeFilters.size(); i++) {
			excludeFilters.set(i, excludeFilters.get(i).trim().toUpperCase().replace("*", "%"));
		}
		for (int i = 0; i < this.includeTypes.size(); i++) {
			includeTypes.set(i, includeTypes.get(i).trim().toUpperCase().replace("*", "%"));
		}
		for (int i = 0; i < this.excludeTypes.size(); i++) {
			excludeTypes.set(i, excludeTypes.get(i).trim().toUpperCase().replace("*", "%"));
		}

		// make dependent objects uppercase
		for (int i = 0; i < this.dependentObjects.size(); i++) {
			dependentObjects.set(i, dependentObjects.get(i).trim().toUpperCase());
		}

		/*
		 * // make extension mappings uppercase for (String ext :
		 * this.extensionMappings) { String objectType = ext.split(":")[0].trim();
		 * String suffix = ext.split(":")[1].trim();
		 * 
		 * this.extensionMappingsMap.put(objectType.toUpperCase(), suffix); } // make
		 * directory mappings uppercase for (String ext : this.directoryMappings) {
		 * String objectType = ext.split(":")[0].trim(); String directory =
		 * ext.split(":")[1].trim();
		 * 
		 * this.directoryMappingsMap.put(objectType.toUpperCase(), directory); }
		 */

		// transform dependent object list into map
		for (String dep : this.dependentObjects) {
			String objType = dep.split(":")[0].trim();
			String depObj = dep.split(":")[1].trim();

			ArrayList<String> depObjects = new ArrayList<String>();
			Arrays.asList(depObj.split(",")).forEach((e) -> depObjects.add(e.trim()));

			this.dependentObjectsMap.put(objType, depObjects);
		}

		// filename templates => set default
		if (filenameTemplates.size() == 0)
			filenameTemplates.add("default:#schema#/#object_type_plural#/#object_name#.sql");

		// filename templates => convert to map
		for (String template : this.filenameTemplates) {
			String objectType = template.split(":")[0].trim();
			String filenameTemplate = template.split(":")[1].trim();

			this.filenameTemplatesMap.put(objectType.toUpperCase(), filenameTemplate);
		}

		if (this.workingDirectorySQLcl == null) {
			// set default to environment variable OPAL_TOOLS_SRC_SQL_DIR
			log.debug("set default for workingDirectorySQLcl: " + this.workingDirectorySQLcl);
			this.workingDirectorySQLcl = System.getenv("OPAL_TOOLS_SRC_SQL_DIR").trim();
		} else
			this.workingDirectorySQLcl = this.workingDirectorySQLcl.trim();
	}

	private void showHeaderInfo() {
		String lSep = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();

		sb.append(VersionInfo.getVersionInfo(this.getClass(), VersionInfo.OPAL_EXPORTER, false) + lSep);
		sb.append("****************************" + lSep);
		sb.append("* User                     : " + this.user + lSep);
		sb.append("* ConnectStr               : " + this.connectStr + lSep);
		sb.append("* OutputDirectory          : " + this.outputDir + lSep);

		if (!this.skipExport) {
			if (!this.includeFilters.isEmpty() || !this.excludeFilters.isEmpty() || !this.schemas.isEmpty())
				sb.append("*" + lSep);
			if (!this.includeFilters.isEmpty())
				sb.append("* IncludeFilter            : " + this.includeFilters + lSep);
			if (!this.includeTypes.isEmpty())
				sb.append("* IncludeTypes             : " + this.includeTypes + lSep);
			if (!this.excludeFilters.isEmpty())
				sb.append("* ExcludeFilter            : " + this.excludeFilters + lSep);
			if (!this.excludeTypes.isEmpty())
				sb.append("* ExcludeTypes             : " + this.excludeTypes + lSep);
			if (!this.schemas.isEmpty())
				sb.append("* Schemas                  : " + this.schemas + lSep);
			if (!this.dependentObjects.isEmpty())
				sb.append("* Dependent Objects        : " + this.dependentObjects + lSep);
		}

		if (this.preScripts.size() > 0 || this.postScripts.size() > 0) {
			sb.append("*" + lSep);
			// sb.append("* Arguments : " + this.arguments+lSep);
			if (this.workingDirectorySQLcl != null && (this.preScripts.size() > 0 || this.postScripts.size() > 0))
				sb.append("* Script Working Directory : " + this.workingDirectorySQLcl + lSep);
			if (this.preScripts.size() > 0)
				sb.append("* Pre Scripts              : " + this.preScripts.toString() + lSep);
			if (this.postScripts.size() > 0)
				sb.append("* Post Scripts             : " + this.postScripts.toString() + lSep);
		}

		if (!this.skipExport) {
			sb.append("*" + lSep);
			/*
			 * if (this.extensionMappings != null) sb.append("* Extension Mapping        : "
			 * + this.extensionMappings + lSep); if (this.directoryMappings != null)
			 * sb.append("* Directory Mapping        : " + this.directoryMappings + lSep);
			 */
			if (this.filenameTemplates.size() > 0) {
				sb.append("* Filename Templates       : " + this.filenameTemplates.toString() + lSep);
			}
			sb.append("* Filename Replace Blanks? : " + this.filenameReplaceBlanks + lSep);
			if (this.exportTemplateDir != null) {
				sb.append("* Export Template Directory: " + this.exportTemplateDir + lSep);
			}
			if (this.parallelThreads > 1) {
				sb.append("* Parallel Degree          : " + this.parallelThreads + lSep);
			}
		}

		sb.append("*" + lSep);

		if (!this.skipExport) {
			sb.append("* SkipErrors?              : " + this.skipErrors + lSep);
		}
		sb.append("* Silent (no prompts)?     : " + this.isSilent + lSep);
//		if (this.templateDir != null)
//			sb.append("* Template Directory       : " + this.templateDir + lSep);

		sb.append("*" + lSep);
		sb.append("* File Encoding (System)   : " + System.getProperty("file.encoding") + lSep);
		sb.append("****************************" + lSep);

		Msg.println(sb.toString());
	}

	private void dumpParameters() {
		log.debug("*** Options");
		log.debug("-url: " + this.jdbcURL);
		log.debug("-o  : " + outputDir);

		log.debug("Include-Filters:");
		for (String flt : this.includeFilters) {
			log.debug("  - " + flt);
		}

		log.debug("Exclude-Filters:");
		for (String flt : this.excludeFilters) {
			log.debug("  - " + flt);
		}

		log.debug("--init_sql_file: " + this.preScripts.toString());
		log.debug("--custom_export: " + this.postScripts.toString());
		// log.debug("--template_dir: " + this.templateDir);

		/*
		 * log.debug("pList:"); for (String flt : this.pList) { log.debug("  - " + flt);
		 * }
		 */
		// access non-option arguments
		log.debug("*** Arguments");
		log.debug("other arguments are:");
		for (String s : arguments)
			log.debug(s);

	}
}
