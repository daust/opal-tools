package de.opal.installer;

import java.io.File;
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
import de.opal.installer.config.ConfigManagerConnectionPool;
import de.opal.utils.MsgLog;

public class InstallerMain {

	/*--------------------------------------------------------------------------------------
	 * Other variables
	 */
	public static final Logger log = LogManager.getLogger(InstallerMain.class.getName());
	private String userIdentity;

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "--connection-pool-file", usage = "connection pool file\ne.g.: connections-dev.json", metaVar = "<file>", required = true)
	private String connectionPoolFile;

	@Option(name = "--config-file", usage = "configuration file\ne.g.: opal-installer.json", metaVar = "<file>", required = true)
	private String configFileName;

	@Option(name = "--validate-only", usage = "don't execute patch, just validate the files and connection pools")
	private boolean validateOnly = false;

	@Option(name = "--mandatory-attributes", handler = WellBehavedStringArrayOptionHandler.class, usage = "list of attributes that must not be null,\ne.g. patch author version", metaVar = "<attr1> [<attr2>] ... [n]")
	private List<String> mandatoryAttributes = new ArrayList<String>();

	@Option(name = "--no-logging", usage = "disable writing a logfile")
	private boolean noLogging = false;

	@Option(name = "--source-list-file", usage = "source file name, e.g. SourceFilesReference.conf", metaVar = "<file>", depends = {"--source-dir"})
	private String patchFilesName;
	
	@Option(name = "--source-dir", usage = "path to the source directory, e.g. ../src/sql", metaVar = "<path>", depends = {"--source-list-file"})
	private String patchFilesSourceDir;
	
	@Option(name = "--silent", usage = "disable all prompts, non-interactive mode")
	private boolean isSilent = false;

	@Option(name = "--silent-execution", usage = "prompt after header information, execute all scripts without prompt.")
	private boolean isSilentExecution = false;


	/**
	 * Main entry point to the Installer
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.debug("*** start ***");

		InstallerMain installerMain = new InstallerMain();

		installerMain.parseParameters(args);
		installerMain.transformParams();
		installerMain.dumpParameters();

		Installer installer = new Installer(installerMain.validateOnly, installerMain.configFileName,
				installerMain.connectionPoolFile, installerMain.userIdentity, installerMain.mandatoryAttributes,
				installerMain.noLogging, installerMain.patchFilesName, installerMain.patchFilesSourceDir, installerMain.isSilent, installerMain.isSilentExecution);
		installer.run();

		MsgLog.println("\n*** done.");

		log.debug("*** end ***");
	}

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.installer.InstallerMain [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println(
				"  Example: java de.opal.installer.InstallerMain" + parser.printExample(OptionHandlerFilter.PUBLIC));
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

			// check whether jdbcURL OR connection pool is specified correctly
			if (this.showHelp) {
				showUsage(System.out, parser);
				System.exit(0);
			}

			// check conn pool file
			// encrypt if it contains unencrypted passwords
			if (!new File(this.connectionPoolFile).exists()) {
				throw new CmdLineException(parser, "connection pool file " + this.connectionPoolFile + " not found",
					    (Throwable) null);
			}

			ConfigManagerConnectionPool configManagerConnectionPools = new ConfigManagerConnectionPool(this.connectionPoolFile);

			// encrypt passwords if required
			if (configManagerConnectionPools.hasUnencryptedPasswords()) {
				configManagerConnectionPools.encryptPasswords(
						configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFile));
				// dump JSON file
				configManagerConnectionPools.writeJSONConf();
			}
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			showUsage(System.err, parser);

			System.exit(1);
		}
	}

	public void transformParams() {
		// trim() parameters
		this.connectionPoolFile = this.connectionPoolFile.trim();
		this.configFileName = this.configFileName.trim();

		this.userIdentity = System.getenv("OPAL_TOOLS_USER_IDENTITY");
		if (this.userIdentity == null || this.userIdentity.isEmpty()) {
			this.userIdentity = System.getProperty("user.name");
		}
	}

	private void dumpParameters() {
		log.debug("*** Options");
		log.debug("connectionPoolFile: " + this.connectionPoolFile);
		log.debug("configFileName: " + this.configFileName);
	}
}
