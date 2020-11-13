package de.opal.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionInfo {
	private static String version = null;

	/**
	 * readVersionFromFile
	 * 
	 * Read version from file version.properties in same package
	 */
	private static void readVersionFromFile(Class myClass) {
		Properties prop = new Properties();
		String result = "";

		try (InputStream inputStream = myClass.getResourceAsStream("version.properties")) {

			prop.load(inputStream);
			result = prop.getProperty("version");

		} catch (IOException e) {
			e.printStackTrace();
		}

		version = result;
	}

	public static void showVersionInfo(Class myClass, String program, boolean exitProgram) {
		if (version == null)
			readVersionFromFile(myClass);

		System.out.println(program + " version: " + version);

		if (exitProgram)
			System.exit(0);
	}

	public static String getVersion(Class myClass) {
		if (version == null)
			readVersionFromFile(myClass);

		return version;
	}

}
