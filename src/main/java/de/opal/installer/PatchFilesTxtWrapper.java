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
	
	public static List<PatchFileMapping> getFileListFromLine(String srcDir, String targetDir, String filterString) throws IOException {
		List<PatchFileMapping> fileList=new ArrayList<PatchFileMapping>();
		File srcDirFile = new File(srcDir);
		//File targetDirFile = new File(targetDir);
		
		IOFileFilter filter = new WildcardFileFilter(filterString);

		// exclude all files from subdirectories
		// else use as directory filter: TrueFileFilter.INSTANCE
		Collection<File> files = FileUtils.listFiles(srcDirFile, filter, null);
		
		// raise exception when the files were not found!
		if (files.isEmpty()) {
			throw new RuntimeException("File(s) \"" + filterString + "\" could not be found in directory \"" + srcDir + "\"." );
		}

		
		for (File file : files) {
			log.debug("  - "+ file.getName());
			//fileList.add(new PatchFileMapping(file, new File(targetDir+File.separator+file.getName())));
			fileList.add(new PatchFileMapping(file, new File(targetDir+File.separator+file.getName())));
			
		}

		// return number 
		return fileList;
	}


}
