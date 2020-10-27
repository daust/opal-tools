package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigConnectionMapping {
	
	public ConfigConnectionMapping(String connectionPoolName, String matchRegEx) {
		super();
		this.connectionPoolName = connectionPoolName;
		this.matchRegEx = matchRegEx;
	}


	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String connectionPoolName;
	@Expose(serialize = true, deserialize = true)
	public String matchRegEx;

	
	@Override
	public String toString() {
		return "name: " + connectionPoolName + "; matchRegEx: " + matchRegEx ;
	}
	
	
}
