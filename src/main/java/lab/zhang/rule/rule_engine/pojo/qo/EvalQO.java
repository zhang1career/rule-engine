package lab.zhang.rule.rule_engine.pojo.qo;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.io.Serializable;
import java.util.Map;

/**
 * Rule evaluation request object
 * 
 * @author Rongjin Zhang
 */
@Data
public class EvalQO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * User ID
     */
    @NotNull(message = "User ID cannot be null")
    @Positive(message = "User ID must be positive")
    private Long userId;
    
    /**
     * Event ID (unsigned long integer)
     */
    @NotNull(message = "Event ID cannot be null")
    @Positive(message = "Event ID must be positive")
    private Long eventId;
    
    /**
     * Business request trace ID
     */
    @NotNull(message = "Trace ID cannot be null")
    private Long traceId;
    
    /**
     * Parameter dictionary for rule calculation
     */
    @NotNull(message = "Arguments cannot be null")
    private Map<String, TypedValue> arguments;
}

