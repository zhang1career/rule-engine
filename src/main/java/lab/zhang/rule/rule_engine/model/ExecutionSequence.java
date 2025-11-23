package lab.zhang.rule.rule_engine.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Execution sequence entity class
 * 
 * @author rule-engine
 */
@Data
public class ExecutionSequence implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Event type ID
     */
    private Integer eventId;
    
    /**
     * Rule ID list (arranged in execution order)
     */
    private List<Long> ruleIds;
    
    /**
     * Execution sequence name
     */
    private String sequenceName;
    
    /**
     * Execution sequence description
     */
    private String description;
}

