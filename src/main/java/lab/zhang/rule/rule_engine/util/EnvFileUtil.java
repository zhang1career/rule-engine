package lab.zhang.rule.rule_engine.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for reading .env file
 * 
 * @author Rongjin Zhang
 */
@Slf4j
public final class EnvFileUtil {

    private EnvFileUtil() {
    }

    /**
     * Read .env file and return a map of key-value pairs
     * 
     * @param envFilePath path to .env file
     * @return map of environment variables
     */
    public static Map<String, String> readEnvFile(String envFilePath) {
        Map<String, String> envMap = new HashMap<>();
        File envFile = new File(envFilePath);
        
        if (!envFile.exists() || !envFile.isFile()) {
            log.warn(".env file not found at: {}", envFilePath);
            return envMap;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse key=value format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    
                    // Remove quotes if present
                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    envMap.put(key, value);
                } else {
                    log.warn("Invalid line format at line {}: {}", lineNumber, line);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read .env file: {}", envFilePath, e);
        }
        
        return envMap;
    }

    /**
     * Read .env file from project root directory
     * 
     * @return map of environment variables
     */
    public static Map<String, String> readEnvFileFromProjectRoot() {
        String projectRoot = System.getProperty("user.dir");
        String envFilePath = Paths.get(projectRoot, ".env").toString();
        return readEnvFile(envFilePath);
    }

    /**
     * Get JAVA_OPTS from .env file
     * 
     * @param envFilePath path to .env file
     * @return JAVA_OPTS value or null if not found
     */
    public static String getJavaOpts(String envFilePath) {
        Map<String, String> envMap = readEnvFile(envFilePath);
        return envMap.get("JAVA_OPTS");
    }

    /**
     * Get JAVA_OPTS from .env file in project root
     * 
     * @return JAVA_OPTS value or null if not found
     */
    public static String getJavaOptsFromProjectRoot() {
        Map<String, String> envMap = readEnvFileFromProjectRoot();
        return envMap.get("JAVA_OPTS");
    }
}

