package de.opal.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import de.opal.installer.util.Msg;

public class OpalFileUtils {

	/**
	 * 
	 * @param srcDir
	 * @param targetDir
	 * @param filterString
	 * @return number of files copied
	 * @throws IOException
	 */
	public static int copyDirectory(String srcDir, String targetDir, String filterString) throws IOException {
		File srcDirFile = new File(srcDir);
		File targetDirFile = new File(targetDir);
		
		int cnt=0;

		IOFileFilter filter = new WildcardFileFilter(filterString);

		// exclude all files from subdirectories
		// else use as directory filter: TrueFileFilter.INSTANCE
		Collection<File> files = FileUtils.listFiles(srcDirFile, filter, null);
		
		// raise exception when the files were not found!
		if (files.isEmpty()) {
			throw new RuntimeException("File(s) \"" + filterString + "\" could not be found in directory \"" + srcDir + "\"." );
		}
		
		for (File file : files) {
			Msg.println("  - "+ file.getName());
			FileUtils.copyFileToDirectory(file, targetDirFile);
			cnt++;
		}

		// return number 
		return cnt;
	}

	
	
}
