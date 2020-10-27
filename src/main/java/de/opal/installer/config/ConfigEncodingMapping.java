package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigEncodingMapping {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String encoding;
	@Expose(serialize = true, deserialize = true)
	public String matchRegEx;

	
	public ConfigEncodingMapping(String encoding, String matchRegEx) {
		super();
		this.encoding = encoding;
		this.matchRegEx = matchRegEx;
	}


	@Override
	public String toString() {
		return "name: " + encoding + "; matchRegEx: " + matchRegEx ;
	}
	
	
}
