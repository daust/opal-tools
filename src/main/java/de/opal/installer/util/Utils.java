package de.opal.installer.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Utils {

	public static final Logger logger = LogManager.getLogger(Utils.class.getName());

	/**
	 * 
	 * @param msg the message to be displayed with the runtimeException
	 */
	public static void throwRuntimeException(String msg) {
		logger.error(msg);
		throw new RuntimeException(msg);
	}

	/**
	 * lists all system properties for debugging purposes
	 */
	public static void logSystemProperties() {
		logger.trace("*** list all system properties");

		// Get all system properties
		Properties props = System.getProperties();
		{
			// Enumerate all system properties
			for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
				// Get property name
				String propName = (String) e.nextElement();

				// Get property value
				String propValue = (String) props.get(propName);

				logger.trace(propName + ": " + propValue);
			}
		}
	}

//	public static void waitForEnter(String message, Object... args) {
//	    Console c = System.console();
//	    if (c != null) {
//	        // printf-like arguments
//	        if (message != null)
//	            c.format(message, args);
//	        c.format("\nPress ENTER to proceed.\n");
//	        c.readLine();
//	    }
//	    
//	    
//		try {
//			System.out.println(message);
//	        System.in.read();
//	    } catch (IOException e) {
//	        // TODO Auto-generated catch block
//	        e.printStackTrace();
//	    }
//
//	}

	public static void waitForEnter(String message, Object... args) {
//		Console c = System.console();
//		if (c != null) {
//			// printf-like arguments
//			if (message != null)
//				c.format(message, args);
//			c.format("\nPress ENTER to proceed.\n");
//			c.readLine();
//		}

		if (message==null || message.equals("")) message = "Press \"ENTER\" to continue..."; 
		System.out.println(message);
		
		try {
            @SuppressWarnings("unused")
			int read = System.in.read(new byte[2]);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

}
