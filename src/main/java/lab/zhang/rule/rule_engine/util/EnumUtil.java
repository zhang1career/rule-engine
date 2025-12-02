package lab.zhang.rule.rule_engine.util;

import java.util.*;

/**
 * Enumeration utility class
 * Provides methods to convert between enumeration IDs and enumeration values
 * 
 * @author Rongjin Zhang
 */
public class EnumUtil {
    
    /**
     * Get enumeration value by ID
     * 
     * @param enumClass enumeration class
     * @param id enumeration ID
     * @param <T> enumeration type
     * @return enumeration value, or null if not found
     */
    public static <T extends Enum<T>> T fromId(Class<T> enumClass, Integer id) {
        if (id == null || enumClass == null) {
            return null;
        }
        
        try {
            // Try to find a static fromId method
            java.lang.reflect.Method fromIdMethod = enumClass.getMethod("fromId", Integer.class);
            @SuppressWarnings("unchecked")
            T result = (T) fromIdMethod.invoke(null, id);
            return result;
        } catch (Exception e) {
            // If fromId method doesn't exist, return null
            return null;
        }
    }
    
    /**
     * Get enumeration ID from enumeration value
     * 
     * @param enumValue enumeration value
     * @param <T> enumeration type
     * @return enumeration ID, or null if not found
     */
    public static <T extends Enum<T>> Integer getId(T enumValue) {
        if (enumValue == null) {
            return null;
        }
        
        try {
            // Try to find getId method
            java.lang.reflect.Method getIdMethod = enumValue.getClass().getMethod("getId");
            return (Integer) getIdMethod.invoke(enumValue);
        } catch (Exception e) {
            // If getId method doesn't exist, return null
            return null;
        }
    }
    
    /**
     * Get all enumeration values with their IDs
     * According to project conventions, returns a list of objects where each object has:
     * - "name": enumeration value (enum name)
     * - "value": enumeration ID
     * 
     * @param enumClass enumeration class name
     * @return list of objects with name and value for each enumeration value
     */
    public static List<Map<String, Object>> getEnumInfo(String enumClass) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        try {
            Class<?> clazz = Class.forName(enumClass);
            if (!clazz.isEnum()) {
                return result;
            }
            
            Object[] enumConstants = clazz.getEnumConstants();
            
            for (Object enumConstant : enumConstants) {
                Map<String, Object> item = new HashMap<>();
                
                // Get enumeration value (name)
                String enumName = ((Enum<?>) enumConstant).name();
                item.put("name", enumName);
                
                // Get enumeration ID (value)
                try {
                    java.lang.reflect.Method getIdMethod = clazz.getMethod("getId");
                    Integer id = (Integer) getIdMethod.invoke(enumConstant);
                    item.put("value", id);
                } catch (Exception e) {
                    // If getId method doesn't exist, use ordinal
                    item.put("value", ((Enum<?>) enumConstant).ordinal());
                }
                
                result.add(item);
            }
        } catch (ClassNotFoundException e) {
            // Class not found, return empty list
        }
        
        return result;
    }
    
    /**
     * Get enumeration information for multiple enumeration classes
     * According to project conventions, returns a map where:
     * - Key: enumeration class name
     * - Value: list of objects, each object has "name" (enumeration value) and "value" (enumeration ID)
     * 
     * @param enumClassNames comma-separated enumeration class names
     * @return map with enumeration class names as keys, and their list of name-value pairs as values
     */
    public static Map<String, List<Map<String, Object>>> getEnumInfoMap(String enumClassNames) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        
        if (enumClassNames == null || enumClassNames.trim().isEmpty()) {
            return result;
        }
        
        String[] names = enumClassNames.split(",");
        for (String name : names) {
            name = name.trim();
            if (name.isEmpty()) {
                continue;
            }
            
            // Try to resolve full class name
            String fullClassName = resolveEnumClassName(name);
            if (fullClassName != null) {
                List<Map<String, Object>> info = getEnumInfo(fullClassName);
                if (!info.isEmpty()) {
                    result.put(name, info);
                }
            }
        }
        
        return result;
    }
    
    /**
     * Resolve enumeration class name to full class name
     * 
     * @param className class name (short or full)
     * @return full class name, or null if not found
     */
    private static String resolveEnumClassName(String className) {
        // Common enumeration classes in this project
        Map<String, String> enumClassMap = new HashMap<>();
        // Support both old names (for backward compatibility) and new names
        enumClassMap.put("RuleStatus", "lab.zhang.rule.rule_engine.enums.RuleStatusEnum");
        enumClassMap.put("RuleStatusEnum", "lab.zhang.rule.rule_engine.enums.RuleStatusEnum");
        enumClassMap.put("RuleType", "lab.zhang.rule.rule_engine.enums.RuleTypeEnum");
        enumClassMap.put("RuleTypeEnum", "lab.zhang.rule.rule_engine.enums.RuleTypeEnum");
        enumClassMap.put("Environment", "lab.zhang.rule.rule_engine.enums.EnvironmentEnum");
        enumClassMap.put("EnvironmentEnum", "lab.zhang.rule.rule_engine.enums.EnvironmentEnum");
        enumClassMap.put("ExecutionItemTypeEnum", "lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum");
        enumClassMap.put("ItemType", "lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum"); // Alias for backward compatibility
        
        // If already full class name, return as is
        if (className.contains(".")) {
            return className;
        }
        
        // Try to find in map
        String fullName = enumClassMap.get(className);
        if (fullName != null) {
            return fullName;
        }
        
        // Try common package prefixes
        String[] prefixes = {
            "lab.zhang.rule.rule_engine.enums.",
            "lab.zhang.rule.rule_engine.engine."
        };
        
        for (String prefix : prefixes) {
            try {
                String fullClassName = prefix + className;
                Class.forName(fullClassName);
                return fullClassName;
            } catch (ClassNotFoundException e) {
                // Continue
            }
        }
        
        return null;
    }
}

