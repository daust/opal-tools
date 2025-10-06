package de.opal.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.opal.installer.util.Msg;

/**
 * Utility class to retrieve and display Oracle database version information
 * including Oracle Database, APEX, and ORDS versions.
 */
public class DatabaseVersionUtil {

    private static final Logger log = LogManager.getLogger(DatabaseVersionUtil.class.getName());

    // SQL queries for version information
    private static final String ORACLE_DB_VERSION_SQL = 
            "SELECT 'Database: ' || SYS_CONTEXT('USERENV', 'DB_NAME') || " +
            "       '; Instance: ' || SYS_CONTEXT('USERENV', 'INSTANCE_NAME') || " +
            "       '; Container: ' || SYS_CONTEXT('USERENV', 'CON_NAME') || " +
            "       '; Host: ' || SYS_CONTEXT('USERENV', 'SERVER_HOST') || " +
            "       '; \n\t\t  Banner: ' || (SELECT replace(BANNER_FULL,chr(10),' ') FROM V$VERSION WHERE BANNER LIKE 'Oracle%') AS connection_banner " +
            "FROM DUAL";

    private static final String APEX_VERSION_SQL = 
        "SELECT '' || version_no || '; API Compatibility: ' || api_compatibility || " +
        "       '; Patch: ' || patch_applied AS version_banner FROM APEX_RELEASE";

    private static final String ORDS_VERSION_SQL = 
        "SELECT '' || VERSION || " +
        "       '; Updated: ' || TO_CHAR(UPDATED_ON, 'YYYY-MM-DD') AS ords_banner " +
        "FROM ORDS_METADATA.ORDS_VERSION";

    /**
     * Displays complete version information for Oracle Database, APEX, and ORDS
     * @param conn Database connection
     */
    public static void displayVersionInfo(Connection conn) {
        if (conn == null) {
            Msg.println("    No database connection available for version check");
            return;
        }

        try {
            String oracleVersion = getOracleVersion(conn);
            String apexVersion = getApexVersion(conn);
            String ordsVersion = getOrdsVersion(conn);

            Msg.println("    Oracle RDBMS: " + oracleVersion);
            Msg.println("    Oracle APEX : " + apexVersion);
            Msg.println("    Oracle ORDS : " + ordsVersion);

        } catch (Exception e) {
            log.error("Error retrieving version information", e);
            Msg.println("    Error retrieving version information: " + e.getMessage());
        }
    }

    /**
     * Gets Oracle Database version and connection information
     * @param conn Database connection
     * @return Oracle version banner string
     */
    public static String getOracleVersion(Connection conn) {
        if (conn == null) {
            return "No connection available";
        }

        try (PreparedStatement stmt = conn.prepareStatement(ORACLE_DB_VERSION_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getString("connection_banner");
            }
            return "Unable to retrieve Oracle version";
            
        } catch (SQLException e) {
            log.debug("Error getting Oracle version", e);
            return "Error retrieving Oracle version: " + e.getMessage();
        }
    }

    /**
     * Gets Oracle APEX version information
     * @param conn Database connection
     * @return APEX version banner string or "Not installed" if APEX is not available
     */
    public static String getApexVersion(Connection conn) {
        if (conn == null) {
            return "No connection available";
        }

        try (PreparedStatement stmt = conn.prepareStatement(APEX_VERSION_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getString("version_banner");
            }
            return "Not installed";
            
        } catch (SQLException e) {
            log.debug("APEX not available or error accessing APEX_RELEASE", e);
            return "Not installed";
        }
    }

    /**
     * Gets Oracle ORDS version information
     * @param conn Database connection
     * @return ORDS version banner string or "Not installed" if ORDS is not available
     */
    public static String getOrdsVersion(Connection conn) {
        if (conn == null) {
            return "No connection available";
        }

        try (PreparedStatement stmt = conn.prepareStatement(ORDS_VERSION_SQL);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getString("ords_banner");
            }
            return "Not installed";
            
        } catch (SQLException e) {
            log.debug("ORDS not available or error accessing ORDS_METADATA.ORDS_VERSION", e);
            return "Not installed";
        }
    }

    /**
     * Gets a compact version summary for display
     * @param conn Database connection
     * @return Compact version information string
     */
    public static String getCompactVersionInfo(Connection conn) {
        if (conn == null) {
            return "No connection available";
        }

        StringBuilder sb = new StringBuilder();
        
        try {
            // Get just the database name and container for compact display
            String dbInfo = getOracleVersion(conn);
            if (dbInfo != null && !dbInfo.startsWith("Error") && !dbInfo.equals("No connection available")) {
                // Extract just DB name and container from the full banner
                if (dbInfo.contains("Database: ") && dbInfo.contains("; Container: ")) {
                    String dbName = extractBetween(dbInfo, "Database: ", ";");
                    String container = extractBetween(dbInfo, "Container: ", ";");
                    if (container == null) container = extractAfter(dbInfo, "Container: ");
                    sb.append(dbName).append("/").append(container);
                } else {
                    sb.append("Oracle DB");
                }
            }

            String apexVersion = getApexVersion(conn);
            if (apexVersion != null && !apexVersion.equals("Not installed")) {
                String version = extractBetween(apexVersion, "Version: ", ";");
                if (version != null) {
                    sb.append("; APEX ").append(version);
                }
            }

            String ordsVersion = getOrdsVersion(conn);
            if (ordsVersion != null && !ordsVersion.equals("Not installed")) {
                String version = extractBetween(ordsVersion, "ORDS Version: ", ";");
                if (version != null) {
                    sb.append("; ORDS ").append(version);
                }
            }

        } catch (Exception e) {
            log.debug("Error creating compact version info", e);
            return "Version info unavailable";
        }

        return sb.length() > 0 ? sb.toString() : "Version info unavailable";
    }

    // Helper methods for string extraction
    private static String extractBetween(String source, String start, String end) {
        if (source == null || start == null || end == null) return null;
        int startIdx = source.indexOf(start);
        if (startIdx == -1) return null;
        startIdx += start.length();
        int endIdx = source.indexOf(end, startIdx);
        if (endIdx == -1) return null;
        return source.substring(startIdx, endIdx).trim();
    }

    private static String extractAfter(String source, String start) {
        if (source == null || start == null) return null;
        int startIdx = source.indexOf(start);
        if (startIdx == -1) return null;
        return source.substring(startIdx + start.length()).trim();
    }
}