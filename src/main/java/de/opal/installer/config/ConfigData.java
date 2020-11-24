package de.opal.installer.config;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;

public class ConfigData {

	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String application;
	@Expose(serialize = true, deserialize = true)
	public String patch;
	@Expose(serialize = true, deserialize = true)
	public String author;
	@Expose(serialize = true, deserialize = true)
	public String version;
	@Expose(serialize = true, deserialize = true)
	public String sqlDir = "sql";
	@Expose(serialize = true, deserialize = true)
	public String traversalType = "INORDER";
	@Expose(serialize = true, deserialize = true)
	public ArrayList<ConfigConnectionMapping> connectionMappings;
	@Expose(serialize = false, deserialize = true)
	public ArrayList<String> staticFiles;
	@Expose(serialize = true, deserialize = true)
	public String sqlFileRegEx = "\\.(sql|pks|pkb)$";
	@Expose(serialize = true, deserialize = true)
	public String waitAfterEachStatement = "false";
	@Expose(serialize = false, deserialize = true)
	public String runMode = "EXECUTE"; // EXECUTE | VALIDATE_ONLY
	@Expose(serialize = true, deserialize = true)
	public ArrayList<RegistryTarget> registryTargets;
	@Expose(serialize = true, deserialize = true)
	public ArrayList<ConfigEncodingMapping> encodingMappings;
	@Expose(serialize = true, deserialize = true)
	public ArrayList<PatchDependency> dependencies;

	// settings only read but not written back
	// upon writing back ... we could introduce absolute paths
	// we assume per default that the .json file is in the top level directory of
	// the package
	@Expose(serialize = false, deserialize = true)
	public String packageDir = ".";

	// internal values, should not be serialized
//	@Expose(serialize = false, deserialize = false)
//	private String internalValue = "";

	@Override
	public String toString() {
		return "Application: " + application + "; Patch: " + patch + "; Author: " + author + "; Version: " + version + "; sqlDir: " + sqlDir
				+ "; traversalType: " + traversalType + "; staticFiles: "
				+ (staticFiles == null ? "" : staticFiles.toString()) + "; connections: "
				+ (connectionMappings == null ? "" : connectionMappings.toString()) + "; sqlFileRegEx: "
				+ (encodingMappings == null ? "" : encodingMappings.toString()) 
				+ (sqlFileRegEx == null ? "" : sqlFileRegEx.toString()) + "; connectionPools: "
				+ "; waitAfterEachStatement: "
				+ (waitAfterEachStatement == null ? "" : waitAfterEachStatement.toString())
				+ "; patchDependencies: "
				+ (dependencies == null ? "" : dependencies.toString());
	}
}
