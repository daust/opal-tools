package de.opal.installer.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Logfile {

	private static Logfile _instance;
	private File file;
	private FileWriter fw;
	private BufferedWriter bw;
	public static final Logger logger = LogManager.getLogger(Logfile.class.getName());

	private Logfile() {
		// TODO: put your code here
	}

	/**
	 * returns Singleton Instance
	 * 
	 * @return LogFile
	 */
	public static synchronized Logfile getInstance() {
		if (_instance == null)
			_instance = new Logfile();
		return _instance;
	}

	public void open(String fileName) throws IOException {
		
		logger.debug("open logfile for " + fileName);
		// create file object
		if (this.file == null) {
			file = new File(fileName);
		}

		// if file doesn't exists, then create it
		if (!this.file.exists()) {
			this.file.createNewFile();
		}
		this.fw = new FileWriter(file.getAbsoluteFile());
		this.bw = new BufferedWriter(fw);

	}

	public void append(String msg) throws IOException {
		this.bw.write(msg);
	}
	public void appendln(String msg) throws IOException {
		this.bw.write(msg+"\n");
	}

	public void close() throws IOException {
		this.bw.close();
	}

}
