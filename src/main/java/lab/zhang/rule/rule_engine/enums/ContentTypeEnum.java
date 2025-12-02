package lab.zhang.rule.rule_engine.enums;

import lombok.Getter;

/**
 * Content type enumeration
 * 
 * Note: According to project conventions, enumeration ID 0 should be UNDEFINED by default.
 * However, for RuleType, EXPRESSION(0) is a valid business type and is the first type.
 * This is an exception to the convention.
 * 
 * @author Rongjin Zhang
 */
@Getter
public enum ContentTypeEnum {
    
    /**
     * Logical expression
     * Note: This is ID 0, which is an exception to the convention that ID 0 should be UNDEFINED.
     * This is because EXPRESSION is the first valid rule type.
     */
    EXPRESSION(0, "Expression"),
    
    /**
     * API query
     */
    API_QUERY(1, "API Query"),
    
    /**
     * SQL query
     */
    SQL_QUERY(2, "SQL Query"),
    
    /**
     * Script execution (Groovy)
     */
    SCRIPT(3, "Script");
    
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
    ContentTypeEnum(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    /**
     * Get RuleTypeEnum by ID
     * 
     * @param id enumeration ID
     * @return RuleTypeEnum enum, or null if not found
     */
    public static ContentTypeEnum fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (ContentTypeEnum type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}

