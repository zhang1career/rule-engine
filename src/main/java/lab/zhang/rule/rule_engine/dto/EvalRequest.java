package lab.zhang.rule.rule_engine.dto;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Rule evaluation request DTO
 * 
 * @author rule-engine
 */
@Data
public class EvalRequest implements Serializable {
    
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
     * Business request trace ID
     */
    private Long traceId;
    
    /**
     * Parameter dictionary for rule calculation
     */
    private Map<String, TypedValue> dataMap;
}

