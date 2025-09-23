package de.opal.installer.zip;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.PatchFileMapping;
import de.opal.installer.PatchFilesTxtWrapper;
import de.opal.installer.config.ConfigConnectionMapping;
import de.opal.installer.config.ConfigManager;
import de.opal.installer.util.EnvironmentUtils;
import de.opal.installer.util.Filesystem;
import de.opal.utils.MsgLog;
import de.opal.utils.StringUtils;
import de.opal.utils.VersionInfo;

public class ZipInstaller {
	public static final Logger log = LogManager.getLogger(ZipInstaller.class.getName());

	private String zipFileName = "";
	private ArrayList<SqlFileWrapper> sqlFiles = null;
	private ArrayList<SqlInstallFileWrapper> sqlInstallFiles = null;
	private BatchFileWrapper batchFile = null;

	private String sqlInstallFileNameTemplate = "script#COUNT#.sql";

	private ConfigManager configManager;
	private ConfigManager defaultsConfigManager;

	private ZipOutputStream zout = null;

	private String configFileName;

	private String patchFilesName;
	private String patchFilesSourceDir;
	private String patchFilesTargetDir;

	private boolean noLogging;

	private boolean isSilent = false;

	List<String> mandatoryAttributes;
	List<String> zipIncludeFiles;

	private String defaultsConfigFileName = null;
	private boolean bConvertToUTF8 = false;
	private String targetSystem = null; // New field for target system

	public ZipInstaller(String zipFileName, String configFileName, List<String> mandatoryAttributes, boolean noLogging,
			String patchFilesName, String patchFilesSourceDir, boolean isSilent, List<String> zipIncludeFiles,
			String defaultsConfigFileName, boolean bConvertToUTF8, String targetSystem) throws IOException {

		this.zipFileName = zipFileName;
		this.configFileName = configFileName;
		this.defaultsConfigFileName = defaultsConfigFileName;
		this.mandatoryAttributes = mandatoryAttributes;
		this.noLogging = noLogging;
		this.patchFilesName = patchFilesName;
		this.patchFilesSourceDir = patchFilesSourceDir;
		this.isSilent = isSilent;
		this.zipIncludeFiles = zipIncludeFiles;
		this.bConvertToUTF8 = bConvertToUTF8;
		this.targetSystem = targetSystem; // Set target system

		this.configManager = new ConfigManager(this.configFileName);
		this.defaultsConfigManager = new ConfigManager(this.defaultsConfigFileName);

		// add NLS Mappings to configManager if not already existing
		if (this.configManager.getConfigData().encodingNLSMappings == null
				&& this.defaultsConfigManager.getConfigData().encodingNLSMappings != null)
			this.configManager.getConfigData().encodingNLSMappings = this.defaultsConfigManager
					.getConfigData().encodingNLSMappings;

		// replace placeholders in opal-installer.json file
		// only replace them during installer, not setup
		this.configManager.replacePlaceholders();

		// validate the mandatory attributes in the config file
		this.configManager.validateMandatoryAttributes(mandatoryAttributes);

		// is this required for the zipInstall() ?
		// TODO: required for zipInstall() ?
		this.configManager.getConfigData().runMode = "EXECUTE";

		this.patchFilesTargetDir = this.configManager.getRelativeFilename(configManager.getSqlDir().getAbsolutePath());

		this.sqlFiles = new ArrayList<SqlFileWrapper>();
		this.sqlInstallFiles = new ArrayList<SqlInstallFileWrapper>();

		String logFileDir = this.configManager.getPackageDir().getAbsolutePath() + File.separator + "logs";
		String logfileName = generateLogFileName(logFileDir);

		if (!this.noLogging) {
			MsgLog.createLogDirectory(logFileDir);
			MsgLog.createLogfile(logfileName);
		}

		// replace variables in zipFileName, e.g."#PARENT_FOLDER_NAME#-with-prompts.zip"
		this.zipFileName = this.configManager.replacePlaceholders(this.zipFileName);
	}

