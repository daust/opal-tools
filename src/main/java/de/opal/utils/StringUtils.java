package de.opal.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	public static String extractSchemaFromUserName(String userName) {
		String patternString = ".*\\[(.*)\\]";
		if (userName.contains("[")) {
			Pattern pattern = Pattern.compile(patternString);
			Matcher matcher = pattern.matcher(userName);
			while (matcher.find())
				userName = matcher.group(1);
		}
		return userName;
	}

	public static String[] removeQuotes(String[] arr) {
		if (arr == null)
			return arr;

		String[] newArr = new String[(arr.length)];

		for (int i = 0; i < arr.length; i++) {
			newArr[i] = arr[i].replaceAll("^\"|\"$", "");
		}

		return newArr;
	}

	// Turn the simple syntax for fileFilters into a regular expression, 
	// e.g. 
	// "fileFilter": "/sql/*schema1*"
	// =>
	// "fileRegex": "\\\\sql\\\\.*schema1.*",
	public static String convertFileFilterToFileRegex(String fileFilter) {
		String fileRegex = fileFilter;

		fileRegex = fileRegex.replace("*", ".*");
		if (osIsWindows())
			fileRegex = fileRegex.replace("/", "\\\\");

		return fileRegex;
	}

	// is the current operating system a windows environment?
	private static Boolean osIsWindows() {
		return System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;
	}

}
