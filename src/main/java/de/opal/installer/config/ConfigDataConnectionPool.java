package de.opal.installer.config;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;

public class ConfigDataConnectionPool {

	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String targetSystem = ""; // e.g. DEVELOPMENT,INTEGRATION,PRODUCTION
	@Expose(serialize = true, deserialize = true)
	public ArrayList<ConfigConnectionPool> connectionPools;

	@Override
	public String toString() {
		return "target system: " + targetSystem + (connectionPools == null ? "" : "; " + connectionPools.toString());
	}
	
    public ConfigDataConnectionPool() {
    	if (connectionPools==null)
    		connectionPools=new ArrayList<ConfigConnectionPool>();
    }
	
}
