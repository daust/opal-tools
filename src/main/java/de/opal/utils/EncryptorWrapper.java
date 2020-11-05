package de.opal.utils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.RandomStringUtils;

public class EncryptorWrapper {
	private static final String key2 = "FinxNA1Q#tEIFCAJ";
	
	private static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

//	private static String padLeft(String s, int n) {
//	    return String.format("%1$" + n + "s", s);  
//	}

	// if it is already encrypted, then do nothing
	// if it is NOT already encrypted, then use method 1:
	public String encryptPWD(String pwd, String encryptionKeyFilename) {
		// check existence of key file
		// if not exists, create and generate random key
		File encryptionKeyFile=new File(encryptionKeyFilename);
		
		int length = 32;
	    boolean useLetters = true;
	    boolean useNumbers = false;
	    String randomKey = RandomStringUtils.random(length, useLetters, useNumbers);
			
		try {
			if (!encryptionKeyFile.exists()) {
				org.apache.commons.io.FileUtils.writeStringToFile(encryptionKeyFile, randomKey, StandardCharsets.UTF_8);
			}
			// read file into
			randomKey=org.apache.commons.io.FileUtils.readFileToString(encryptionKeyFile, StandardCharsets.UTF_8);			
		} catch(IOException e) {
			throw new RuntimeException("File "+encryptionKeyFilename+ " could not be accessed.");
		}
				
		pwd = pwd.trim();
		if (pwd.startsWith("1:")) {
			// password is encrypted with method 1 ... do nothing
		} else {
			// not encrypted => ENCRYPT!
			pwd = "1:" + Encryptor.encrypt(randomKey, key2, pwd);
		}
		
		
		return pwd;
	}

	public String decryptPWD(String pwd, String encryptionKeyFilename) {
		pwd = pwd.trim();
		if (pwd != null){
			if (pwd.startsWith("1:")) {
				
				File encryptionKeyFile=new File(encryptionKeyFilename);
				String randomKey="";
				
				try {
					// read file into
					randomKey=org.apache.commons.io.FileUtils.readFileToString(encryptionKeyFile, StandardCharsets.UTF_8);			
				} catch(IOException e) {
					throw new RuntimeException("File "+encryptionKeyFilename+ " could not be accessed.");
				}
				
				// password is encrypted with method 1
				String encPwd = pwd.substring("1:".length());
				pwd = Encryptor.decrypt(randomKey, key2, encPwd);
			} else {
				// not encrypted => do nothing
			}			
		}
		
		return pwd;
	}

	
	private String getKeyIPAddress() {
		String key1 = "";
		String localhostname = "";

		try {
			localhostname = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
		}
		
		if (localhostname.length()>16){
			localhostname = localhostname.substring(0,16);
		}
		
		key1 = padRight(localhostname,16).replace(" ", "|");
		//_logger.info("key1: #"+key1+"#");
		
		return key1;
	}

	
    public Boolean isEncrypted(String pwd) {
    	Boolean isEncrypted=false;
    	
    	if (pwd != null){
			if (pwd.startsWith("1:")) {
				isEncrypted=true;
			} else {
				// not encrypted => do nothing
			}			
		}
    	
    	return isEncrypted;
    }

	

}
