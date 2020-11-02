package de.opal.exporter;

import static org.kohsuke.args4j.ExampleMode.ALL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import de.opal.installer.util.Msg;

public class DBExporter {

	/*--------------------------------------------------------------------------------------
	 * Other variables
	 */
	public static final Logger log = LogManager.getLogger(DBExporter.class.getName());
	private String user = "";
	private String pwd = "";
	private String connectStr = "";
	private String version = ""; // will be loaded from file version.txt which will be populated by the gradle
									// build process
	private HashMap<String, ArrayList<String>> dependentObjectsMap = new HashMap<String, ArrayList<String>>();
	private HashMap<String, String> extensionMappingsMap = new HashMap<String, String>();
	private HashMap<String, String> directoryMappingsMap = new HashMap<String, String>();

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-url", usage = "database connection jdbc url, e.g.: scott/tiger@localhost:1521:ORCL", metaVar = "<jdbc url>", required = true)
	private String jdbcURL;

	@Option(name = "-o", usage = "output directory, e.g. '.' or '/u01/project/src/sql'", aliases = "--output-dir", metaVar = "<directory>", required = true)
	private String outputDir;

	// passing in multiple parameters: -p p1 p2 p3
	// https://stackoverflow.com/questions/23800070/multiple-args-with-arg4j
	// @Option(name = "-p", handler=WellBehavedStringArrayOptionHandler.class)
	// private List<String> pList=new ArrayList<String>();

	@Option(name = "-i", handler = WellBehavedStringArrayOptionHandler.class, usage = "include filter, e.g.: %XLIB%", aliases = "--include", metaVar = "<filter1> [<filter2>] ... [n]")
	private List<String> includeFilters = new ArrayList<String>();

	@Option(name = "-it", handler = WellBehavedStringArrayOptionHandler.class, usage = "include types, e.g.: TABLE PACKAGE", aliases = "--include-types", metaVar = "<type1> [<type2>] ... [n]")
	private List<String> includeTypes = new ArrayList<String>();

	@Option(name = "-e", handler = WellBehavedStringArrayOptionHandler.class, usage = "exclude filter, e.g.: %AQ$% %SYS_%", aliases = "--exclude", metaVar = "<type1> [<type2>] ... [n]")
	private List<String> excludeFilters = new ArrayList<String>();

	@Option(name = "-et", handler = WellBehavedStringArrayOptionHandler.class, usage = "exclude types, e.g.: JOB", aliases = "--exclude-types", metaVar = "<type1> [<type2>] ... [n]")
	private List<String> excludeTypes = new ArrayList<String>();

	@Option(name = "-s", handler = WellBehavedStringArrayOptionHandler.class, usage = "schemas to be included, only relevant when connecting as DBA", aliases = "--schemas", metaVar = "<schema1> [<schema2>] ... [n]")
	private List<String> schemas = new ArrayList<String>();

	@Option(name = "-d", handler = WellBehavedStringArrayOptionHandler.class, usage = "dependent objects, e.g. TABLE:COMMENTS,INDEXES", aliases = "--dependent-objects", metaVar = "<type>:<deptype1>,<deptype2> ... [n]")
	private List<String> dependentObjects = new ArrayList<String>();

	@Option(name = "-em", handler = WellBehavedStringArrayOptionHandler.class, usage = "mapping of object types to filename suffixes, e.g.: DEFAULT:sql PACKAGE:pks", aliases = "--extension-map", metaVar = "<map1> [<map2>] ... [n]")
	private List<String> extensionMappings = new ArrayList<String>();
	
	@Option(name = "-dm", handler = WellBehavedStringArrayOptionHandler.class, usage = "mapping of object types to directories, e.g.: PACKAGE:package \"package body:package\"", aliases = "--directory-map", metaVar = "<map1> [<map2>] ... [n]")
	private List<String> directoryMappings = new ArrayList<String>();
	
	// receives other command line parameters than options
	@Argument
	private List<String> arguments = new ArrayList<String>();

	// @Option(name = "-a", aliases = "--all_objects", usage = "by default, the
	// objects are selected from user_objects. With this switch it will be selected
	// from all_objects")
	// private boolean selectFromAllObjects = false;

	@Option(name = "-se", aliases = "--skip-errors", usage = "ORA- errors will not cause the program to abort")
	private boolean skipErrors = false;

	// @Option(name = "-t", usage = "template directory", aliases =
	// "--template_dir", metaVar = "TEMPLATE DIRECTORY")
	// private File templateDir;

	@Option(name = "-cf", usage = "custom export file, e.g. ./apex.sql", aliases = "--custom-file", metaVar = "<sql script>")
	private File customExportSQLFile;

	@Option(name = "-if", usage = "sql initialization file that is executed when the db session is initialized, similar to the login.sql file for sqlplus, e.g. ./login.sql or ./init.sql", aliases = "--init-file", metaVar = "<sql script>")
	private File customSQLInitFile;

	@Option(name = "--silent", usage = "turns of prompts")
	private boolean isSilent = false;

	// @Option(name = "-h", aliases = "--help", usage = "display this help page")
	// private boolean showHelp = false;

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

