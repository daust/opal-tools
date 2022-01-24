/**
 * Wrapper for each sql file that needs to be installed
 */
package de.opal.installer.zip;

/**
 * @author daust
 *
 */
public class SqlFileWrapper {

	public String filenameAbsolute="";
	public String filenameZipFile="";
	public String connPoolName="";
	public String encoding="";

	/**
	 * 
	 */
	public SqlFileWrapper(String filenameAbsolute, String filenameZipFile, String connPoolName, String encoding) {
		this.filenameAbsolute=filenameAbsolute;
		this.filenameZipFile= filenameZipFile;
		this.connPoolName=connPoolName;
		this.encoding=encoding;		
	}

}
