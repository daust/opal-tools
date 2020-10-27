package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class RegistryTarget {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String connectionPoolName;
	@Expose(serialize = true, deserialize = true)
	public String tablePrefix;
	
	@Override
	public String toString() {
		return "connectionPoolName: " + connectionPoolName + "; tablePrefix: " + tablePrefix;
	}
}
