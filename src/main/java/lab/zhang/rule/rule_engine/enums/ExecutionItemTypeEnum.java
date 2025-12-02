package lab.zhang.rule.rule_engine.enums;

/**
 * Execution item type enumeration
 * 
 * Note: According to project conventions, enumeration ID 0 should be UNDEFINED by default.
 * However, for ExecutionItemTypeEnum, RULE(0) is a valid business type and is the first type.
 * This is an exception to the convention.
 * 
 * @author Rongjin Zhang
 */
public enum ExecutionItemTypeEnum {
    
    /**
     * Single rule
     * Note: This is ID 0, which is an exception to the convention that ID 0 should be UNDEFINED.
     * This is because RULE is the first valid execution item type.
     */
    RULE(0, "Rule"),
    
    /**
     * Rule group
     */
    RULE_GROUP(1, "Rule Group");
    
    /**
     * Enumeration ID (stored in database)
     */
    private final Integer id;
    
    /**
     * Description
     */
    private final String description;
    
    /**
     * Constructor
     * 
     * @param id enumeration ID
     * @param description description
     */
    ExecutionItemTypeEnum(Integer id, String description) {
        this.id = id;
        this.description = description;
    }
    
    /**
     * Get enumeration ID
     * 
     * @return enumeration ID
     */
    public Integer getId() {
        return id;
    }
    
    /**
     * Get description
     * 
     * @return description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get ExecutionItemTypeEnum by ID
     * 
     * @param id enumeration ID
     * @return ExecutionItemTypeEnum enum, or null if not found
     */
    public static ExecutionItemTypeEnum fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (ExecutionItemTypeEnum type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
