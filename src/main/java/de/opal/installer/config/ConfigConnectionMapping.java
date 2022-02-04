package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigConnectionMapping {

	public ConfigConnectionMapping(String connectionPoolName, String fileRegex, String fileFilter, String description) {
		super();
		this.connectionPoolName = connectionPoolName;
		this.fileRegex = fileRegex;
		this.fileFilter = fileFilter;
		this.description = description;
	}

	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String connectionPoolName;
	@Expose(serialize = true, deserialize = true)
	public String fileRegex;
	@Expose(serialize = true, deserialize = true)
	public String fileFilter;
	@Expose(serialize = true, deserialize = true)
	public String description;

	@Override
	public String toString() {
		return "name: " + connectionPoolName + "; fileRegex: " + fileRegex + "; fileFilter: " + fileFilter
				+ "; description: " + description;
	}
}
