package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class EncodingNLSMapping {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String encoding;
	@Expose(serialize = true, deserialize = true)
	public String NLS_LANG;
	
	@Override
	public String toString() {
		return "encoding: " + encoding + "; NLS_LANG: " + NLS_LANG;
	}
}