	/**
	 * Main entry point to the DB Exporter
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.debug("*** start ***");

		DBExporter dbExporter = new DBExporter();

		dbExporter.readVersionFromFile();
		dbExporter.parseParameters(args);
		dbExporter.transformParams();
		dbExporter.dumpParameters();
		dbExporter.showHeaderInfo();

		Exporter exporter = new Exporter(dbExporter.user, dbExporter.pwd, dbExporter.connectStr, dbExporter.outputDir,
				dbExporter.skipErrors, dbExporter.dependentObjectsMap, dbExporter.isSilent,dbExporter.extensionMappingsMap, dbExporter.directoryMappingsMap);
		exporter.export(dbExporter.customSQLInitFile, dbExporter.customExportSQLFile, dbExporter.includeFilters,
				dbExporter.excludeFilters, dbExporter.schemas, dbExporter.includeTypes, dbExporter.excludeTypes);

		Msg.println("\n*** done.");

		log.debug("*** end ***");
	}

	/**
	 * doMain is actually doing the work
	 * 
	 * @param args
	 * @throws IOException
	 */
	public void parseParameters(String[] args) throws IOException {
		ParserProperties properties = ParserProperties.defaults();
		properties.withUsageWidth(120);
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

		} catch (CmdLineException e) {
			// if there's a problem in the command line,
			// you'll get this exception. this will report
			// an error message.
			System.err.println(e.getMessage());
			System.err.println("\njava de.opal.exporter.DBExporter [options...] arguments...");
			// print the list of available options
			parser.printUsage(System.err);
			System.err.println();

			// print option sample. This is useful some time
			System.err.println("  Example: java de.opal.exporter.DBExporter" + parser.printExample(ALL));

			System.exit(1);
		}

	}

	public void transformParams() {
		this.user = this.user.toUpperCase();

		if (includeFilters.isEmpty()) {
			includeFilters.add("%");
		}
		// use user as first entry in empty schemas list
		if (schemas.isEmpty()) {
			schemas.add(user);
		}
		// make schemas uppercase
		for (int i = 0; i < schemas.size(); i++) {
			schemas.set(i, schemas.get(i).toUpperCase());
		}

		// make filter uppercase
		for (int i = 0; i < includeFilters.size(); i++) {
			includeFilters.set(i, includeFilters.get(i).toUpperCase());
		}
		for (int i = 0; i < excludeFilters.size(); i++) {
			excludeFilters.set(i, excludeFilters.get(i).toUpperCase());
		}
		for (int i = 0; i < this.includeTypes.size(); i++) {
			includeTypes.set(i, includeTypes.get(i).toUpperCase());
		}
		for (int i = 0; i < this.excludeTypes.size(); i++) {
			excludeTypes.set(i, excludeTypes.get(i).toUpperCase());
		}

		// make dependent objects uppercase
		for (int i = 0; i < this.dependentObjects.size(); i++) {
			dependentObjects.set(i, dependentObjects.get(i).toUpperCase());
		}
		// make extension mappings uppercase
		for (String ext : this.extensionMappings) {
			String objectType = ext.split(":")[0];
			String suffix = ext.split(":")[1];

			this.extensionMappingsMap.put(objectType.toUpperCase(),suffix);
		}
		// make directory mappings uppercase
		for (String ext : this.directoryMappings) {
			String objectType = ext.split(":")[0];
			String directory = ext.split(":")[1];

			this.directoryMappingsMap.put(objectType.toUpperCase(),directory);
		}

		// transform dependent object list into map
		for (String dep : this.dependentObjects) {
			String objType = dep.split(":")[0];
			String depObj = dep.split(":")[1];

			this.dependentObjectsMap.put(objType, new ArrayList<String>(Arrays.asList(depObj.split(","))));
		}
	}

	private void showHeaderInfo() {
		String lSep = System.getProperty("line.separator");
		StringBuilder sb = new StringBuilder();

		sb.append("OPAL Exporter version " + this.version + lSep);
		sb.append("*************************" + lSep);
		sb.append("* User                  : " + this.user + lSep);
		sb.append("* ConnectStr            : " + this.connectStr + lSep);
		sb.append("* OutputDirectory       : " + this.outputDir + lSep);

		if (!this.includeFilters.isEmpty() || !this.excludeFilters.isEmpty() || !this.schemas.isEmpty())
			sb.append("*" + lSep);
		if (!this.includeFilters.isEmpty())
			sb.append("* IncludeFilter         : " + this.includeFilters + lSep);
		if (!this.includeTypes.isEmpty())
			sb.append("* IncludeTypes          : " + this.includeTypes + lSep);
		if (!this.excludeFilters.isEmpty())
			sb.append("* ExcludeFilter         : " + this.excludeFilters + lSep);
		if (!this.excludeTypes.isEmpty())
			sb.append("* ExcludeTypes          : " + this.excludeTypes + lSep);
		if (!this.schemas.isEmpty())
			sb.append("* Schemas               : " + this.schemas + lSep);
		if (!this.dependentObjects.isEmpty())
			sb.append("* Dependent Objects     : " + this.dependentObjects + lSep);

		sb.append("*" + lSep);
		// sb.append("* Arguments : " + this.arguments+lSep);
		if (this.customSQLInitFile != null)
			sb.append("* CustomSQLInitFile     : " + this.customSQLInitFile + lSep);
		if (this.customExportSQLFile != null)
			sb.append("* CustomExportSQLFile   : " + this.customExportSQLFile + lSep);

		sb.append("*" + lSep);
		if (this.extensionMappings != null)
			sb.append("* Extension Mapping     : " + this.extensionMappings + lSep);
		sb.append("*" + lSep);
		if (this.directoryMappings != null)
			sb.append("* Directory Mapping     : " + this.directoryMappings + lSep);
		
		
		sb.append("* SkipErrors?           : " + this.skipErrors + lSep);
		sb.append("* Silent (no prompts)?  : " + this.isSilent + lSep);
//		if (this.templateDir != null)
//			sb.append("* Template Directory    : " + this.templateDir + lSep);

		sb.append("*" + lSep);
		sb.append("* File Encoding (System): " + System.getProperty("file.encoding") + lSep);
		sb.append("*************************" + lSep);

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

		log.debug("--init_sql_file: " + this.customSQLInitFile);
		log.debug("--custom_export: " + this.customExportSQLFile);
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
