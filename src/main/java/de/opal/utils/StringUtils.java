package de.opal.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
  public static String extractSchemaFromUserName(String userName) {
    String patternString = ".*\\[(.*)\\]";
    if (userName.contains("[")) {
      Pattern pattern = Pattern.compile(patternString);
      Matcher matcher = pattern.matcher(userName);
      while (matcher.find())
        userName = matcher.group(1); 
    } 
    return userName;
  }
}
