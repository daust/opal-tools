package de.oc.utils;

import java.net.UnknownHostException;

public class EncryptorWrapper {
	private static final String key2 = "ThisIsASecretKet";
	
	private static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

//	private static String padLeft(String s, int n) {
//	    return String.format("%1$" + n + "s", s);  
//	}

	// if it is already encrypted, then do nothing
	// if it is NOT already encrypted, then use method 1:
	public String encryptPWD(String pwd) {
		pwd = pwd.trim();
		
		if (pwd.startsWith("1:")) {
			// password is encrypted with method 1 ... do nothing
		} else {
			// not encrypted => ENCRYPT!
			pwd = "1:" + Encryptor.encrypt(getKey1(), key2, pwd);
		}
		
		return pwd;
	}

	public String decryptPWD(String pwd) {

		if (pwd != null){
			if (pwd.startsWith("1:")) {
				// password is encrypted with method 1
				String encPwd = pwd.substring("1:".length());
				pwd = Encryptor.decrypt(getKey1(), key2, encPwd);
			} else {
				// not encrypted => do nothing
			}			
		}
		
		return pwd;
	}

	
	private String getKey1() {
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
