package de.opal.installer.config;

import java.util.ArrayList;

import com.google.gson.annotations.Expose;

public class TextReplacement {
	// these parameters get exported / imported to the config file
	@Expose(serialize = true, deserialize = true)
	public String fileRegEx;
	@Expose(serialize = true, deserialize = true)
	public ArrayList<TextReplacementExpression> expressions;
	
	@Override
	public String toString() {
		return "fileRegEx: " + fileRegEx + "; expressions: " + expressions;
	}
}
