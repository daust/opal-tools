package de.opal.exporter;

public class ObjectTypeMappingMetadata {

    /**
     * The names for object_types in user_objects / all_objects 
     * are different from the types used in DBMS_METADATA
	 *
     * @param type - object_type from data dictionary view USER_OBJECTS
     *
     * @return type name for using it in package DBMS_METADATA
     */
    public static String map2TypeForDBMS(String type) {
        if (type.contains("DATABASE LINK"))
            return "DB_LINK";
        if (type.equals("JOB"))
            return "PROCOBJ";
		if (type.equals("SCHEDULE"))
            return "PROCOBJ";
		if (type.equals("PROGRAM"))
            return "PROCOBJ";
		if (type.equals("PACKAGE"))
            return "PACKAGE_SPEC";
		
        return type.replace(" ", "_");
    }
}
