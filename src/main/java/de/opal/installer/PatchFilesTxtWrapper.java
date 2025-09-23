package de.opal.installer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.opal.installer.util.EnvironmentUtils;

public class PatchFilesTxtWrapper {

	public static final Logger log = LoggerFactory.getLogger(PatchFilesTxtWrapper.class.getName());

	private String currentSourcePathName = "";
	private String currentTargetPathName = "";
	private String patchFilesName;
	private String patchFilesSourceDir;
	private String patchFilesTargetDir;

	public PatchFilesTxtWrapper(String patchFilesName, String patchFilesSourceDir, String patchFilesTargetDir) {
		this.patchFilesName = patchFilesName;
		this.patchFilesSourceDir = patchFilesSourceDir;
		this.patchFilesTargetDir = patchFilesTargetDir;
	}

	private void setRelativePaths(String source, String target) {
		if (source != null) {
			this.currentSourcePathName = this.patchFilesSourceDir + File.separator + source;
		} else {
			this.currentSourcePathName = this.patchFilesSourceDir;
		}
		if (target != null) {
			this.currentTargetPathName = this.patchFilesTargetDir + File.separator + target;
		} else {
			this.currentTargetPathName = this.patchFilesTargetDir;
		}

		log.debug("*** New Source Path: #" + source + "#" + " - " + this.currentSourcePathName);
		log.debug("*** New Target Path: #" + target + "#" + " - " + this.currentTargetPathName);

	}

