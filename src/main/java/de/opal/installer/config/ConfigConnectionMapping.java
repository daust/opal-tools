package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigConnectionMapping {
	
	public ConfigConnectionMapping(String connectionPoolName, String matchRegEx, String description) {
		super();
		this.connectionPoolName = connectionPoolName;
		this.matchRegEx = matchRegEx;
		this.description=description;
	}


	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String connectionPoolName;
	@Expose(serialize = true, deserialize = true)
	public String matchRegEx;
	@Expose(serialize = true, deserialize = true)
	public String description;

	
	@Override
	public String toString() {
		return "name: " + connectionPoolName + "; matchRegEx: " + matchRegEx+ "; description: " + description ;
	}
	
	
}
