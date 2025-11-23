package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Rule execution context
 * Used to pass and share data during rule execution
 * 
 * @author rule-engine
 */
@Data
public class RuleExecutionContext implements Serializable {
    
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
     * Trace ID
     */
    private Long traceId;
    
    /**
     * Input parameter dictionary
     */
    private Map<String, TypedValue> dataMap;
    
    /**
     * Variable storage during execution (for data transfer between rules)
     */
    private Map<String, TypedValue> variables;
    
    /**
     * Current execution depth (for recursion control)
     */
    private Integer executionDepth;
    
    /**
     * Maximum execution depth (to prevent infinite recursion)
     */
    private static final int MAX_EXECUTION_DEPTH = 100;
    
    public RuleExecutionContext() {
        this.variables = new HashMap<>();
        this.executionDepth = 0;
    }
    
    public RuleExecutionContext(Long userId, Integer eventId, Long traceId, 
                               Map<String, TypedValue> dataMap) {
        this.userId = userId;
        this.eventId = eventId;
        this.traceId = traceId;
        this.dataMap = dataMap != null ? dataMap : new HashMap<>();
        this.variables = new HashMap<>();
        this.executionDepth = 0;
    }
    
    /**
     * Increment execution depth
     */
    public void incrementDepth() {
        this.executionDepth++;
        if (this.executionDepth > MAX_EXECUTION_DEPTH) {
            throw new RuntimeException("Maximum execution depth exceeded: " + MAX_EXECUTION_DEPTH);
        }
    }
    
    /**
     * Decrement execution depth
     */
    public void decrementDepth() {
        if (this.executionDepth > 0) {
            this.executionDepth--;
        }
    }
    
    /**
     * Set variable
     */
    public void setVariable(String key, TypedValue value) {
        this.variables.put(key, value);
    }
    
    /**
     * Get variable
     */
    public TypedValue getVariable(String key) {
        return this.variables.get(key);
    }
}

