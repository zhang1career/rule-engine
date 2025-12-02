package lab.zhang.rule.rule_engine.enums;

import lombok.Getter;

/**
 * Environment type enumeration
 * 
 * @author Rongjin Zhang
 */
@Getter
public enum EnvironmentEnum {
    
    /**
     * Undefined - Default value, not used in business logic
     */
    UNDEFINED(0, "Undefined"),
    
    /**
     * Test environment
     */
    TEST(1, "Test Environment"),
    
    /**
     * Production environment
     */
    PRODUCTION(2, "Production Environment"),
    
    /**
     * Gray environment
     */
    GRAY(3, "Gray Environment");
    
    /**
     * Enumeration ID (stored in database)
     * -- GETTER --
     *  Get enumeration ID
     */
    private final Integer id;
    
    /**
     * Description
     * -- GETTER --
     *  Get description
     */
    private final String description;
    
    /**
     * Constructor
     * 
     * @param id enumeration ID
     * @param description description
     */
    EnvironmentEnum(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    /**
     * Get EnvironmentEnum by ID
     * 
     * @param id enumeration ID
     * @return EnvironmentEnum enum, or null if not found
     */
    public static EnvironmentEnum fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (EnvironmentEnum env : values()) {
            if (env.id.equals(id)) {
                return env;
            }
        }
        return null;
    }
}

