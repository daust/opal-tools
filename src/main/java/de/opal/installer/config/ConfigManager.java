package de.opal.installer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.opal.installer.db.DBUtils;
import de.opal.utils.StringUtils;

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
	
	public String replacePlaceholders(String value) {
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
		this.configData.sqlFileRegex = replacePlaceholders(this.configData.sqlFileRegex);
		this.configData.version = replacePlaceholders(this.configData.version);
	}

	public String doTextReplacements(String filename, String fileContents) {
		ArrayList<TextReplacement> textReplacements = null;
		String str = fileContents;

		// first find matching dataSource
		textReplacements = this.getConfigData().textReplacements;

		if (textReplacements != null) {
			for (TextReplacement textReplacement : textReplacements) {
				Pattern p = Pattern.compile(textReplacement.fileRegEx, Pattern.CASE_INSENSITIVE);

				log.debug("test regex: " + textReplacement.fileRegEx + " with " + textReplacement.expressions);
				if (p.matcher(filename).find()) {
					log.debug("process file " + filename + " with: " + textReplacement.expressions);

					// replace texts
					for (TextReplacementExpression expression : textReplacement.expressions) {
						Pattern ep = Pattern.compile(expression.regEx, Pattern.CASE_INSENSITIVE);
						Matcher m = ep.matcher(str);
						str = m.replaceAll(expression.value);
					}

					break;
				}
			}
		}

		return str;
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

		// when looking for a match then we need to prefix with / or \ because the old
		// regular expressions start with / or \ and not sql
		/* TODO:  make this VERSION dependent starting with version 2.7.1 */
		if (filename.startsWith("sql"))
			filename = File.separator + filename;

		log.debug("determine encoding for file: " + filename);

		// first find matching dataSource
		encodingMappings = this.getConfigData().encodingMappings;

		// return immediately if not encoding settings were passed.
		if (encodingMappings == null) {
			return "";
		}
		for (ConfigEncodingMapping configEncodingMapping : encodingMappings) {
			if (configEncodingMapping.fileRegex != null &&
					configEncodingMapping.fileFilter != null)
				throw new RuntimeException("You cannot use both fileFilter AND fileRegex at the same time, you have to choose one.");
			
			Pattern p;
			// use regular expression to map file path to connection pool
			// PREFER fileRegex if both fileFilter and fileRegex are defined
			if (configEncodingMapping.fileRegex != null) {
				// use fileRegex
				p = Pattern.compile(configEncodingMapping.fileRegex, Pattern.CASE_INSENSITIVE);
			} else {
				// use fileFilter
				String fileRegex = StringUtils.convertFileFilterToFileRegex(configEncodingMapping.fileFilter);
				p = Pattern.compile(fileRegex, Pattern.CASE_INSENSITIVE);
			}

			log.debug("test mapping: " + configEncodingMapping.encoding + " with " + configEncodingMapping.fileRegex);
			if (p.matcher(filename).find()) {
				encoding = configEncodingMapping.encoding;
				log.debug("process file " + filename + " with encoding: " + encoding);

				break;
			}
		}

		return encoding;
	}
	
	public String convertOSEncodingToNLS_LANG(String encoding) {
		String nls_lang=""; // default?
		
		if (this.configData.encodingNLSMappings == null)
			throw new RuntimeException("Encoding to NLS_LANG mappings are missing in defaults config file.");
		
		for (EncodingNLSMapping mapping : this.configData.encodingNLSMappings) {
			if (mapping.encoding.toLowerCase().equals(encoding.toLowerCase())){
				nls_lang=mapping.NLS_LANG;
			}
		}
		
		if (nls_lang.isEmpty())
			throw new RuntimeException("Encoding to NLS_LANG mapping for encoding " + encoding + " is missing in defaults config file.");
				
		return nls_lang;
	}

	public String getRelativeFilename(String filename) {
		String relativeFilename = null;

		if (filename == null)
			return null;

		relativeFilename = filename.replace(this.getPackageDirName() + File.separator, "");

		return relativeFilename;
	}
	
	public void validateMandatoryAttributes(List<String> mandatoryAttributes) {
		ConfigData data = this.getConfigData();
		Class<?> configDataclass = data.getClass();

		for (String attr : mandatoryAttributes) {
			try {
				Field field = configDataclass.getDeclaredField(attr);

				String strValue = (String) field.get(data);

				if (strValue == null || strValue.isEmpty()) {
					throw new RuntimeException(
							"Attribute \"" + attr + "\" in file " + this.configFileName + " cannot be empty.");
				}

			} catch (Exception e) {
				System.err.println(e.getLocalizedMessage());
				System.exit(1);
			}
		}

	}


}
