package de.opal.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.RandomStringUtils;

public class EncryptorWrapper {
	private static final String key2 = "FinxNA1Q#tEIFCAJ";

	// if it is already encrypted, then do nothing
	// if it is NOT already encrypted, then use method 1:
	// empty passwords count as already encrypted
	public String encryptPWD(String pwd, String encryptionKeyFilename) {
		// check existence of key file
		// if not exists, create and generate random key
		File encryptionKeyFile = new File(encryptionKeyFilename);

		int length = 32;
		boolean useLetters = true;
		boolean useNumbers = false;
		String randomKey = RandomStringUtils.random(length, useLetters, useNumbers);

		try {
			if (!encryptionKeyFile.exists()) {
				org.apache.commons.io.FileUtils.writeStringToFile(encryptionKeyFile, randomKey, StandardCharsets.UTF_8);
			}
			// read file into
			randomKey = org.apache.commons.io.FileUtils.readFileToString(encryptionKeyFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException("File " + encryptionKeyFilename + " could not be accessed.");
		}

		if (pwd != null) {
			pwd = pwd.trim();
			if (pwd.startsWith("1:")) {
				// password is encrypted with method 1 ... do nothing
			} else {
				// not encrypted => ENCRYPT!
				pwd = "1:" + Encryptor.encrypt(randomKey, key2, pwd);
			}
		}

		return pwd;
	}

	public String decryptPWD(String pwd, String encryptionKeyFilename) {
		pwd = pwd.trim();
		if (pwd != null && !pwd.equals("")) {
			if (pwd.startsWith("1:")) {

				File encryptionKeyFile = new File(encryptionKeyFilename);
				String randomKey = "";

				try {
					// read file into
					randomKey = org.apache.commons.io.FileUtils.readFileToString(encryptionKeyFile,
							StandardCharsets.UTF_8);
				} catch (IOException e) {
					throw new RuntimeException("File " + encryptionKeyFilename + " could not be accessed.");
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

	public Boolean isEncrypted(String pwd) {
		Boolean isEncrypted = false;

		if (pwd != null && !pwd.equals("")) {
			if (pwd.startsWith("1:")) {
				isEncrypted = true;
			} else {
				// not encrypted => do nothing
			}
		} else {
			// blank passwords don't have to be encrypted
			isEncrypted = true;
		}

		return isEncrypted;
	}

}
