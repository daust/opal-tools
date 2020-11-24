package de.opal.installer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.opal.installer.db.DBUtils;

/**
 * @author daust
 *
 */
public class ConfigManager {
	public static final Logger log = LogManager.getLogger(ConfigManager.class.getName());

	private String packageDirName; // this is the path to the base directory of the patch / release / package
	private String configFileName; // this is the location of the configFile installer.json, package.json, etc.

	private File configFile;
	private File packageDir;
	private File sqlDir;

	// definitions off all connection pools
	private ConfigData configData;

	/*
	 * INORDER_ALL_FILES: INORDER: only sql files
	 */
	public enum TraversalType {
		STATIC_FILES, INORDER, INORDER_ALL_FILES
	}

	private TraversalType traversalType;

	private String replacePlaceholders(String value) {
		// abort when value is null
		if (value == null || value.isEmpty())
			return value;

		// first you trim
		String newValue = value.trim();
		String envValue = "";

		// replace parent_folder_name
		newValue = newValue.replace("#PARENT_FOLDER_NAME#", this.packageDir.getName());

		// replace placeholder with env variable value
		// if env variable is null, at least replace placeholder
		envValue = DBUtils.nvl(System.getenv("OPAL_TOOLS_USER_IDENTITY"), "");
		newValue = newValue.replace("#ENV_OPAL_TOOLS_USER_IDENTITY#", envValue);

		return newValue;
	}

	/**
	 * trim() and replace() placeholders
	 */
	public void replacePlaceholders() {

		this.configData.application = replacePlaceholders(this.configData.application);
		this.configData.author = replacePlaceholders(this.configData.author);
		this.configData.packageDir = replacePlaceholders(this.configData.packageDir);
		this.configData.patch = replacePlaceholders(this.configData.patch);
		this.configData.sqlDir = replacePlaceholders(this.configData.sqlDir);
		this.configData.sqlFileRegEx = replacePlaceholders(this.configData.sqlFileRegEx);
		this.configData.version = replacePlaceholders(this.configData.version);
	}

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public ConfigManager(String configFileName) throws IOException {
		log.info("init Config()");
		log.trace("configFileName: " + configFileName);

		this.configFileName = configFileName;
		this.configFile = new File(configFileName);
		this.configData = new ConfigData();
		this.readJSONConf(this.configFile);

		// get package path from configData, can be relative or absolute
		String packageDirPathName = this.configData.packageDir;
		Path packageDirPath = Paths.get(packageDirPathName);
		if (!packageDirPath.isAbsolute()) {
			log.trace("Path is relative ...");
			// relative path, so do concatenate from config file
			packageDirPath = this.configFile.getParentFile().toPath().resolve(this.configData.packageDir);
			log.trace("packageDirPath: " + packageDirPath.toString());
		}

		log.debug("packageDirPath: " + packageDirPath.toRealPath().toString());
		this.packageDirName = packageDirPath.toRealPath().toString();
		this.packageDir = new File(this.packageDirName);

		// sqlDir
		// get sqlDir path from configData, can be relative or absolute
		String sqlDirPathName = this.configData.sqlDir;
		Path sqlDirPath = Paths.get(sqlDirPathName);
		if (!sqlDirPath.isAbsolute()) {
			log.trace("SQL dir path is relative ...");
			// relative path, so do concatenate from packageDir
			sqlDirPath = this.packageDir.toPath().resolve(sqlDirPathName);
			log.trace("packageDirPath: " + packageDirPath);
			log.trace("sqlDirPath: " + sqlDirPath);
		}

		try {
			log.trace("sqlDirPath: " + sqlDirPath.toRealPath().toString());
			this.sqlDir = sqlDirPath.toRealPath().toFile();
		} catch (IOException ex) {
			// ignore for connection pools
			/* TODO: separate config files and throw error message when file not found */
		}

		log.debug("traversalType: " + this.configData.traversalType);
		this.traversalType = TraversalType.valueOf(this.configData.traversalType);

		// this.dumpConfig();
	}

	// @SuppressWarnings("unchecked")
	public void readJSONConf(File configFile) throws IOException {

		try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().disableHtmlEscaping()
					.create();

			configData = gson.fromJson(reader, ConfigData.class);

			if (configData != null) {
				log.debug(configData.toString());
			} else {
				log.debug("Config file is empty: " + configFileName);
			}

		}
	}

	public void writeJSONConf() throws IOException {

		if (configData == null) {
			configData = new ConfigData();
		}

		log.debug(configData.toString());

		try {
			Writer writer = new FileWriter(this.configFileName);

			// plain output
			// Gson gson = new GsonBuilder().create();
			// with pretty printing
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().disableHtmlEscaping()
					.create();

			gson.toJson(configData, writer);

			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}

	public void writeJSONConfInitFile() throws IOException {
		/* clean defaults if requested */
		configData.traversalType = configData.traversalType == "INORDER" ? null : configData.traversalType;
		configData.sqlDir = "sql".contentEquals(configData.sqlDir) ? null : configData.sqlDir;

		// can't remove wait statement, because when installing the patch it won't wait
		// configData.waitAfterEachStatement=null;
		configData.runMode = null;

		// now write it generically
		writeJSONConf();
	}

	/**
	 * 
	 * @return
	 */
	public File getPackageDir() {
		return packageDir;
	}

	public ConfigData getConfigData() {
		return configData;
	}

	public void setConfigData(ConfigData configData) {
		this.configData = configData;
	}

	/**
	 * @return the packageDirName
	 */
	public String getPackageDirName() {
		return packageDirName;
	}

	/**
	 * @return the configFileName
	 */
	public String getConfigFileName() {
		return configFileName;
	}

	/**
	 * @return the configFile
	 */
	public File getConfigFile() {
		return configFile;
	}

	/**
	 * @return the sqlDir
	 */
	public File getSqlDir() {
		return sqlDir;
	}

	/**
	 * @param sqlDir the sqlDir to set
	 */
	public void setSqlDir(File sqlDir) {
		this.sqlDir = sqlDir;
	}

	/**
	 * @return the traversalType
	 */
	public TraversalType getTraversalType() {
		return traversalType;
	}

	/**
	 * 
	 * @param filename
	 * @return
	 */
	public String getEncoding(String filename) {
		ArrayList<ConfigEncodingMapping> encodingMappings;
		String encoding = "";

		log.debug("determine encoding for file: " + filename);

		// first find matching dataSource
		encodingMappings = this.getConfigData().encodingMappings;

		// return immediately if not encoding settings were passed.
		if (encodingMappings == null) {
			return "";
		}
		for (ConfigEncodingMapping configEncodingMapping : encodingMappings) {
			Pattern p = Pattern.compile(configEncodingMapping.matchRegEx);

			log.debug("test mapping: " + configEncodingMapping.encoding + " with " + configEncodingMapping.matchRegEx);
			if (p.matcher(filename).find()) {
				encoding = configEncodingMapping.encoding;
				log.debug("process file " + filename + " with encoding: " + encoding);

				break;
			}
		}

		return encoding;
	}

}
