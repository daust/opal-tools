package de.opal.utils;

import java.awt.Desktop;
import java.io.File;

/*
 * https://stackoverflow.com/questions/7024031/java-open-a-file-windows-mac
 */

public class OSDetector {
	private static boolean isWindows = false;
	private static boolean isLinux = false;
	private static boolean isMac = false;

	static {
		String os = System.getProperty("os.name").toLowerCase();
		isWindows = os.contains("win");
		isLinux = os.contains("nux") || os.contains("nix");
		isMac = os.contains("mac");
	}

	public static boolean isWindows() {
		return isWindows;
	}

	public static boolean isLinux() {
		return isLinux;
	}

	public static boolean isMac() {
		return isMac;
	};

	public static boolean open(File file) {
		try {
			if (OSDetector.isWindows()) {
				Runtime.getRuntime()
						.exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", file.getAbsolutePath() });
				return true;
			} else if (OSDetector.isLinux() || OSDetector.isMac()) {
				Runtime.getRuntime().exec(new String[] { "/usr/bin/open", file.getAbsolutePath() });
				return true;
			} else {
				// Unknown OS, try with desktop
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(file);
					return true;
				} else {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
			return false;
		}
	}

}