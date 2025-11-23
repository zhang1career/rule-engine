package lab.zhang.rule.rule_engine.enums;

/**
 * Rule status enumeration
 * 
 * @author rule-engine
 */
public enum RuleStatus {
    
    /**
     * Offline - Not allowed to be added to any execution sequence for eventId
     */
    OFFLINE("Offline"),
    
    /**
     * Test - Only test environment requests can trigger execution for eventId
     */
    TEST("Test"),
    
    /**
     * A/B Test - Only production environment requests can trigger execution for eventId,
     * distributed evenly based on userId
     */
    AB_TEST("A/B Test"),
    
    /**
     * Full - Only production environment requests can trigger execution for eventId
     */
    FULL("Full");
    
    private final String description;
    
    RuleStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

