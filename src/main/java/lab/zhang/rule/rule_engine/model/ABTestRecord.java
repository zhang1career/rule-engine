package lab.zhang.rule.rule_engine.model;

import lombok.Data;

import java.io.Serializable;

/**
 * A/B test record entity class
 * Used to record the correspondence between userId, eventId and ruleId
 * 
 * @author rule-engine
 */
@Data
public class ABTestRecord implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * User ID
     */
    private Long userId;
    
    /**
     * Event type ID
     */
    private Integer eventId;
    
    /**
     * Rule ID
     */
    private Long ruleId;
    
    /**
     * Creation time
     */
    private Long createTime;
}

