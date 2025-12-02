package lab.zhang.rule.rule_engine.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

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
     * A/B Test - Only production environment requests can trigger execution for eventId,
     * distributed evenly based on userId
     */
    AB_TEST(3, "A/B Test"),
    
    /**
     * Full - Only production environment requests can trigger execution for eventId
     */
    FULL(4, "Full");
    
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
     * State transition rules:
     * 1. OFFLINE -> only TEST
     * 2. TEST -> GRAY, OFFLINE
     * 3. GRAY -> OFFLINE, AB_TEST
     * 4. AB_TEST -> OFFLINE, FULL
     * 5. FULL -> OFFLINE, AB_TEST
     * 
     * @param targetStatus the target status to transition to
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(RuleStatusEnum targetStatus) {
        if (targetStatus == null) {
            return false;
        }
        
        // Same status is always allowed (no-op)
        if (this == targetStatus) {
            return true;
        }
        
        switch (this) {
            case OFFLINE:
                // OFFLINE can only transition to TEST
                return targetStatus == TEST;
                
            case TEST:
                // TEST can transition to GRAY or OFFLINE
                return targetStatus == GRAY || targetStatus == OFFLINE;
                
            case GRAY:
                // GRAY can transition to OFFLINE or AB_TEST
                return targetStatus == OFFLINE || targetStatus == AB_TEST;
                
            case AB_TEST:
                // AB_TEST can transition to OFFLINE or FULL
                return targetStatus == OFFLINE || targetStatus == FULL;
                
            case FULL:
                // FULL can transition to OFFLINE or AB_TEST
                return targetStatus == OFFLINE || targetStatus == AB_TEST;
                
            default:
                return false;
        }
    }
    
    /**
     * Get all allowed target statuses for transition from this status.
     * 
     * @return set of allowed target statuses
     */
    public Set<RuleStatusEnum> getAllowedTargetStatuses() {
        switch (this) {
            case OFFLINE:
                return EnumSet.of(TEST);
                
            case TEST:
                return EnumSet.of(GRAY, OFFLINE);
                
            case GRAY:
                return EnumSet.of(OFFLINE, AB_TEST);
                
            case AB_TEST:
                return EnumSet.of(OFFLINE, FULL);
                
            case FULL:
                return EnumSet.of(OFFLINE, AB_TEST);
                
            default:
                return EnumSet.noneOf(RuleStatusEnum.class);
        }
    }
}

