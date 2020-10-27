package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class ConfigConnectionPool {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String name;
	@Expose(serialize = true, deserialize = true)
	public String user;
	@Expose(serialize = true, deserialize = true)
	public String password;
	@Expose(serialize = true, deserialize = true)
	public String connectString;
	
	@Override
	public String toString() {
		return "name: " + name + "; user: " + user+ "; password: " + password+ "; connectString: " + connectString ;
	}
	
	public ConfigConnectionPool( String name, String user, String password, String connectString) {
		this.user=user;
		this.name=name;
		this.password=password;
		this.connectString=connectString;
	}
}
