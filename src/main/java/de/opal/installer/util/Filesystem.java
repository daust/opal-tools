package de.opal.installer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.config.ConfigManager;
import de.opal.utils.MsgLog;

public class Filesystem {

	/**
	 * Fields
	 */
	private String baseDirName;
	// private File baseDir;

	public static final Logger log = LogManager.getLogger(Filesystem.class.getName());

//	private List<FileNode> fileList = new ArrayList<FileNode>();

	/**
	 * Constructor
	 * 
	 * @param baseDirName
	 */
	public Filesystem(String baseDirName) {
		super();
		this.baseDirName = baseDirName;
//		this.baseDir = new File(baseDirName);
	}

	public Filesystem(File baseDir) {
		super();
		this.baseDirName = baseDir.toString();
//		this.baseDir = baseDir;
	}

	/**
	 * scanTree() - load tree into memory
	 * 
	 */
	public List<FileNode> scanTree() {

		List<FileNode> fileList = new ArrayList<FileNode>();

		log.debug("\n*** Scan Tree: directory: " + baseDirName);
		// MsgLog.println("\ntraversalType: " + traversalType.toString());

		Path start = FileSystems.getDefault().getPath(this.baseDirName);
		try {
			// Files.walk(start).filter(path -> path.toFile().isFile()).filter(path ->
			// path.toString().endsWith(".sql"))
			Files.walk(start).sorted().filter(path -> path.toFile().isFile()).forEach(path -> {
				log.debug(path.toString());
				fileList.add(new FileNode(path.toFile()));
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileList;
	}

	/**
	 * 
	 * 
	 * @param srcFileList
	 * @return
	 * @throws IOException
	 */
	public List<FileNode> filterTreeInorder(List<FileNode> srcFileList, String sqlFileRegEx, Logfile logfile,
			ConfigManager configManager) throws IOException {

		List<FileNode> fileList = new ArrayList<FileNode>();

		log.debug("\n*** filterTreeInorder ");
		log.debug("\nregex: " + sqlFileRegEx);

		// MsgLog.print("*** List of files to be installed:\n\n");

		Pattern p = Pattern.compile(sqlFileRegEx);

		for (FileNode fileNode : srcFileList) {
			String baseDirectoryName=configManager.getPackageDirName();
			String relativeFilename = fileNode.getFile().getAbsolutePath()
					.replace(baseDirectoryName, "");

			if (p.matcher(relativeFilename).find()) {
				String overrideEncoding = configManager.getEncoding(relativeFilename);
				if (overrideEncoding.isEmpty()) {
					MsgLog.println("file: (system encoding) - " + relativeFilename);
				} else {
					MsgLog.println(
							"file: (override encoding: " + overrideEncoding + ") - " + relativeFilename);
				}

				//MsgLog.println("file: " + fileNode.getFile().toString());
				fileList.add(fileNode);
			}
		}

		return fileList;
	}

	/**
	 * 
	 * 
	 * @param srcFileList
	 * @return
	 * @throws IOException 
	 */
	public List<FileNode> filterTreeStaticFiles(List<FileNode> srcFileList, ArrayList<String> staticFiles) throws IOException {

		List<FileNode> fileList = new ArrayList<FileNode>();

		log.debug("\n*** filterTreeStaticFiles ");
		log.debug("\nstaticFiles: " + staticFiles.toString());

		for (String staticFile : staticFiles) {
			for (FileNode fileNode : srcFileList) {

				if (fileNode.getFile().getName().equals(staticFile)) {
					MsgLog.println("file: " + fileNode.getFile().toString());
					fileList.add(fileNode);
				}
			}

		}

		return fileList;
	}

	/**
	 * Getter / Setter
	 * 
	 * @return
	 */

	public String getBaseDir() {
		return baseDirName;
	}

	public void setBaseDir(String baseDir) {
		this.baseDirName = baseDir;
	}

}
