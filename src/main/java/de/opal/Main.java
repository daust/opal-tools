package de.opal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.Installer;
import de.opal.installer.util.Msg;

public class Main {

	public static final Logger log = LogManager.getLogger(Main.class.getName());

	//private static final String version = "0.5.0";

	public static void main(String[] args) throws Exception {

		log.info("*** start");

		Main main = new Main();
		main.readConfig(args);

		log.info("*** end");

	}

	/**
	 * Read the values from the command line
	 * 
	 * @param args
	 * @throws Exception 
	 */
	private void readConfig(String[] args) throws Exception {
		
		String command = "";
		String [] parameters = null;
		
		// read command line parameters and exit if no know command found	
		for(int i = 0; i < args.length; i++) {
            log.debug(" Parameter(" + i + "):" + args[i]);
        }

		if (args.length == 0) {
			showUsage();
			return;
		} else {
			command = args[0];
			parameters = new String[args.length-1];
			// remove command from argument list
			System.arraycopy(args, 1, parameters, 0, args.length-1);
			
			log.debug("command: "+command);
			switch (command) {
			// run
			case "initPatch":
				PatchInit main = new PatchInit(parameters);
				main.run();

				break;
			case "copyPatchFiles":
				CopyPatchFiles cpf = new CopyPatchFiles(parameters);
				cpf.run();

				break;
			case "executePatch":
				Installer inst = new Installer(parameters);
				inst.run();
				
				break;
			default:
				showUsage();
			}
		}
	}

	private static void showUsage() {
		Msg.println("");
		Msg.println("Usage: ");
		Msg.println("");
		Msg.println("java -jar installer.jar <command> <parameters>");
		Msg.println("");
		Msg.println("Commands:");
		Msg.println("  initPatch <target directory> <source directory>");
		Msg.println("    // create patch directory and copy template files to patch directory");
		Msg.println("");
		Msg.println("  copyPatchFiles <target directory> <source directory> <file containing the file list>");
		Msg.println("    // copy files from source tree to patch directory");
		Msg.println("");
		Msg.println("  executePatch EXECUTE <json config file incl. path> <json config file for connection information>");
		Msg.println("    // run/install the actual patch, pausing is optional");
		Msg.println("");
		Msg.println("  executePatch VALIDATE_ONLY <json config file incl. path> <json config file for connection information>");
		Msg.println("    // run/install the actual patch in simulation mode, check all connection pools, ");
		Msg.println("    // runs without pause");
		Msg.println("");
		Msg.println("  configure");
		Msg.println("    // will configure the initial setup and create the files in the proper location.  ");
		Msg.println("    // creates connection pools and batch scripts");
		Msg.println("");
	}

}