	/**
	 * @param fsTree
	 * @throws IOException
	 */
	public void createZipFromFiletree(List<PatchFileMapping> fsTree) throws IOException {

		// create zip file
		FileOutputStream fout = new FileOutputStream(this.zipFileName);
		this.zout = new ZipOutputStream(fout);

		if (this.bConvertToUTF8)
			MsgLog.println("\nconverting encoding of sql files");
		
		// process all files in sql subdirectory (including referenced files)
		for (PatchFileMapping fileMapping : fsTree) {
			// pick the right file when using SourceFilesReference.conf
			String filenameAbsolute = (fileMapping.srcFile != null) ? fileMapping.srcFile.getAbsolutePath()
					: fileMapping.destFile.getAbsolutePath();
			String filenameZipFile = convertFilePathseparatorToSlash(this.configManager.getRelativeFilename(fileMapping.destFile.getAbsolutePath()));
			String connPoolName = getConnPoolNameForFile(fileMapping.destFile.getAbsolutePath());
			String encoding = configManager.getEncoding(fileMapping.destFile.getAbsolutePath());

			SqlFileWrapper sqlFile = null;
			// use UTF-8 encoding everywhere if conversion is requested
			if (this.bConvertToUTF8)
				sqlFile = new SqlFileWrapper(filenameAbsolute, filenameZipFile, connPoolName, StandardCharsets.UTF_8.name());
			else
				sqlFile = new SqlFileWrapper(filenameAbsolute, filenameZipFile, connPoolName, encoding);

			log.debug("process file: " + sqlFile.filenameAbsolute);

			sqlFiles.add(sqlFile);
			// add file to zip
			addFileToZip(sqlFile.filenameZipFile, sqlFile.filenameAbsolute, encoding);
		}

		MsgLog.println("\nadding sql files to zip");
		// create sqlInstallFiles array in memory
		log.debug("*** create sqlInstallFiles in memory");
		SqlInstallFileWrapper sqlInstallFile = new SqlInstallFileWrapper(
				sqlInstallFileNameTemplate.replace("#COUNT#", Integer.toString(this.sqlInstallFiles.size() + 1)), "",
				this.isSilent);

		SqlFileWrapper lastSqlFile = null;

		for (SqlFileWrapper sqlFile : this.sqlFiles) {
			log.debug("  process file: " + sqlFile.filenameAbsolute);
			MsgLog.println("  " + sqlFile.filenameZipFile + "; encoding: " + sqlFile.encoding + "; conn pool: "
					+ sqlFile.connPoolName);

			// break sql install file because of change in connection pool or encoding?
			if (lastSqlFile != null) {
				if (!sqlFile.connPoolName.equals(lastSqlFile.connPoolName)
						|| !sqlFile.encoding.equals(lastSqlFile.encoding)) {
					// switch file (first save the current file to the zip, then create a new one)
					// sqlInstallFile.addFooter();
					this.sqlInstallFiles.add(sqlInstallFile);

					sqlInstallFile = new SqlInstallFileWrapper(sqlInstallFileNameTemplate.replace("#COUNT#",
							Integer.toString(this.sqlInstallFiles.size() + 1)), "", this.isSilent);
				}
			}

			// store the connection pool and encoding for the sqlinstallFile
			if (sqlInstallFile.getConnPoolName().isEmpty()) {
				sqlInstallFile.setConnPoolName(sqlFile.connPoolName);
				sqlInstallFile.setEncoding(sqlFile.encoding);
			}

			// add script to install<num>.sql file
			sqlInstallFile.addScript(sqlFile);

			// save current file as last file
			lastSqlFile = sqlFile;
		}
		// add the last file
		// sqlInstallFile.addFooter();
		this.sqlInstallFiles.add(sqlInstallFile);

		// add install sql files to zip
		for (SqlInstallFileWrapper sqlInstallFile2 : this.sqlInstallFiles) {
			addStringToZip(sqlInstallFile2.getFilename(), sqlInstallFile2.getContents());
		}

		log.debug("*** create batch install");
		MsgLog.println("\ncreating sql wrapper files");
		this.batchFile = new BatchFileWrapper(this.configManager);

		for (SqlInstallFileWrapper sqlInstallFile2 : this.sqlInstallFiles) {
			log.debug("  process file: " + sqlInstallFile2.getFilename());
			MsgLog.println("  " + sqlInstallFile2.getFilename());

			// add script to install<num>.sql file
			this.batchFile.addScript(sqlInstallFile2);
		}

		// add batch files to zip
		MsgLog.println("\ncreating shell wrapper files");
		MsgLog.println("  install_sqlcl.sh");
		MsgLog.println("  install_sqlplus.sh");
		MsgLog.println("  install_sqlcl.cmd");
		MsgLog.println("  install_sqlplus.cmd");

		addStringToZip("install_sqlcl.sh", this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Linux,
				BatchFileWrapper.ScriptRunner.sqlcl));
		addStringToZip("install_sqlplus.sh", this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Linux,
				BatchFileWrapper.ScriptRunner.sqlplus));
		addStringToZip("install_sqlcl.cmd", this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Windows,
				BatchFileWrapper.ScriptRunner.sqlcl));
		addStringToZip("install_sqlplus.cmd", this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Windows,
				BatchFileWrapper.ScriptRunner.sqlplus));

		// add zip include files
		if (this.zipIncludeFiles.size() > 0) {
			MsgLog.println("\nadding additional zip include files");

			for (String zipIncludeFile : zipIncludeFiles) {
				// MsgLog.println(" " + this.configManager.getRelativeFilename(zipIncludeFile));
				addFileToZipRecursively(zipIncludeFile);
			}
		}

		// close zip file
		zout.close();

		// show debug info

