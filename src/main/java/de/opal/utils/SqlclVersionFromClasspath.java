package de.opal.utils;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class SqlclVersionFromClasspath {
    
    public static class VersionInfo {
        private String version;
        private String jarPath;
        
        public VersionInfo(String version, String jarPath) {
            this.version = version;
            this.jarPath = jarPath;
        }
        
        public String getVersion() { return version; }
        public String getJarPath() { return jarPath; }
    }
    
    public static VersionInfo getSqlclVersionFromClasspath() {
        try {
            // Versuche dbtools-common.jar im Classpath zu finden
            ClassLoader classLoader = SqlclVersionFromClasspath.class.getClassLoader();
            
            // Suche nach einer Klasse aus dbtools-common.jar
            // Beispiel: oracle.dbtools.common.config.file.ConfigFile
            Class<?> dbtoolsClass = null;
            String[] possibleClasses = {
                "oracle.dbtools.common.config.file.ConfigFile",
                "oracle.dbtools.raptor.newscriptrunner.ScriptRunner",
                "oracle.dbtools.db.DBUtil"
            };
            
            for (String className : possibleClasses) {
                try {
                    dbtoolsClass = Class.forName(className);
                    break;
                } catch (ClassNotFoundException e) {
                    // Versuche nächste Klasse
                }
            }
            
            if (dbtoolsClass == null) {
                return new VersionInfo("dbtools-common.jar not found in classpath", null);
            }
            
            // Hole den Pfad zur JAR-Datei
            URL location = dbtoolsClass.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = URLDecoder.decode(location.getPath(), StandardCharsets.UTF_8);
            
            // Entferne "file:" prefix falls vorhanden
            if (jarPath.startsWith("file:")) {
                jarPath = jarPath.substring(5);
            }
            
            // Für Windows: entferne führenden Slash
            if (System.getProperty("os.name").toLowerCase().contains("win") 
                && jarPath.startsWith("/")) {
                jarPath = jarPath.substring(1);
            }
            
            File jarFile = new File(jarPath);
            
            if (!jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
                return new VersionInfo("Not running from JAR", jarPath);
            }
            
            // Lese Manifest
            JarFile jar = new JarFile(jarFile);
            Manifest manifest = jar.getManifest();
            
            String version = null;
            if (manifest != null) {
                // Versuche verschiedene Manifest-Attribute
                version = manifest.getMainAttributes().getValue("Implementation-Version");
                
                if (version == null) {
                    version = manifest.getMainAttributes().getValue("Bundle-Version");
                }
                
                if (version == null) {
                    version = manifest.getMainAttributes().getValue("Specification-Version");
                }
                if (version == null) {
                    version = manifest.getMainAttributes().getValue("Implementation-Build");
                }
                if (version == null) {
                    version = manifest.getMainAttributes().getValue("Build-Timestamp");
                }
            }
            
            jar.close();
            
            return new VersionInfo(
                version != null ? version : "Version not found in manifest",
                jarFile.getAbsolutePath()
            );
            
        } catch (Exception e) {
            e.printStackTrace();
            return new VersionInfo("Error: " + e.getMessage(), null);
        }
    }
    
    public static void main(String[] args) {
        VersionInfo info = getSqlclVersionFromClasspath();
        
        System.out.println("SQLcl Version Information:");
        System.out.println("==========================");
        System.out.println("Version: " + info.getVersion());
        System.out.println("JAR Path: " + info.getJarPath());
    }
}