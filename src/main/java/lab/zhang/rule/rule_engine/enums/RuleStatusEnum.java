package lab.zhang.rule.rule_engine.enums;

import lab.zhang.rule.rule_engine.config.RuleStatusConfig;
import lombok.Getter;

import java.util.*;

/**
 * Rule status enumeration
 * 
 * Note: According to project conventions, enumeration ID 0 should be UNDEFINED by default.
 * However, for RuleStatus, OFFLINE(0) is a valid business status and is used as the default
 * status in the database. This is an exception to the convention.
 * 
 * @author Rongjin Zhang
 */
@Getter
public enum RuleStatusEnum {
    
    /**
     * Offline - Not allowed to be added to any execution sequence for eventId
     * Note: This is ID 0, which is an exception to the convention that ID 0 should be UNDEFINED.
     * This is because OFFLINE is the default status in the database.
     */
    OFFLINE(0, "Offline"),
    
    /**
     * Test - Only test environment requests can trigger execution for eventId
     */
    TEST(1, "Test"),
    
    /**
     * Gray - Only gray environment requests can trigger execution for eventId
     */
    GRAY(2, "Gray"),
    
    /**
     * Online - Only production environment requests can trigger execution for eventId.
     * Traffic control (A/B test) is applied to ONLINE rules through rule groups.
     */
    ONLINE(3, "Online");
    
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
    RuleStatusEnum(Integer id, String description) {
        this.id = id;
        this.description = description;
    }

    /**
     * Get RuleStatusEnum by ID
     * 
     * @param id enumeration ID
     * @return RuleStatusEnum enum, or null if not found
     */
    public static RuleStatusEnum fromId(Integer id) {
        if (id == null) {
            return null;
        }
        for (RuleStatusEnum status : values()) {
            if (status.id.equals(id)) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * Check if a status transition from this status to the target status is allowed.
     * 
     * State transition rules are defined in RULE_STATUS_CHANGE_MAP:
     * 1. OFFLINE -> only TEST
     * 2. TEST -> GRAY, OFFLINE
     * 3. GRAY -> OFFLINE, ONLINE
     * 4. ONLINE -> OFFLINE
     * 
     * @param targetStatus the target status to transition to
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canChangeTo(RuleStatusEnum targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        
        // Same status is always allowed (no-op)
        if (this == targetStatus) {
            return true;
        }
        
        // Get allowed target statuses from configuration map
        Set<RuleStatusEnum> allowedTargetStatuses = RuleStatusConfig.RULE_STATUS_CHANGE_MAP.get(this);
        if (allowedTargetStatuses == null || allowedTargetStatuses.isEmpty()) {
            return false;
        }
        
        return allowedTargetStatuses.contains(targetStatus);
    }
    
    /**
     * Get all allowed target statuses for transition from this status.
     * 
     * @return set of allowed target statuses
     */
    public Set<RuleStatusEnum> getAllowedTargetStatuses() {
        Set<RuleStatusEnum> allowedTargetStatuses = RuleStatusConfig.RULE_STATUS_CHANGE_MAP.get(this);
        if (allowedTargetStatuses == null || allowedTargetStatuses.isEmpty()) {
            return EnumSet.noneOf(RuleStatusEnum.class);
        }
        return EnumSet.copyOf(allowedTargetStatuses);
    }
}

