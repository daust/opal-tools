package de.opal.utils;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.Installer;
import de.opal.installer.util.Logfile;

/**
 * Logger for the console and/or file
 * 
 * @author daust
 *
 */
public class MsgLog {
	public static final Logger log = LogManager.getLogger(MsgLog.class.getName());
	private static MsgLog instance;
	private static Logfile logfile;

	private MsgLog() {
	}

	/**
	 * returns Singleton Instance
	 * 
	 * @return Msg
	 */
	public static synchronized MsgLog getInstance() {
		if (instance == null)
			instance = new MsgLog();
		return instance;
	}

	/**
	 * print on console and into logfile
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public static void println(String msg) throws IOException {
		System.out.println(msg);
		if (logfile != null) {
			logfile.appendln(msg);
		}
	}

	/**
	 * print on console and into logfile
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public static void print(String msg) throws IOException {
		System.out.print(msg);
		if (logfile != null) {
			logfile.append(msg);
		}
	}

	/**
	 * println on console only
	 * 
	 * @param msg
	 */
	public static void consolePrintln(String msg) {
		System.out.println(msg);
	}

	/**
	 * print on console only
	 * 
	 * @param msg
	 */
	public static void consolePrint(String msg) {
		System.out.print(msg);
	}

	/**
	 * println in logfile only
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public static void logfilePrintln(String msg) throws IOException {
		logfile.appendln(msg);
	}

	/**
	 * println in logfile only
	 * 
	 * @param msg
	 * @throws IOException
	 */
	public static void logfilePrint(String msg) throws IOException {
		logfile.append(msg);
	}

	/**
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public static void createLogfile(String filename) throws IOException {
		if (logfile != null) {
			throw new RuntimeException("Logfile already opened, only one is allowed.");
		}
		logfile = Logfile.getInstance();
		logfile.open(filename);
	}

	/**
	 * 
	 * @param filename
	 * @throws IOException
	 */
	public static void createLogDirectory(String directoryName) throws IOException {
		// create logfile directory if it does not exist
		File logFileDirFile = new File(directoryName);
		if (!logFileDirFile.exists()) {
			logFileDirFile.mkdir();
		}
	}

	/**
	 * 
	 * @throws IOException
	 */
	public static void closeLogfile() throws IOException {
		if (logfile != null)
			logfile.close();
	}
}
