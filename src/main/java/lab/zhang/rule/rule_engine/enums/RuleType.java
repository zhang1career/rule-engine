package lab.zhang.rule.rule_engine.enums;

/**
 * Rule type enumeration
 * 
 * @author rule-engine
 */
public enum RuleType {
    
    /**
     * Logical expression
     */
    EXPRESSION("Expression"),
    
    /**
     * API query
     */
    API_QUERY("API Query"),
    
    /**
     * SQL query
     */
    SQL_QUERY("SQL Query"),
    
    /**
     * Script execution (Groovy)
     */
    SCRIPT("Script");
    
    private final String description;
    
    RuleType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

