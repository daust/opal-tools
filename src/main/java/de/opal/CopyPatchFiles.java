package de.opal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.opal.installer.util.Msg;

public class CopyPatchFiles {
	public static final Logger log = LoggerFactory.getLogger(Main.class.getName());

	private String sourcePathName = "";
	private String targetPathName = "";
	private String patchFilesName = "";

	private String currentSourcePathName = "";
	private String currentTargetPathName = "";

	public static void main(String[] args) throws SQLException, IOException {

		log.info("*** start");
		CopyPatchFiles main = new CopyPatchFiles(args);

		main.run();
		log.info("*** end");
	}

	/**
	 * Constructor
	 * 
	 * @param args - initialize with command line parameters
	 */
	public CopyPatchFiles(String[] args) {
		readConfig(args);

		// initially the source and target path names are derived from the starting
		// point,
		// the base directories for source and target
		setRelativePaths(null, null);
	}

	public void run() throws IOException {
		Msg.println("copy files from:" + this.sourcePathName + 
				  "\n           to  :" + this.targetPathName + "\n");
		Msg.println("process patch file listing in: " + this.patchFilesName + "\n");

		// read file line by line
		try (BufferedReader br = new BufferedReader(new FileReader(this.patchFilesName))) {

			String line;
			while ((line = br.readLine()) != null) {
				processLine(line);
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

	private void processLine(String line) throws IOException {
		// is this a comment line? Then it will be skipped
		if (line.trim().startsWith("#")) {
			// skip line => comment
			return;
		}

		// is this a path mapping directive?
		// then it will change the current from and to directories
		if (line.trim().contains("=>")) {
			log.debug("line contains path mapping: " + line);

			String source = line.substring(0, line.indexOf("=>")).trim();
			String target = line.substring(line.indexOf("=>") + "=>".length() + 1).trim();

			Msg.println("Mapping: " + source + " => " + target);
			setRelativePaths(source, target);
			

			return;
		}

		// process file
		if (!line.trim().isEmpty()) {
			log.debug("process line: " + line);
			File src = new File(this.currentSourcePathName);
			File target = new File(this.currentTargetPathName);
			
			Msg.println("  " + line );

			IOFileFilter filter = new WildcardFileFilter(line);
			FileUtils.copyDirectory(src, target, filter);
		}
	}

	/**
	 * Read the values from the command line
	 * 
	 * @param args
	 */
	private void readConfig(String[] args) {
		// read command line parameters and exit if no know command found
		if (args.length != 3) {
			showUsage();

			System.exit(1);
		} else {
			this.targetPathName = args[0];
			this.sourcePathName = args[1];
			this.patchFilesName = args[2];
		}

	}

	private static void showUsage() {
		Msg.println("");
		Msg.println("Copies files from source tree to the patch directory.");
		Msg.println("");
		Msg.println("Usage: ");
		Msg.println("");
		Msg.println(
				"java -jar installer.jar copyPatchFiles <target directory> <source directory> <file containing the file list>");
		Msg.println("");
		Msg.println("Sample");
		Msg.println(
				"  java -jar installer.jar copyPatchFiles <target directory> <source directory> <file containing the file list>");
		Msg.println("");

	}

}
