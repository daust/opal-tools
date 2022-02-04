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

import de.opal.installer.PatchFileMapping;
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
	 * only the files, not the directories
	 * 
	 */
	public List<PatchFileMapping> scanTree() {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** Scan Tree: directory: " + baseDirName);
		// MsgLog.println("\ntraversalType: " + traversalType.toString());

		Path start = FileSystems.getDefault().getPath(this.baseDirName);
		try {
			Files.walk(start).sorted().filter(path -> path.toFile().isFile()).forEach(path -> {
				log.debug(path.toString());
				fileList.add(new PatchFileMapping(null, path.toFile()));
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileList;
	}

	/**
	 * Return files and directories
	 * @return
	 */
	public List<PatchFileMapping> scanTreeFilesAndDirectories() {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** Scan Tree: directory: " + baseDirName);
		// MsgLog.println("\ntraversalType: " + traversalType.toString());

		Path start = FileSystems.getDefault().getPath(this.baseDirName);
		try {
			Files.walk(start).sorted().forEach(path -> {
				log.debug(path.toString());
				fileList.add(new PatchFileMapping(null, path.toFile()));
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
	public List<PatchFileMapping> filterTreeInorder(List<PatchFileMapping> srcFileList, String sqlFileRegex, ConfigManager configManager) throws IOException {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** filterTreeInorder ");
		log.debug("\nregex: " + sqlFileRegex);

		// MsgLog.print("*** List of files to be installed:\n\n");

		Pattern p = Pattern.compile(sqlFileRegex, Pattern.CASE_INSENSITIVE);

		for (PatchFileMapping fileMapping : srcFileList) {
			String relativeFilename = configManager.getRelativeFilename(fileMapping.destFile.getAbsolutePath());

			if (p.matcher(relativeFilename).find()) {
				//String overrideEncoding = configManager.getEncoding(relativeFilename);
				/*
				 * if (overrideEncoding.isEmpty()) { MsgLog.println("file: (system encoding) - "
				 * + relativeFilename); } else { MsgLog.println("file: (override encoding: " +
				 * overrideEncoding + ") - " + relativeFilename); }
				 */
				// MsgLog.println("file: " + fileNode.getFile().toString());
				fileList.add(fileMapping);
			}
		}

		return fileList;
	}

	// display empty files at the end
	public void displayTree(List<PatchFileMapping> srcFileList, String sqlFileRegex, ConfigManager configManager)
			throws IOException {

		ArrayList<String> emptyFiles=new ArrayList<String>();
		
		log.debug("\n*** displayTree ");
		log.debug("\nregex: " + sqlFileRegex);

		for (PatchFileMapping fileMapping : srcFileList) {
			String relativeFilename = configManager.getRelativeFilename(fileMapping.destFile.getAbsolutePath());

			String referenceFileString="";
			if (fileMapping.srcFile !=null)
				referenceFileString = " (=> Ref: " + fileMapping.srcFile.getPath() + ")";
			
			String overrideEncoding = configManager.getEncoding(relativeFilename);
			if (overrideEncoding.isEmpty()) {
				MsgLog.println("   file: (system encoding) - " + relativeFilename + referenceFileString);
			} else {
				MsgLog.println("   file: (override encoding: " + overrideEncoding + ") - " + relativeFilename + referenceFileString);
			}
						
			// check for empty files
			if (fileMapping.srcFile != null) {
				if (Files.size(fileMapping.srcFile.toPath())==0){
					emptyFiles.add(relativeFilename);
				}
			} else {
				if (Files.size(fileMapping.destFile.toPath())==0){
					emptyFiles.add(relativeFilename);
				}
			}
		}
		
		// display empty files if any exist
		if (emptyFiles.size()>0) {
			MsgLog.println("\n!!! WARNING: The following files are empty, please check: ");
			for (int i = 0; i < emptyFiles.size(); i++) {
				MsgLog.println("   " + emptyFiles.get(i));
			}			
		}
	}

	
	/**
	 * 
	 * 
	 * @param srcFileList
	 * @return
	 * @throws IOException
	 */
	public List<PatchFileMapping> filterTreeStaticFiles(List<PatchFileMapping> srcFileList,
			ArrayList<String> staticFiles) throws IOException {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** filterTreeStaticFiles ");
		log.debug("\nstaticFiles: " + staticFiles.toString());

		for (String staticFile : staticFiles) {
			for (PatchFileMapping fileMapping : srcFileList) {

				if (fileMapping.destFile.getName().equals(staticFile)) {
					MsgLog.println("file: " + fileMapping.destFile.toString());
					fileList.add(fileMapping);
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
