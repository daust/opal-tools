package de.opal.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.opal.installer.util.Msg;
import de.opal.utils.OpalFileUtils;
import de.opal.utils.VersionInfo;

public class CopySourceFilesMain {
	public static final Logger log = LoggerFactory.getLogger(CopySourceFilesMain.class.getName());

	private String currentSourcePathName = "";
	private String currentTargetPathName = "";

	// build process
	private int fileCopyCount = 0;

	/*--------------------------------------------------------------------------------------
	 * Command line parameters
	 * - https://github.com/kohsuke/args4j
	 * - https://args4j.kohsuke.org/args4j/apidocs 
	 */

	@Option(name = "-h", aliases = "--help", usage = "show this help page", help = true)
	private boolean showHelp;

	@Option(name = "-v", aliases = "--version", usage = "show version information", help = true)
	private boolean showVersion;

	@Option(name = "--source-dir", usage = "source directory, e.g. ../src/sql", metaVar = "<directory>", required = true)
	private String sourcePathName;

	@Option(name = "--source-list-file", usage = "file that contains the sources to be copied, e.g. SourceFilesCopy.conf", metaVar = "<file>", required = true)
	private String sourceFilesName;

	@Option(name = "--target-dir", usage = "target directory, e.g. ./sql", metaVar = "<directory>", required = true)
	private String targetPathName;


	public CopySourceFilesMain() {

	}

	public static void main(String[] args) throws SQLException, IOException {

		log.info("*** start");
		CopySourceFilesMain main = new CopySourceFilesMain();
		main.run(args);

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

	private void showUsage(PrintStream out, CmdLineParser parser) {
		out.println("\njava de.opal.installer.CopySourceFiles [options...]");

		// print the list of available options
		parser.printUsage(out);

		out.println();

		// print option sample. This is useful some time
		out.println(
				"  Example: java de.opal.installer.CopySourceFiles" + parser.printExample(OptionHandlerFilter.PUBLIC));
	}

	public void run(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();

		parseParameters(args);
		transformParams();
		dumpParameters();

		// initially the source and target path names are derived from the starting
		// point,
		// the base directories for source and target
		setRelativePaths(null, null);

		Msg.println("copy files from:" + this.sourcePathName + "\n           to  :" + this.targetPathName + "\n");
		Msg.println("process source file listing in: " + this.sourceFilesName + "\n");

		// read file line by line
		try (BufferedReader br = new BufferedReader(new FileReader(this.sourceFilesName))) {

			String line;
			while ((line = br.readLine()) != null) {
				processLine(line.trim());
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		displayStatsFooter(this.fileCopyCount, startTime);

		Msg.println("\n*** done.\n");

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
	 * @param line (should already be trimmed())
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
			Msg.println("  process directive: " + line);
			// copy files and return number of files copied
			this.fileCopyCount += OpalFileUtils.copyDirectory(this.currentSourcePathName, this.currentTargetPathName,
					line);
		}
	}

	private void displayStatsFooter(int totalObjectCnt, long startTime) {
		long finish = System.currentTimeMillis();
		long timeElapsed = finish - startTime;
		int minutes = (int) (timeElapsed / (60 * 1000));
		int seconds = (int) ((timeElapsed / 1000) % 60);
		String timeElapsedString = String.format("%d:%02d", minutes, seconds);

		Msg.println("\n*** The script copied " + totalObjectCnt + " files in " + timeElapsedString + " [mm:ss].");

	}

}
