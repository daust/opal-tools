package de.opal.db;

public class ConnectionUtility {

	public static String transformJDBCConnectString(String connectString) {
		String transformedURL=connectString;
		
		// already contains the full url
		if (transformedURL.startsWith("jdbc:")) {
			// ok
		}else {
			transformedURL = "jdbc:oracle:thin:@" + transformedURL;
		}
		
		return transformedURL;
	}
}
