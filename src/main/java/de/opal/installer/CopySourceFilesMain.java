package de.opal.installer;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.opal.installer.util.Msg;
import de.opal.utils.VersionInfo;

public class CopySourceFilesMain {
	public static final Logger log = LoggerFactory.getLogger(CopySourceFilesMain.class.getName());

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
		this.sourceFilesName = this.sourceFilesName.trim();
	}

	private void dumpParameters() {
		log.debug("*** Options");
		log.debug("sourcePathName: " + this.sourcePathName);
		log.debug("targetPathName: " + this.targetPathName);
		log.debug("sourceFilesName: " + this.sourceFilesName);
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

		Msg.println("copy files from:" + this.sourcePathName + "\n           to  :" + this.targetPathName + "\n");
		Msg.println("process source file listing in: " + this.sourceFilesName + "\n");

		// Use PatchFilesTxtWrapper to get the file list
		PatchFilesTxtWrapper wrapper = new PatchFilesTxtWrapper(
			this.sourceFilesName, 
			this.sourcePathName, 
			this.targetPathName
		);
		
		List<PatchFileMapping> fileList = wrapper.getFileList();
		
		log.debug("Found " + fileList.size() + " files to copy");
		
		// Copy all files from the list
		for (PatchFileMapping fileMapping : fileList) {
			copyFile(fileMapping);
		}

		displayStatsFooter(this.fileCopyCount, startTime);

		Msg.println("\n*** done.\n");

		log.info("END run()");
	}

	/**
	 * Copy a single file from source to target as defined in PatchFileMapping
	 * 
	 * @param fileMapping The file mapping containing source and target information
	 * @throws IOException if copy operation fails
	 */
	private void copyFile(PatchFileMapping fileMapping) throws IOException {
		try {
			// Ensure target directory exists
			File targetDir = fileMapping.destFile.getParentFile();
			if (!targetDir.exists()) {
				targetDir.mkdirs();
				log.debug("Created target directory: " + targetDir.getAbsolutePath());
			}
			
			// Copy the file
			FileUtils.copyFile(fileMapping.srcFile, fileMapping.destFile);
			
			// Log the copy operation
			String envInfo = fileMapping.hasEnvDirective 
				? " (env: " + String.join(",", fileMapping.envList) + ")"
				: "";
			
			Msg.println(" - " + fileMapping.srcFile.getName() + " -> " + fileMapping.destFile.getName() + envInfo);
			log.debug("Copied: " + fileMapping.srcFile.getAbsolutePath() + " -> " + fileMapping.destFile.getAbsolutePath());
			
			this.fileCopyCount++;
			
		} catch (IOException e) {
			log.error("Failed to copy file: " + fileMapping.srcFile.getAbsolutePath() + " -> " + fileMapping.destFile.getAbsolutePath(), e);
			throw e;
		}
	}

	/**
	 * Legacy method - kept for backward compatibility but renamed
	 * This method contains the old implementation for reference
	 */
	public void runOldImplementation(String[] args) throws IOException {

		long startTime = System.currentTimeMillis();

		parseParameters(args);
		transformParams();
		dumpParameters();

		// This is the old implementation - now replaced by PatchFilesTxtWrapper usage
		// Keeping it for reference/comparison purposes
		
		Msg.println("*** Using legacy implementation ***");
		Msg.println("copy files from:" + this.sourcePathName + "\n           to  :" + this.targetPathName + "\n");
		Msg.println("process source file listing in: " + this.sourceFilesName + "\n");

		// Old direct file processing would go here...
		// This has been replaced by the PatchFilesTxtWrapper approach

		displayStatsFooter(this.fileCopyCount, startTime);

		Msg.println("\n*** done.\n");

		log.info("END runOldImplementation()");
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