package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class PatchDependency {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String application;
	@Expose(serialize = true, deserialize = true)
	public String patch;
	@Expose(serialize = true, deserialize = true)
	public String referenceId;
	@Expose(serialize = true, deserialize = true)
	public String version;

	@Override
	public String toString() {
		return "application: " + application + "; patch: " + patch+ "; referenceId: " + referenceId + "; version: " + version;
	}
}
