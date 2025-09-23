package de.opal.utils;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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
	public static int copyDirectory(String srcDir, String targetDir, String filterString, String envDirective) throws IOException {
	    File srcDirFile = new File(srcDir).getCanonicalFile();
	    File targetDirFile = new File(targetDir);

	    int cnt = 0;

	    IOFileFilter filter = new WildcardFileFilter(filterString);

	    // Get all files and directories that match the filter
	    Collection<File> allItems = FileUtils.listFilesAndDirs(srcDirFile, filter, filter);
	    
	    // Filter out the source directory itself
	    List<File> itemsToProcess = allItems.stream()
	        .filter(file -> {
	            try {
	                return !file.getCanonicalPath().equals(srcDirFile.getCanonicalPath());
	            } catch (IOException e) {
	                // If we can't get canonical path, include the file anyway
	                return true;
	            }
	        })
	        .collect(Collectors.toList());

	    // raise exception when no items were found!
	    if (itemsToProcess.isEmpty()) {
	        throw new RuntimeException(
	            "File(s)/Directory(ies) \"" + filterString + "\" could not be found in directory \"" + srcDir + "\".");
	    }

	    for (File item : itemsToProcess) {
	        String newItemName = buildNewFileName(item.getName(), envDirective);
	        String itemType = item.isDirectory() ? "DIR" : "FILE";
	        Msg.println(" - " + itemType + ": " + item.getName() + " -> " + newItemName);
	        
	        if (item.isDirectory()) {
	            File newTargetDir = new File(targetDirFile, newItemName);
	            FileUtils.copyDirectory(item, newTargetDir);
	        } else {
	            File targetFile = new File(targetDirFile, newItemName);
	            FileUtils.copyFile(item, targetFile);
	        }

	        cnt++;
	    }

	    return cnt;
	}

	// Overloaded method for backward compatibility
	public static int copyDirectory(String srcDir, String targetDir, String filterString) throws IOException {
	    return copyDirectory(srcDir, targetDir, filterString, null);
	}

	private static String buildNewFileName(String fileName, String envDirective) {
	    //if (envDirective == null || envDirective.isEmpty()) {
	    //    // No environment directive, just add the default suffix
	    //    return addSuffixToFileName(fileName, "");
	    //}
	    
	    // Build the environment suffix from the directive
	    // Convert "int,prod" to "-env(int,prod)"
	    String envSuffix = "-env(" + envDirective.replace(" ", "") + ")";
	    
	    int lastDotIndex = fileName.lastIndexOf('.');
	    
	    if (lastDotIndex == -1) {
	        // No extension, just append suffix
	        return fileName + envSuffix;
	    } else {
	        // Insert suffix before the extension
	        String nameWithoutExtension = fileName.substring(0, lastDotIndex);
	        String extension = fileName.substring(lastDotIndex);
	        return nameWithoutExtension + envSuffix + extension;
	    }
	}

	private static String addSuffixToFileName(String fileName, String suffix) {
	    int lastDotIndex = fileName.lastIndexOf('.');
	    
	    if (lastDotIndex == -1) {
	        // No extension, just append suffix
	        return fileName + suffix;
	    } else {
	        // Insert suffix before the extension
	        String nameWithoutExtension = fileName.substring(0, lastDotIndex);
	        String extension = fileName.substring(lastDotIndex);
	        return nameWithoutExtension + suffix + extension;
	    }
	}

}
