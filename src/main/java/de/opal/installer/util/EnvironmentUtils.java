package de.opal.installer.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.opal.utils.MsgLog;

/**
 * Utility class for environment-related operations
 * Centralizes environment directive parsing and filtering logic
 */
public class EnvironmentUtils {
	
	private static final Logger log = LoggerFactory.getLogger(EnvironmentUtils.class.getName());

	/**
	 * Extracts environment list from a filename that has -env(...) format
	 * 
	 * Examples:
	 * "preinstall-env(int,prod).sql" -> ["int", "prod"]
	 * "config-env(dev,test,staging).xml" -> ["dev", "test", "staging"]  
	 * "startup.sh" -> [] (empty list)
	 * 
	 * @param filename The filename to parse
	 * @return ArrayList of environment names, empty if no directive found
	 */
	public static ArrayList<String> extractEnvironmentFromFilename(String filename) {
		ArrayList<String> envList = new ArrayList<String>();
		
		if (filename == null) {
			return envList;
		}
		
		// Pattern to match -env(environments) in filename
		Pattern envPattern = Pattern.compile("-env\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
		Matcher matcher = envPattern.matcher(filename);
		
		if (!matcher.find()) {
			return envList;
		}
		
		// Extract the environments from -env(env1,env2,env3)
		String environments = matcher.group(1);
		
		// Split by comma and clean up each environment name
		String[] envArray = environments.split(",");
		for (String env : envArray) {
			String trimmedEnv = env.trim();
			if (!trimmedEnv.isEmpty()) {
				envList.add(trimmedEnv);
			}
		}
		
		log.debug("Extracted environments from filename '" + filename + "': " + envList);
		
		return envList;
	}

	
	/**
	 * Determines if a file should be included for the target system based on its
	 * -env() configuration, checking the entire path hierarchy from top-level down,
	 * allowing more specific (local) environment directives to override broader ones
	 * 
	 * @param filename     The full file path to check
	 * @param targetSystem The target system/environment (null or empty means include all files)
	 * @return true if the file should be included, false otherwise
	 * @throws IOException 
	 */
	public static boolean shouldIncludeFileForTarget(String filename, String targetSystem) throws IOException {
		// If no target system specified, include ALL files
		if (targetSystem == null || targetSystem.trim().isEmpty()) {
			log.debug("No target system specified - including file: " + filename);
			return true;
		}
		
		File file = new File(filename);
		
		// Build the path hierarchy from top-level to most specific
		List<String> pathComponents = new ArrayList<String>();
		File currentPath = file;
		while (currentPath != null) {
			pathComponents.add(0, currentPath.getName()); // Add to beginning to reverse order
			currentPath = currentPath.getParentFile();
		}
		
		// Start with "include" as default and process from top-level down
		boolean shouldInclude = true;
		String lastMatchedComponent = "";
		String lastMatchedEnvs = "";
		
		// Process path components from top-level to most specific
		for (String pathComponent : pathComponents) {
			// Pattern to match -env(environments) in path component
			Pattern envPattern = Pattern.compile("-env\\(([^)]+)\\)", Pattern.CASE_INSENSITIVE);
			Matcher matcher = envPattern.matcher(pathComponent);
			
			if (matcher.find()) {
				// Found environment directive in this path component
				String environments = matcher.group(1);
				
				// Split by comma and trim whitespace
				String[] envArray = environments.split(",");
				
				boolean matchFound = false;
				for (String env : envArray) {
					String trimmedEnv = env.trim();
					if (trimmedEnv.equalsIgnoreCase(targetSystem)) {
						matchFound = true;
						break;
					}
				}
				
				// Override previous decision with more specific one
				shouldInclude = matchFound;
				lastMatchedComponent = pathComponent;
				lastMatchedEnvs = environments;
				
				log.debug("Found env directive in path component '" + pathComponent + "' (env: " + environments + ") - " + 
						 (matchFound ? "MATCHES" : "NO MATCH") + " for target: " + targetSystem);
			}
		}
		
		// Log the final decision
		if (!lastMatchedComponent.isEmpty()) {
			if (shouldInclude) {
				log.debug("Including file: " + filename + " - environment match in '" + lastMatchedComponent + 
						 "' (env: " + lastMatchedEnvs + ") matches target: " + targetSystem);
			} else {
				MsgLog.println("Excluding file with env restrictions: " + filename + " - environment restriction in '" + lastMatchedComponent + 
						"' (env: " + lastMatchedEnvs + ") does not match target: " + targetSystem);				
			}
		} else {
			log.debug("Including file: " + filename + " - no environment restrictions found in path");
		}
		
		return shouldInclude;
	}

	/**
	 * Removes all directives from a filename or line, returning just the clean filename
	 * 
	 * Examples:
	 * "preinstall.sql /env int,prod" -> "preinstall.sql"
	 * "config.xml /env dev,test" -> "config.xml"
	 * "startup.sh" -> "startup.sh"
	 * 
	 * @param filenameWithDirectives The filename that may contain directives
	 * @return The clean filename without any directives
	 */
	public static String removeDirectivesFromFilename(String filenameWithDirectives) {
		if (filenameWithDirectives == null) {
			return null;
		}
		
		// Find the position of the first directive marker
		int directiveStart = filenameWithDirectives.indexOf("/env ");
		
		if (directiveStart == -1) {
			// No directives found, just trim and return
			return filenameWithDirectives.trim();
		}
		
		// Extract everything before the first directive
		String cleanFilename = filenameWithDirectives.substring(0, directiveStart).trim();
		
		log.debug("Removed directives: '" + filenameWithDirectives + "' -> '" + cleanFilename + "'");
		
		return cleanFilename;
	}
	
	/**
	 * Extracts the environment list from a line containing /env directive
	 * 
	 * Examples:
	 * "preinstall.sql /env int,prod" -> ["int", "prod"]
	 * "config.xml /env dev,test,staging" -> ["dev", "test", "staging"]
	 * "startup.sh" -> [] (empty list)
	 * 
	 * @param line The line that may contain /env directive
	 * @return ArrayList of environment names, empty if no directive found
	 */
	public static ArrayList<String> extractEnvironmentList(String line) {
		ArrayList<String> envList = new ArrayList<String>();
		
		if (line == null || !line.contains("/env ")) {
			return envList;
		}
		
		// Find the /env directive
		int envStart = line.indexOf("/env ");
		if (envStart == -1) {
			return envList;
		}
		
		// Extract everything after "/env "
		String envPart = line.substring(envStart + "/env ".length()).trim();
		
		// Split by comma and clean up each environment name
		String[] environments = envPart.split(",");
		for (String env : environments) {
			String trimmedEnv = env.trim();
			if (!trimmedEnv.isEmpty()) {
				envList.add(trimmedEnv);
			}
		}
		
		log.debug("Extracted environments from '" + line + "': " + envList);
		
		return envList;
	}
	
	/**
	 * Builds the target filename with environment directive if present
	 * 
	 * Examples:
	 * buildTargetFileName("preinstall.sql", ["int", "prod"]) -> "preinstall-env(int,prod).sql"
	 * buildTargetFileName("config.xml", []) -> "config.xml"
	 * 
	 * @param originalFileName The original filename
	 * @param envList List of environments, empty if no directive
	 * @return The target filename with environment suffix if applicable
	 */
	public static String buildTargetFileName(String originalFileName, ArrayList<String> envList) {
		if (envList.isEmpty()) {
			return originalFileName;
		}
		
		// Build the environment suffix from the list
		String envSuffix = "-env(" + String.join(",", envList) + ")";
		
		int lastDotIndex = originalFileName.lastIndexOf('.');
		
		if (lastDotIndex == -1) {
			// No extension, just append suffix
			return originalFileName + envSuffix;
		} else {
			// Insert suffix before the extension
			String nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
			String extension = originalFileName.substring(lastDotIndex);
			return nameWithoutExtension + envSuffix + extension;
		}
	}
}