//		if (!this.noLogging) {
//			MsgLog.println("\n*** DEBUG ***\n");
//			MsgLog.println("");
//			MsgLog.println("* Linux/sqlcl\n");
//			MsgLog.println(this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Linux,
//					BatchFileWrapper.ScriptRunner.sqlcl));
//			MsgLog.println("");
//			MsgLog.println("* Linux/sqlplus\n");
//			MsgLog.println(this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Linux,
//					BatchFileWrapper.ScriptRunner.sqlplus));
//			MsgLog.println("");
//			MsgLog.println("* Windows/sqlcl\n");
//			MsgLog.println(this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Windows,
//					BatchFileWrapper.ScriptRunner.sqlcl));
//			MsgLog.println("");
//			MsgLog.println("* Windows/sqlplus\n");
//			MsgLog.println(this.batchFile.getContents(BatchFileWrapper.OperatingSystem.Windows,
//					BatchFileWrapper.ScriptRunner.sqlplus));
//			MsgLog.println("");
//			MsgLog.println("* install1.sql\n");
//			MsgLog.println(sqlInstallFiles.get(0).getContents());
//			MsgLog.println("");
//			MsgLog.println("* install2.sql\n");
//			MsgLog.println(sqlInstallFiles.get(1).getContents());
//		}
	}

