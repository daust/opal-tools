package de.opal.installer.config;

import com.google.gson.annotations.Expose;

public class TextReplacementExpression {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String regEx;
	@Expose(serialize = true, deserialize = true)
	public String value;
	
	@Override
	public String toString() {
		return "regEx: " + regEx + "; value: " + value;
	}
}
