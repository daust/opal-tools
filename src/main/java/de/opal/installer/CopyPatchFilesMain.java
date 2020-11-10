package de.opal.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.opal.installer.util.Msg;

public class CopyPatchFilesMain {
	public static final Logger log = LoggerFactory.getLogger(CopyPatchFilesMain.class.getName());

	private String currentSourcePathName = "";
	private String currentTargetPathName = "";
	
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

	@Option(name = "--patch-file-name", usage = "target path for the patch", metaVar = "<path>", required=true)
	private String patchFilesName;

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
	
	public CopyPatchFilesMain() {
		
	}

	public static void main(String[] args) throws SQLException, IOException {

		log.info("*** start");
		CopyPatchFilesMain main = new CopyPatchFilesMain();
		
		main.readVersionFromFile();
		main.parseParameters(args);
		main.transformParams();
		main.dumpParameters();
		// initially the source and target path names are derived from the starting
		// point,
		// the base directories for source and target
		main.setRelativePaths(null, null);

		main.run();
		log.info("*** end");
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

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.installer.CopyPatchFiles [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println("  Example: java de.opal.installer.CopyPatchFiles" + parser.printExample(OptionHandlerFilter.PUBLIC));
	}

	
	public void run() throws IOException {
		Msg.println("copy files from:" + this.sourcePathName + 
				  "\n           to  :" + this.targetPathName + "\n");
		Msg.println("process patch file listing in: " + this.patchFilesName + "\n");

		// read file line by line
		try (BufferedReader br = new BufferedReader(new FileReader(this.patchFilesName))) {

			String line;
			while ((line = br.readLine()) != null) {
				processLine(line.trim());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		Msg.println("");
		log.info("END run()");
	}

	private void setRelativePaths(String source, String target) {
		if (source != null) {
			this.currentSourcePathName = this.sourcePathName + File.separator + source;
		} else {
			this.currentSourcePathName = this.sourcePathName;
		}
		if (target != null) {
			this.currentTargetPathName = this.targetPathName + File.separator + target;
		} else {
			this.currentTargetPathName = this.targetPathName;
		}

		log.debug("*** New Source Path: #" + source + "#" + " - " + this.currentSourcePathName);
		log.debug("*** New Target Path: #" + target + "#" + " - " + this.currentTargetPathName);

	}

	/**
	 * 
	 * @param line (should already be trimmed()
	 * @throws IOException
	 */
	private void processLine(String line) throws IOException {
		// is this a comment line? Then it will be skipped
		if (line.startsWith("#")) {
			// skip line => comment
			return;
		}

		// is this a path mapping directive?
		// then it will change the current from and to directories
		if (line.contains("=>")) {
			log.debug("line contains path mapping: " + line);

			String source = line.substring(0, line.indexOf("=>")).trim();
			String target = line.substring(line.indexOf("=>") + "=>".length() + 1).trim();

			Msg.println("Mapping: " + source + " => " + target);
			setRelativePaths(source, target);
			
			return;
		}

		// process file
		if (!line.isEmpty()) {
			log.debug("process line: " + line);
			File src = new File(this.currentSourcePathName);
			File target = new File(this.currentTargetPathName);
			
			Msg.println("  " + line );

			IOFileFilter filter = new WildcardFileFilter(line);
			FileUtils.copyDirectory(src, target, filter);
		}
	}
}
