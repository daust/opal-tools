package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigEncodingMapping {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String encoding;
	@Expose(serialize = true, deserialize = true)
	public String fileRegex;
	@Expose(serialize = true, deserialize = true)
	public String fileFilter;
	@Expose(serialize = true, deserialize = true)
	public String description;

	public ConfigEncodingMapping(String encoding, String fileRegex, String fileFilter, String description) {
		super();
		this.encoding = encoding;
		this.fileRegex = fileRegex;
		this.fileFilter = fileFilter;
		this.description = description;
	}

	@Override
	public String toString() {
		return "name: " + encoding + "; fileRegex: " + fileRegex + "; fileFilter: " + fileFilter + "; description: "
				+ description;
	}

}
