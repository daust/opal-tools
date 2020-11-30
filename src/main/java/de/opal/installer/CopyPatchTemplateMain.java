package de.opal.installer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import de.opal.installer.util.Msg;
import de.opal.utils.VersionInfo;

public class CopyPatchTemplateMain {
	
	/*--------------------------------------------------------------------------------------
	 * Other variables
	 */
	public static final Logger log = LogManager.getLogger(InstallerMain.class.getName());

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "-v", aliases = "--version", usage = "show version information", help = true)
	private boolean showVersion;

	@Option(name = "--source-dir", usage = "template directory", metaVar = "<directory>", required=true)
	private String sourcePathName;

	@Option(name = "--target-dir", usage = "target directory for the patch", metaVar = "<directory>", required=true)
	private String targetPathName;

	public CopyPatchTemplateMain() {
	
	}
	
	public static void main(String[] args) throws SQLException, IOException {
		log.info("*** start ***");

		CopyPatchTemplateMain main = new CopyPatchTemplateMain();
		
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
			
			if (this.showVersion) {
				VersionInfo.showVersionInfo(this.getClass(), "OPAL Installer", true);
			}

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
