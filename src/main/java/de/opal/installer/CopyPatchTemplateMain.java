package de.opal.installer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import de.opal.installer.config.ConfigManager;
import de.opal.installer.util.Msg;

public class CopyPatchTemplateMain {
	
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

	@Option(name = "--source-path", usage = "path to the template directory structure", metaVar = "<path>", required=true)
	private String sourcePathName;

	@Option(name = "--target-path", usage = "target path for the patch", metaVar = "<path>", required=true)
	private String targetPathName;

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

	public CopyPatchTemplateMain() {
	
	}
	
	public static void main(String[] args) throws SQLException, IOException {
		log.info("*** start ***");

		CopyPatchTemplateMain main = new CopyPatchTemplateMain();
		
		main.readVersionFromFile();
		main.parseParameters(args);
		main.transformParams();
		main.dumpParameters();
		main.run();

		log.info("*** end");
	}

		
	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.installer.CopyPatchTemplate [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println("  Example: java de.opal.installer.CopyPatchTemplate" + parser.printExample(OptionHandlerFilter.PUBLIC));
	}

	/**
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
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			showUsage(System.err, parser);

			System.exit(1);
		}
	}

	public void transformParams() {
		// trim() parameters
		this.sourcePathName = this.sourcePathName.trim();
		this.targetPathName = this.targetPathName.trim();
	}

	private void dumpParameters() {
		log.debug("*** Options");
		log.debug("sourcePathName: " + this.sourcePathName);
		log.debug("targetPathName: " + this.targetPathName);
	}	
	
	
	public void run() throws IOException {
		log.info("START, copy files from " + this.sourcePathName + " to " + this.targetPathName);
		Msg.println("copy files from:" + this.sourcePathName + 
				  "\n           to  :" + this.targetPathName + "\n");
		
		FileUtils.copyDirectory(new File(this.sourcePathName), new File(this.targetPathName));
		log.info("END run()");
	}


}
