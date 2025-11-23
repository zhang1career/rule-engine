package lab.zhang.rule.rule_engine.dto;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import java.io.Serializable;

/**
 * Rule evaluation response DTO
 * 
 * @author rule-engine
 */
@Data
public class EvalResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Calculation result
     */
    private TypedValue result;
    
    /**
     * Whether successful
     */
    private Boolean success;
    
    /**
     * Error message
     */
    private String errorMessage;
    
    public EvalResponse() {
    }
    
    public EvalResponse(TypedValue result) {
        this.result = result;
        this.success = true;
    }
    
    public EvalResponse(String errorMessage) {
        this.success = false;
        this.errorMessage = errorMessage;
    }
}

