package lab.zhang.rule.rule_engine.enums;

/**
 * Environment type enumeration
 * 
 * @author rule-engine
 */
public enum Environment {
    
    /**
     * Test environment
     */
    TEST("Test Environment"),
    
    /**
     * Production environment
     */
    PRODUCTION("Production Environment");
    
    private final String description;
    
    Environment(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

