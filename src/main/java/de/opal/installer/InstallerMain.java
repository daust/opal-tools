package de.opal.installer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import de.opal.installer.config.ConfigManager;
import de.opal.installer.util.Msg;

public class InstallerMain {

	/*--------------------------------------------------------------------------------------
	 * Other variables
	 */
	public static final Logger log = LogManager.getLogger(InstallerMain.class.getName());
	private String version; // will be loaded from file version.txt which will be populated by the gradle
							// build process

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "--connection-pool-file", usage = "connection pool file\ne.g.: connections-dev.json", metaVar = "<file>", required=true)
	private String connectionPoolFile;

	@Option(name = "--config-file", usage = "configuration file\ne.g.: opal-installer.json", metaVar = "<file>", required=true)
	private String configFileName;

	@Option(name = "--validate-only", usage = "don't execute patch, just validate the files and connection pools")
	private boolean validateOnly = false;

	/**
	 * readVersionFromFile
	 * 
	 * Read version from file version.properties in same package
	 */
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
	 * Main entry point to the Installer
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		log.debug("*** start ***");

		InstallerMain installerMain = new InstallerMain();

		installerMain.readVersionFromFile();
		installerMain.parseParameters(args);
		installerMain.transformParams();
		installerMain.dumpParameters();

		Installer installer = new Installer(installerMain.validateOnly, installerMain.configFileName,
				installerMain.connectionPoolFile);
		installer.run();

		Msg.println("\n*** done.");

		log.debug("*** end ***");
	}

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.installer.InstallerMain [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println("  Example: java de.opal.installer.InstallerMain" + parser.printExample(OptionHandlerFilter.PUBLIC));
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
				throw new CmdLineException(parser, "connection pool file " + this.connectionPoolFile + " not found");
			}

			ConfigManager configManagerConnectionPools = new ConfigManager(this.connectionPoolFile);

			// encrypt passwords if required
			if (configManagerConnectionPools.hasUnencryptedPasswords()) {
				configManagerConnectionPools.encryptPasswords(
						configManagerConnectionPools.getEncryptionKeyFilename(this.connectionPoolFile));
				// dump JSON file
				configManagerConnectionPools.writeJSONConfPool();
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
	}

	private void dumpParameters() {
		log.debug("*** Options");
		log.debug("connectionPoolFile: " + this.connectionPoolFile);
		log.debug("configFileName: " + this.configFileName);
	}
}