	public List<PatchFileMapping> getFileList() {

		List<PatchFileMapping> fileMappingList = new ArrayList<PatchFileMapping>();

		// initially the source and target path names are derived from the starting
		// point,
		// the base directories for source and target
		setRelativePaths(null, null);

		log.debug(
				"copy files from:" + this.patchFilesSourceDir + "\n           to  :" + this.patchFilesTargetDir + "\n");
		log.debug("process patch file listing in: " + this.patchFilesName + "\n");

		// read file line by line
		try (BufferedReader br = new BufferedReader(new FileReader(this.patchFilesName))) {

			String line;
			while ((line = br.readLine()) != null) {
				fileMappingList.addAll(processLine(line.trim()));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileMappingList;

	}

	private List<PatchFileMapping> processLine(String line) throws IOException {
		List<PatchFileMapping> files = new ArrayList<PatchFileMapping>();

		// is this a comment line? Then it will be skipped
		if (line.startsWith("#")) {
			// skip line => comment
		} else {
			// is this a path mapping directive?
			// then it will change the current from and to directories
			if (line.contains("=>")) {
				log.debug("line contains path mapping: " + line);

				String source = line.substring(0, line.indexOf("=>")).trim();
				String target = line.substring(line.indexOf("=>") + "=>".length() + 1).trim();

				log.debug("Mapping: " + source + " => " + target);
				setRelativePaths(source, target);
			} else {
				// process file
				if (!line.isEmpty()) {
					log.debug("process line: " + line);
					log.debug("  process directive: " + line);
					// copy files and return number of files copied
					files.addAll(getFileListFromLine(this.currentSourcePathName,
							this.currentTargetPathName, line));
				}
			}
		}

		return files;
	}
	
	public static List<PatchFileMapping> getFileListFromLine(String srcDir, String targetDir, String line) throws IOException {
		List<PatchFileMapping> fileList = new ArrayList<PatchFileMapping>();
		File srcDirFile = new File(srcDir);
		
		// Extract clean filename and environment directive using EnvironmentUtils
		String filterString = EnvironmentUtils.removeDirectivesFromFilename(line);
		ArrayList<String> envList = EnvironmentUtils.extractEnvironmentList(line);
		Boolean hasEnvDirective = !envList.isEmpty();
		
		log.debug("Filter string: " + filterString);
		if (hasEnvDirective) {
			log.debug("Environment directive found: " + String.join(",", envList));
		}
		
		// Check if filterString refers to a specific directory (only if it doesn't contain wildcards)
		File potentialDir = new File(srcDirFile, filterString);
		if (!filterString.contains("*") && !filterString.contains("?") && 
		    potentialDir.exists() && potentialDir.isDirectory()) {
			log.debug("Filter string refers to directory: " + potentialDir.getPath());
			
			// Handle directory copying - get all files recursively from the specified directory
			Collection<File> files = FileUtils.listFiles(potentialDir, 
				org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE, 
				org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE);
			
			// Also include the directory itself for structure preservation
			String targetDirName = EnvironmentUtils.buildTargetFileName(filterString, envList);
			
			for (File file : files) {
				log.debug("  - " + file.getPath());
				
				// Calculate relative path from the matched directory (not from srcDir)
				String relativePath = potentialDir.toPath().relativize(file.toPath()).toString();
				
				// Create target file preserving structure under the renamed directory
				File targetFile = new File(targetDir + File.separator + targetDirName + File.separator + relativePath);
				
				// Create PatchFileMapping with environment information
				fileList.add(new PatchFileMapping(file, targetFile, hasEnvDirective, envList));
			}
			
		} else {
			// Handle file pattern matching (including wildcards like f1/*.sql)
			Collection<File> files;
			
			if (filterString.contains("/") || filterString.contains("\\")) {
				// Pattern contains directory separators (like f1/*.sql)
				// We need to handle this differently
				String dirPart = "";
				String filePart = "";
				
				// Split the pattern into directory and file parts
				int lastSeparator = Math.max(filterString.lastIndexOf('/'), filterString.lastIndexOf('\\'));
				if (lastSeparator != -1) {
					dirPart = filterString.substring(0, lastSeparator);
					filePart = filterString.substring(lastSeparator + 1);
				} else {
					filePart = filterString;
				}
				
				log.debug("Directory part: '" + dirPart + "', File part: '" + filePart + "'");
				
				// Create the subdirectory to search in
				File searchDir = dirPart.isEmpty() ? srcDirFile : new File(srcDirFile, dirPart);
				
				if (!searchDir.exists()) {
					throw new RuntimeException("Directory \"" + dirPart + "\" could not be found in \"" + srcDir + "\".");
				}
				
				IOFileFilter fileFilter = new WildcardFileFilter(filePart);
				
				// Search in the specific subdirectory
				files = FileUtils.listFiles(searchDir, fileFilter, null); // null = don't recurse into subdirs
				
			} else {
				// Simple pattern without directory separators (like *.sql)
				IOFileFilter filter = new WildcardFileFilter(filterString);
				
				// Include all files from subdirectories recursively
				files = FileUtils.listFiles(srcDirFile, filter, 
					org.apache.commons.io.filefilter.TrueFileFilter.INSTANCE);
			}
			
			// raise exception when the files were not found!
			if (files.isEmpty()) {
				throw new RuntimeException("File(s) matching pattern \"" + filterString + "\" could not be found in directory \"" + srcDir + "\".");
			}
			
			for (File file : files) {
				log.debug("  - "+ file.getPath());
				
				// Calculate the relative path to preserve directory structure
				String relativePath = srcDirFile.toPath().relativize(file.toPath()).toString();
				
				// Create target filename with environment directive if present using EnvironmentUtils
				String targetFileName = EnvironmentUtils.buildTargetFileName(file.getName(), envList);
				
				// Preserve directory structure in target
				File targetFile;
				if (relativePath.contains(File.separator)) {
					// File is in a subdirectory, preserve the structure but rename the file
					String parentPath = relativePath.substring(0, relativePath.lastIndexOf(File.separator));
					targetFile = new File(targetDir + File.separator + parentPath + File.separator + targetFileName);
				} else {
					// File is in root directory
					targetFile = new File(targetDir + File.separator + targetFileName);
				}
				
				// Create PatchFileMapping with environment information
				fileList.add(new PatchFileMapping(file, targetFile, hasEnvDirective, envList));
			}
		}

		return fileList;
	}
}