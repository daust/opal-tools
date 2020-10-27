package de.opal.installer.util;

import java.io.File;

public class FileNode {
	
	public FileNode(File file) {
		super();
		this.file = file;
	}

	private File file;

	/**
	 * @return the file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @param file the file to set
	 */
	public void setFile(File file) {
		this.file = file;
	}

}
