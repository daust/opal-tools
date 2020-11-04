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
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.opal.installer.util.Msg;
import de.opal.utils.EncryptorWrapper;

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
			log.debug("Path is relative ...");
			// relative path, so do concatenate from config file
			packageDirPath = this.configFile.getParentFile().toPath().resolve(this.configData.packageDir);
			log.debug("packageDirPath: " + packageDirPath.toString());
		}

		log.debug("packageDirPath: " + packageDirPath.toRealPath().toString());
		this.packageDirName = packageDirPath.toRealPath().toString();
		this.packageDir = new File(this.packageDirName);

		// sqlDir
		// get sqlDir path from configData, can be relative or absolute
		String sqlDirPathName = this.configData.sqlDir;
		Path sqlDirPath = Paths.get(sqlDirPathName);
		if (!sqlDirPath.isAbsolute()) {
			log.trace("Path is relative ...");
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

	public boolean hasUnencryptedPasswords() {
		EncryptorWrapper enc = new EncryptorWrapper();
		boolean hasUnencryptedPasswords=false;
		
		for (ConfigConnectionPool pool : this.configData.connectionPools) {
			if (!enc.isEncrypted(pool.password)) {
				hasUnencryptedPasswords=true;
			}
		}
		return hasUnencryptedPasswords;

	}
	public void encryptPasswords() {
		EncryptorWrapper enc = new EncryptorWrapper();
		
		for (ConfigConnectionPool pool : this.configData.connectionPools) {
			if (!enc.isEncrypted(pool.password)) {
				pool.password = enc.encryptPWD(pool.password);
			}
		}
	}
	public void decryptPasswords() {
		EncryptorWrapper enc = new EncryptorWrapper();
		
		for (ConfigConnectionPool pool : this.configData.connectionPools) {
			if (enc.isEncrypted(pool.password)) {
				pool.password = enc.decryptPWD(pool.password);
			}
		}
	}

	public void dumpConfig() {
		Msg.println("*** Dump config ***");
		Msg.println("packageDir: " + this.packageDirName);
		Msg.println("configFileName: " + this.configFileName);

		//Msg.println(this.configData.toString());
	}

	// @SuppressWarnings("unchecked")
	public void readJSONConf(File configFile) throws IOException {

		// try(Reader reader = new
		// InputStreamReader(Config.class.getResourceAsStream("/Server2.json"),
		// "UTF-8")){

		try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

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

		/*
		 * configData.setAuthor("Dietmar"); configData.setPackageName("Package1");
		 * ArrayList<String> connections = new ArrayList<>(Arrays.asList("system",
		 * "shdb_200", "shdb_jda")); configData.setConnections(connections);
		 * configData.setInternalValue("hidden");
		 */
		log.debug(configData.toString());

		try {
			Writer writer = new FileWriter(this.configFileName);

			// plain output
			// Gson gson = new GsonBuilder().create();
			// with pretty printing
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

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
		configData.targetSystem = null;

		// can't remove wait statement, because when installing the patch it won't wait
		// configData.waitAfterEachStatement=null;

		configData.runMode = null;

		// now write it generically
		writeJSONConf();
	}

	public void writeJSONConfPool() throws IOException {

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
