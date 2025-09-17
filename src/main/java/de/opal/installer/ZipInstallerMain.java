package de.opal.installer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import de.opal.exporter.WellBehavedStringArrayOptionHandler;
import de.opal.installer.zip.ZipInstaller;
import de.opal.utils.MsgLog;

public class ZipInstallerMain {

	/*--------------------------------------------------------------------------------------
	 * Other variables
	 */
	public static final Logger log = LogManager.getLogger(ZipInstallerMain.class.getName());

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "--zip-file", usage = "zip file name, e.g. install.zip", metaVar = "<file>", required=true)
	private String zipFileName;

	@Option(name = "--config-file", usage = "configuration file\ne.g.: opal-installer.json", metaVar = "<file>", required = true)
	private String configFileName;

	@Option(name = "--defaults-config-file", usage = "configuration file for defaults\ncurrently the mappings for NLS encodings \nfor the zipInstaller are supported\ne.g.: conf/opal-installer.json", metaVar = "<file>")
	private String defaultsConfigFileName;

	@Option(name = "--mandatory-attributes", handler = WellBehavedStringArrayOptionHandler.class, usage = "list of attributes that must not be null,\ne.g. patch author version", metaVar = "<attr1> [<attr2>] ... [n]")
	private List<String> mandatoryAttributes = new ArrayList<String>();

	@Option(name = "--no-logging", usage = "disable writing a logfile")
	private boolean noLogging = false;

	@Option(name = "--source-list-file", usage = "source file name, e.g. SourceFilesReference.conf", metaVar = "<file>", depends = {
			"--source-dir" })
	private String patchFilesName;

	@Option(name = "--source-dir", usage = "path to the source directory, e.g. ../src/sql", metaVar = "<path>", depends = {
			"--source-list-file" })
	private String patchFilesSourceDir;

	@Option(name = "--silent", usage = "disable all prompts, non-interactive mode")
	private boolean isSilent = false;

	@Option(name = "--zip-include-files", handler = WellBehavedStringArrayOptionHandler.class, usage = "list of files or directories to be included into the final zip-file.", metaVar = "<file1> <file2> ...")
	private List<String> zipIncludeFiles = new ArrayList<String>();
	
	@Option(name = "--convert-files-to-utf8", usage = "all sql files are converted to utf8 before adding them to the .zip file ")
	private boolean bConvertToUTF8 = false;

	@Option(name = "--target-system", usage = "target environment system (e.g. int, prod, dev). If not specified, ALL files will be included regardless of environment directives", metaVar = "<system>")
	private String targetSystem;

	/**
	 * Main entry point to the ZipInstaller
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.debug("*** start ***");

		ZipInstallerMain installerMain = new ZipInstallerMain();

		installerMain.parseParameters(args);
		installerMain.transformParams();
		installerMain.dumpParameters();

		ZipInstaller zipInstaller = new ZipInstaller(installerMain.zipFileName, installerMain.configFileName, installerMain.mandatoryAttributes,
				installerMain.noLogging, installerMain.patchFilesName, installerMain.patchFilesSourceDir,
				installerMain.isSilent, installerMain.zipIncludeFiles, installerMain.defaultsConfigFileName, installerMain.bConvertToUTF8, installerMain.targetSystem);

		zipInstaller.zipInstallRun();

		MsgLog.println("\n*** done.");

		log.debug("*** end ***");
	}

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.installer.ZipInstallerMain [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println(
				"  Example: java de.opal.installer.ZipInstallerMain" + parser.printExample(OptionHandlerFilter.PUBLIC));
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

			if (this.showHelp) {
				showUsage(System.out, parser);
				System.exit(0);
			}

		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			showUsage(System.err, parser);

			System.exit(1);
		}
	}

	public void transformParams() {
		// trim() parameters
		this.configFileName = this.configFileName.trim();
		if (this.targetSystem != null) {
			this.targetSystem = this.targetSystem.trim();
		}
	}

	private void dumpParameters() {
		log.debug("*** Options");
		log.debug("configFileName: " + this.configFileName);
		log.debug("targetSystem: " + this.targetSystem);
	}
}