package de.opal.installer.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.opal.utils.EncryptorWrapper;

/**
 * @author daust
 *
 */
public class ConfigManagerConnectionPool {
	public static final Logger log = LogManager.getLogger(ConfigManagerConnectionPool.class.getName());

	private String configFileName; // this is the location of the configFile installer.json, package.json, etc.
	private File configFile;
	private ConfigDataConnectionPool configDataConnectionPool;

	/**
	 * Constructor
	 * 
	 * @throws IOException
	 */
	public ConfigManagerConnectionPool(String configFileName) throws IOException {
		log.info("init Config()");
		log.trace("configFileName: " + configFileName);

		this.configFileName = configFileName;
		this.configFile = new File(configFileName);
		this.configDataConnectionPool = new ConfigDataConnectionPool();
		this.readJSONConf(this.configFile);
	}

	public boolean hasUnencryptedPasswords() {
		EncryptorWrapper enc = new EncryptorWrapper();
		boolean hasUnencryptedPasswords = false;

		for (ConfigConnectionPool pool : this.configDataConnectionPool.connectionPools) {
			if (!enc.isEncrypted(pool.password)) {
				hasUnencryptedPasswords = true;
			}
		}
		return hasUnencryptedPasswords;

	}

	public void encryptPasswords(String encryptionKeyFilename) {
		EncryptorWrapper enc = new EncryptorWrapper();

		for (ConfigConnectionPool pool : this.configDataConnectionPool.connectionPools) {
			if (!enc.isEncrypted(pool.password)) {
				pool.password = enc.encryptPWD(pool.password, encryptionKeyFilename);
			}
		}
	}

	public void decryptPasswords(String encryptionKeyFilename) {
		EncryptorWrapper enc = new EncryptorWrapper();

		for (ConfigConnectionPool pool : this.configDataConnectionPool.connectionPools) {
			if (enc.isEncrypted(pool.password)) {
				pool.password = enc.decryptPWD(pool.password, encryptionKeyFilename);
			}
		}
	}

	// the encryption keys will be stored in the same directory as the connection
	// pool
	public String getEncryptionKeyFilename(String connPoolFilename) {
		return new File(connPoolFilename).getParent() + File.separatorChar + "keys.txt";
	}

	// @SuppressWarnings("unchecked")
	public void readJSONConf(File configFile) throws IOException {

		try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().disableHtmlEscaping()
					.create();

			configDataConnectionPool = gson.fromJson(reader, ConfigDataConnectionPool.class);

			if (configDataConnectionPool != null) 
				log.debug(configDataConnectionPool.toString());
			 else 
				log.debug("Config file is empty: " + configFileName);

		}
	}

	public void writeJSONConf() throws IOException {

		if (configDataConnectionPool == null) {
			configDataConnectionPool = new ConfigDataConnectionPool();
		}

		log.debug(configDataConnectionPool.toString());

		try {
			Writer writer = new FileWriter(this.configFileName);

			// plain output
			// Gson gson = new GsonBuilder().create();
			// with pretty printing
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().disableHtmlEscaping()
					.create();

			gson.toJson(configDataConnectionPool, writer);

			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}

	}


	public ConfigDataConnectionPool getConfigDataConnectionPool() {
		return configDataConnectionPool;
	}

	public void setConfigData(ConfigDataConnectionPool configDataConnectionPool) {
		this.configDataConnectionPool = configDataConnectionPool;
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
}
