package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigEncodingMapping {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String encoding;
	@Expose(serialize = true, deserialize = true)
	public String fileRegex;
	@Expose(serialize = true, deserialize = true)
	public String description;
	
	public ConfigEncodingMapping(String encoding, String fileRegex, String description) {
		super();
		this.encoding = encoding;
		this.fileRegex = fileRegex;
		this.description=description;
	}


	@Override
	public String toString() {
		return "name: " + encoding + "; fileRegex: " + fileRegex + "; description: " + description;
	}
	
	
}
