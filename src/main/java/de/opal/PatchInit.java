package de.opal;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.util.Msg;

public class PatchInit {
	public static final Logger log = LogManager.getLogger(PatchInit.class.getName());

	private String sourcePathName = "";
	private String targetPathName = "";

	public static void main(String[] args) throws SQLException, IOException {

		log.info("*** start");
		PatchInit main = new PatchInit(args);

		main.run();
		log.info("*** end");
	}

	/**
	 * Constructor
	 * 
	 * @param args - initialize with command line parameters
	 */
	public PatchInit(String[] args) {
		readConfig(args);
	}

	public void run() throws IOException {
		log.info("START, copy files from " + this.sourcePathName + " to " + this.targetPathName);
		Msg.println("copy files from:" + this.sourcePathName + 
				  "\n           to  :" + this.targetPathName + "\n");
		
		FileUtils.copyDirectory(new File(this.sourcePathName), new File(this.targetPathName));
		log.info("END run()");
	}

	/**
	 * Read the values from the command line
	 * 
	 * @param args
	 */
	private void readConfig(String[] args) {
		for(int i = 0; i < args.length; i++) {
            log.debug(" Parameter(" + i + "):" + args[i]);
        }

		// read command line parameters and exit if no know command found
		if (args.length != 2) {
			showUsage();

			System.exit(1);
		} else {
			this.targetPathName = args[0];
			this.sourcePathName = args[1];
		}
	}

	private static void showUsage() {
		Msg.println("");
		Msg.println("Creates a patch directory and copies template files to the patch directory.");
		Msg.println("");
		Msg.println("Usage: ");
		Msg.println("");
		Msg.println("java -jar installer.jar initPatch <target directory> <source directory>");
		Msg.println("");
		Msg.println("Sample");
		Msg.println("  java -jar installer.jar initPatch <target directory> <source directory>");
		Msg.println("");
	}

}
