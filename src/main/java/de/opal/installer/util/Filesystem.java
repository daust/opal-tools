package de.opal.installer.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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

	public static final Logger log = LogManager.getLogger(Filesystem.class.getName());

	/**
	 * Constructor
	 * 
	 * @param baseDirName
	 */
	public Filesystem(String baseDirName) {
		super();
		this.baseDirName = baseDirName;
	}

	public Filesystem(File baseDir) {
		super();
		this.baseDirName = baseDir.toString();
	}

	/**
	 * scanTree() - load tree into memory only the files, not the directories Parses
	 * environment directives from existing filenames
	 */
	public List<PatchFileMapping> scanTree() {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** Scan Tree: directory: " + baseDirName);

		Path start = FileSystems.getDefault().getPath(this.baseDirName);
		try {
			Files.walk(start).sorted().filter(path -> path.toFile().isFile()).forEach(path -> {
				log.debug(path.toString());

				// Parse environment directive from filename if present
				String filename = path.getFileName().toString();
				ArrayList<String> envList = EnvironmentUtils.extractEnvironmentFromFilename(filename);
				Boolean hasEnvDirective = !envList.isEmpty();

				fileList.add(new PatchFileMapping(null, path.toFile(), hasEnvDirective, envList));
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileList;
	}

	/**
	 * Return files and directories Parses environment directives from existing
	 * filenames
	 */
	public List<PatchFileMapping> scanTreeFilesAndDirectories() {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** Scan Tree: directory: " + baseDirName);

		Path start = FileSystems.getDefault().getPath(this.baseDirName);
		try {
			Files.walk(start).sorted().forEach(path -> {
				log.debug(path.toString());

				// Parse environment directive from filename if present
				String filename = path.getFileName().toString();
				ArrayList<String> envList = EnvironmentUtils.extractEnvironmentFromFilename(filename);
				Boolean hasEnvDirective = !envList.isEmpty();

				fileList.add(new PatchFileMapping(null, path.toFile(), hasEnvDirective, envList));
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileList;
	}

	/**
	 * Filter files based on regex pattern and target environment
	 */
	public List<PatchFileMapping> filterTreeInorder(List<PatchFileMapping> srcFileList, String sqlFileRegex,
			ConfigManager configManager, String targetSystem) throws IOException {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** filterTreeInorder ");
		log.debug("\nregex: " + sqlFileRegex);
		log.debug("\ntargetSystem: " + targetSystem);

		Pattern p = Pattern.compile(sqlFileRegex, Pattern.CASE_INSENSITIVE);

		for (PatchFileMapping fileMapping : srcFileList) {
			String relativeFilename = configManager.getRelativeFilename(fileMapping.destFile.getAbsolutePath());

			if (p.matcher(relativeFilename).find()) {
				// Different logic based on whether targetSystem is specified
				if (targetSystem == null) {
					// When no target system, only include files without environment restrictions
					if (!hasEnvironmentRestrictions(relativeFilename)) {
						fileList.add(fileMapping);
						log.debug("Including file without env restrictions: " + relativeFilename);
					} else {
						MsgLog.println("Excluding file with env restrictions: " + relativeFilename);
					}
				} else {
					// When target system is specified, check if file should be included - log
					// skipped files
					if (EnvironmentUtils.shouldIncludeFileForTarget(relativeFilename, targetSystem)) {
						fileList.add(fileMapping);
						log.debug("Including file: " + relativeFilename);
					}
					// Note: skipped files are already logged by EnvironmentUtils with
					// logSkipped=true
				}
			}
		}

		return fileList;
	}

	/**
	 * Filter static files based on target environment
	 */
	public List<PatchFileMapping> filterTreeStaticFiles(List<PatchFileMapping> srcFileList,
			ArrayList<String> staticFiles, String targetSystem) throws IOException {

		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();

		log.debug("\n*** filterTreeStaticFiles ");
		log.debug("\nstaticFiles: " + staticFiles.toString());
		log.debug("\ntargetSystem: " + targetSystem);

		for (String staticFile : staticFiles) {
			for (PatchFileMapping fileMapping : srcFileList) {

				if (fileMapping.destFile.getName().equals(staticFile)) {
					// Different logic based on whether targetSystem is specified
					if (targetSystem == null) {
						// When no target system, only include files without environment restrictions
						if (!hasEnvironmentRestrictions(fileMapping.destFile.getName())) {
							MsgLog.println("file: " + fileMapping.destFile.toString());
							fileList.add(fileMapping);
							log.debug("Including static file without env restrictions: "
									+ fileMapping.destFile.getName());
						} else {
							log.debug("Excluding static file with env restrictions: " + fileMapping.destFile.getName());
						}
					} else {
						// When target system is specified, check if file should be included - log
						// skipped files
						if (EnvironmentUtils.shouldIncludeFileForTarget(fileMapping.destFile.getName(), targetSystem)) {
							MsgLog.println("file: " + fileMapping.destFile.toString());
							fileList.add(fileMapping);
							log.debug("Including static file: " + fileMapping.destFile.getName());
						}
						// Note: skipped files are already logged by EnvironmentUtils with
						// logSkipped=true
					}
				}
			}
		}

		return fileList;
	}

	/**
	 * Checks if a file path contains any environment restrictions (-env(...)
	 * patterns)
	 * 
	 * @param filePath The full file path to check
	 * @return true if the path contains any environment directives, false otherwise
	 */
	private boolean hasEnvironmentRestrictions(String filePath) {
		File file = new File(filePath);

		// Check the entire path hierarchy for environment directives
		File currentPath = file;
		while (currentPath != null) {
			String pathComponent = currentPath.getName();

			// Pattern to match -env(environments) in any part of the path
			Pattern envPattern = Pattern.compile("-env\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
			if (envPattern.matcher(pathComponent).find()) {
				return true; // Found environment directive
			}

			currentPath = currentPath.getParentFile();
		}

		return false; // No environment directives found
	}

	// display empty files at the end
	public void displayTree(List<PatchFileMapping> srcFileList, String sqlFileRegex, ConfigManager configManager)
			throws IOException {

		ArrayList<String> emptyFiles = new ArrayList<String>();

		log.debug("\n*** displayTree ");
		log.debug("\nregex: " + sqlFileRegex);

		for (PatchFileMapping fileMapping : srcFileList) {
			String relativeFilename = configManager.getRelativeFilename(fileMapping.destFile.getAbsolutePath());

			String referenceFileString = "";
			if (fileMapping.srcFile != null)
				referenceFileString = " (=> Ref: " + fileMapping.srcFile.getPath() + ")";

			// Show environment information if present
			String envInfo = fileMapping.hasEnvDirective ? " (env: " + String.join(",", fileMapping.envList) + ")" : "";

			String overrideEncoding = configManager.getEncoding(relativeFilename);
			if (overrideEncoding.isEmpty()) {
				MsgLog.println("   file: (system encoding) - " + relativeFilename + referenceFileString + envInfo);
			} else {
				MsgLog.println("   file: (override encoding: " + overrideEncoding + ") - " + relativeFilename
						+ referenceFileString + envInfo);
			}

			// check for empty files
			if (fileMapping.srcFile != null) {
				if (Files.size(fileMapping.srcFile.toPath()) == 0) {
					emptyFiles.add(relativeFilename);
				}
			} else {
				if (Files.size(fileMapping.destFile.toPath()) == 0) {
					emptyFiles.add(relativeFilename);
				}
			}
		}

		// display empty files if any exist
		if (emptyFiles.size() > 0) {
			MsgLog.println("\n!!! WARNING: The following files are empty, please check: ");
			for (int i = 0; i < emptyFiles.size(); i++) {
				MsgLog.println("   " + emptyFiles.get(i));
			}
		}
	}

	/**
	 * Getter / Setter
	 */
	public String getBaseDir() {
		return baseDirName;
	}

	public void setBaseDir(String baseDir) {
		this.baseDirName = baseDir;
	}
}