//	private void addFileToZip(String filenameZipFile, String filenameAbsolute) throws IOException {
//		File f = new File(filenameAbsolute);
//
//		if (!f.exists()) {
//			throw new RuntimeException("File not found: " + filenameAbsolute);
//		}
//
//		ZipEntry ze = new ZipEntry(convertPathSeparatorForZip(filenameZipFile));
//		this.zout.putNextEntry(ze);
//		byte[] bytes = Files.readAllBytes(new File(filenameAbsolute).toPath());
//		this.zout.write(bytes, 0, bytes.length);
//		this.zout.closeEntry();
//	}

	private String convertFilePathseparatorToSlash( String filename) {
		String newName=filename;
		
		if (File.separatorChar != '/') {
			newName = newName.replace('\\', '/');
		  }
		
		return newName;
	}
	
	/**
	 * convert file to utf8 before adding it to the zip file
	 * 
	 * @param filenameZipFile
	 * @param filenameAbsolute
	 * @param encoding
	 * @throws IOException
	 */
	private void addFileToZip(String filenameZipFile, String filenameAbsolute, String encoding) throws IOException {
		File f = new File(filenameAbsolute);

		if (!f.exists()) {
			throw new RuntimeException("File not found: " + filenameAbsolute);
		}

		ZipEntry ze = new ZipEntry(convertFilePathseparatorToSlash(filenameZipFile));
		this.zout.putNextEntry(ze);

		if (this.bConvertToUTF8 && !encoding.equals(StandardCharsets.UTF_8.name())) {
//			Charset srcCharset = Charset.forName(encoding);
//			Charset destCharset = Charset.forName("UTF-8");
//
//			ByteBuffer byteBuffer = ByteBuffer.wrap(Files.readAllBytes(new File(filenameAbsolute).toPath()));
//			CharBuffer charBuffer = srcCharset.decode(byteBuffer);
//
//			this.zout.write(charBuffer., 0, bytes.length);

			MsgLog.println("  converting file " + filenameZipFile + " from " + encoding + " to " + StandardCharsets.UTF_8.name());
			
			Charset charsetOutput = StandardCharsets.UTF_8;
			CharsetEncoder encoder = charsetOutput.newEncoder();

			// Convert the byte array from starting inputEncoding into UCS2
			CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
			CharBuffer cbuf = decoder.decode(ByteBuffer.wrap(Files.readAllBytes(new File(filenameAbsolute).toPath())));

			// Convert the internal UCS2 representation into outputEncoding
			ByteBuffer bbuf = encoder.encode(CharBuffer.wrap(cbuf));
			// System.out.println(new String(bbuf.array(), 0, bbuf.limit(), charsetOutput));c

			byte[] bytes = new byte[bbuf.remaining()];
			bbuf.get(bytes);

			this.zout.write(bytes, 0, bytes.length);

		} else { // write directly without conversion
			byte[] bytes = Files.readAllBytes(new File(filenameAbsolute).toPath());

			this.zout.write(bytes, 0, bytes.length);
		}

		this.zout.closeEntry();
	}

	private void addFileToZipRecursively(String filenameAbsolute) throws IOException {
		File f = new File(filenameAbsolute);

		if (!f.exists()) {
			throw new RuntimeException("File not found: " + filenameAbsolute);
		}

		Filesystem fs = new Filesystem(filenameAbsolute);
		List<PatchFileMapping> fileList = fs.scanTreeFilesAndDirectories();

		for (PatchFileMapping patchFileMapping : fileList) {
			String relativeFilename="";
			Path fileParent=f.toPath().getParent();
			if (fileParent==null)
				relativeFilename=filenameAbsolute;
			else
				relativeFilename= f.toPath().getParent().relativize(patchFileMapping.destFile.toPath()).toString();
			
			//MsgLog.println("  " + patchFileMapping.destFile.toPath() + " => " + relativeFilename);
			MsgLog.println("  " + relativeFilename);

			if (patchFileMapping.destFile.isFile()) {
				// handle file
				ZipEntry ze = new ZipEntry(convertFilePathseparatorToSlash(relativeFilename));
				this.zout.putNextEntry(ze);

				// only add bytes if it is a file, not a directory
				byte[] bytes = Files.readAllBytes(new File(patchFileMapping.destFile.getAbsolutePath()).toPath());
				this.zout.write(bytes, 0, bytes.length);

				this.zout.closeEntry();

			} else {
				// handle directory
				ZipEntry ze = new ZipEntry(convertFilePathseparatorToSlash(relativeFilename + "/")); // add slash for empty directories
				this.zout.putNextEntry(ze);
				this.zout.closeEntry();
			}

		}
	}

	private void addStringToZip(String filenameZipFile, String contents) throws IOException {
		ZipEntry ze = new ZipEntry(convertFilePathseparatorToSlash(filenameZipFile));
		this.zout.putNextEntry(ze);
		byte[] bytes = contents.getBytes();
		this.zout.write(bytes, 0, bytes.length);
		this.zout.closeEntry();
	}

	private String getConnPoolNameForFile(String filename) {
		ArrayList<ConfigConnectionMapping> connectionMappings;
		String connPoolName = "";

		log.debug("getConnPoolNameForFile for file: " + filename);

		// first find matching dataSource
		connectionMappings = this.configManager.getConfigData().connectionMappings;
		for (ConfigConnectionMapping configConnectionMapping : connectionMappings) {
			Pattern p;
			
			if (configConnectionMapping.fileRegex != null &&
					configConnectionMapping.fileFilter != null)
				throw new RuntimeException("You cannot use both fileFilter AND fileRegex at the same time, you have to choose one.");
			
			// use regular expression to map file path to connection pool
			// PREFER fileRegex if both fileFilter and fileRegex are defined
			if (configConnectionMapping.fileRegex != null) {
				// use fileRegex
				p = Pattern.compile(configConnectionMapping.fileRegex, Pattern.CASE_INSENSITIVE);
			} else {
				// use fileFilter
				String fileRegex = StringUtils.convertFileFilterToFileRegex(configConnectionMapping.fileFilter);
				p = Pattern.compile(fileRegex, Pattern.CASE_INSENSITIVE);
			}

			log.debug("test mapping: " + configConnectionMapping.connectionPoolName + " with "
					+ configConnectionMapping.fileRegex);
			if (p.matcher(filename).find()) {
				connPoolName = configConnectionMapping.connectionPoolName;
				log.debug("process file " + filename + " with dataSource: " + connPoolName);

				break;
			} else {
				log.debug("  no match with regex: " + configConnectionMapping.fileRegex);
			}
		}
		if (connPoolName.isEmpty()) {
			throw new RuntimeException("no match found for path: " + filename);
		}

		return connPoolName;
	}

	private String generateLogFileName(String logFileDir) {
		String logFileName = "";

		logFileName = logFileDir + File.separator + "export-scripts-"
				+ new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".log";

		return logFileName;
	}

	/**
	 * Applies environment filtering to the complete file list
	 * This ensures that only files appropriate for the target environment are processed
	 * 
	 * @param fileList The complete list of files
	 * @param targetSystem The target environment system (null means include all files)
	 * @return Filtered list containing only files appropriate for the target environment
	 * @throws IOException 
	 */
	private List<PatchFileMapping> applyEnvironmentFiltering(List<PatchFileMapping> fileList, String targetSystem) throws IOException {
		List<PatchFileMapping> filteredList = new ArrayList<PatchFileMapping>();
		
		// If no target system specified, include ALL files
		if (targetSystem == null || targetSystem.trim().isEmpty()) {
			log.debug("No target system specified - including ALL files regardless of environment directives");
			return new ArrayList<PatchFileMapping>(fileList); // Return copy of original list
		}
		
		log.debug("Applying environment filtering for target system: " + targetSystem);
		
		for (PatchFileMapping fileMapping : fileList) {
			boolean shouldInclude = true;
			String fileName = fileMapping.destFile.getName();
			String filePath = fileMapping.destFile.getAbsolutePath();
			
			// Use EnvironmentUtils to check if file should be included - log skipped files
			shouldInclude = EnvironmentUtils.shouldIncludeFileForTarget(filePath, targetSystem);
			
			if (shouldInclude) {
				filteredList.add(fileMapping);
			}
		}
		
		return filteredList;
	}

	/**
	 * zipInstallRun()
	 * 
	 * create zip file for installation by DBAs
	 * 
	 * @throws Exception
	 * 
	 * @throws SQLException
	 */
	public void zipInstallRun() throws Exception {
		long startTime = System.currentTimeMillis();

		List<PatchFileMapping> fsTree, fsTreeFull;
		List<PatchFileMapping> fsTreePatchFiles;
		List<PatchFileMapping> fsTreeFiltered; // For environment filtering
		PatchFilesTxtWrapper patchFilesTxtWrapper = null;

		// run application
		log.debug("zipInstallRun()");
		try {
			Filesystem fs = new Filesystem(configManager.getSqlDir());

			MsgLog.println("OPAL Installer version " + VersionInfo.getVersion(this.getClass()));
			MsgLog.println("*************************");
			MsgLog.println("** Application           : " + this.configManager.getConfigData().application);
			MsgLog.println("** Patch                 : " + this.configManager.getConfigData().patch);
			MsgLog.println("** Version               : " + this.configManager.getConfigData().version);
			MsgLog.println("** Author                : " + this.configManager.getConfigData().author);

			MsgLog.println("**");
			MsgLog.println("** Zip File Name         : " + this.zipFileName);
			if (this.targetSystem != null && !this.targetSystem.trim().isEmpty()) {
				MsgLog.println("** Target System         : " + this.targetSystem);
			} else {
				MsgLog.println("** Target System         : ALL (no environment filtering)");
			}

			MsgLog.println("**");
			MsgLog.println("** Config File           : " + this.configManager.getConfigFileName());
			MsgLog.println("** SQL directory         : " + this.configManager.getSqlDir());
			if (!this.defaultsConfigFileName.isEmpty()) {
				MsgLog.println("** Defaults Config File  : " + this.defaultsConfigManager.getConfigFileName());
			}

			MsgLog.println("**");
			if (this.patchFilesName != null)
				MsgLog.println("** Patch File            : " + this.patchFilesName);
			if (this.patchFilesSourceDir != null)
				MsgLog.println("** Patch File Source Dir : " + this.patchFilesSourceDir);
			if (this.patchFilesTargetDir != null)
				MsgLog.println("** Patch File Target Dir : " + this.patchFilesTargetDir);

			MsgLog.println("**");
			MsgLog.println("** File Encoding (System): " + System.getProperty("file.encoding"));
			MsgLog.println("*************************");

//			if (this.isSilent)
//				MsgLog.consolePrintln("File listing of the files to be installed");
//			else
//				Utils.waitForEnter(
//						"Please press <enter> to start the ");

			// scan all files in tree and store in TreeFull
			fsTreeFull = fs.scanTree();
			
			// Apply basic regex filtering - pass null for targetSystem to include all files initially
			// We'll do environment filtering later in applyEnvironmentFiltering()
			fsTree = fs.filterTreeInorder(fsTreeFull, configManager.getConfigData().sqlFileRegex, configManager, this.targetSystem);

			// scan files from PatchFiles.txt and merge with list
			if (this.patchFilesName != null) {
				patchFilesTxtWrapper = new PatchFilesTxtWrapper(this.patchFilesName, this.patchFilesSourceDir,
						this.patchFilesTargetDir);
				fsTreePatchFiles = patchFilesTxtWrapper.getFileList();
				if (fsTreePatchFiles != null) {
					log.debug("Found " + fsTreePatchFiles.size() + " files from PatchFiles.txt");
					log.debug(fsTreePatchFiles.toString());
				}

				fsTree.addAll(fsTreePatchFiles);
				Collections.sort(fsTree);
			}

			// Apply environment filtering to the complete file list
			fsTreeFiltered = applyEnvironmentFiltering(fsTree, this.targetSystem);
			
			// Log filtering statistics
			int originalCount = fsTree.size();
			int filteredCount = fsTreeFiltered.size();
			int excludedCount = originalCount - filteredCount;
			
			if (this.targetSystem != null && !this.targetSystem.trim().isEmpty()) {
				MsgLog.println("\n*** Environment Filtering Results for target system: " + this.targetSystem);
				MsgLog.println("*** Total files found: " + originalCount);
				MsgLog.println("*** Files to be processed: " + filteredCount);
				if (excludedCount > 0) {
					MsgLog.println("*** Files excluded due to environment restrictions: " + excludedCount);
				}
			} else {
				MsgLog.println("\n*** Environment Filtering Results: ENVIRONMENT-NEUTRAL FILES ONLY");
				MsgLog.println("*** Total files found: " + originalCount);
				MsgLog.println("*** Environment-neutral files to be processed: " + filteredCount);
				if (excludedCount > 0) {
					MsgLog.println("*** Environment-specific files excluded: " + excludedCount);
				}
			}
			MsgLog.println(""); 

			createZipFromFiletree(fsTreeFiltered);

			displayStatsFooter(fsTreeFiltered.size(), startTime);

		} catch (Exception e) {
			log.error(e.getMessage());
			MsgLog.closeLogfile();

			// reraise exception
			throw (e);
		} finally {
			MsgLog.closeLogfile();
		}
	}

	private void displayStatsFooter(int totalObjectCnt, long startTime) throws IOException {
		long finish = System.currentTimeMillis();
		long timeElapsed = finish - startTime;
		int minutes = (int) (timeElapsed / (60 * 1000));
		int seconds = (int) ((timeElapsed / 1000) % 60);
		String timeElapsedString = String.format("%d:%02d", minutes, seconds);

		MsgLog.println("\n*** " + totalObjectCnt + " files were processed in " + timeElapsedString + " [mm:ss].");
	}

}