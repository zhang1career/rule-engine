package lab.zhang.rule.rule_engine.pojo.dto;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lombok.Data;

import java.io.Serializable;

/**
 * Rule evaluation response DTO
 *
 * @author Rongjin Zhang
 */
@Data
public class EvalResultDTO implements Serializable {

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
    private String errmsg;

    /**
     * Execution trace information (execution process details)
     */
    private ExecutionTrace trace;

    public EvalResultDTO() {
    }

    public EvalResultDTO(TypedValue result) {
        this.result = result;
        this.success = true;
    }

    public EvalResultDTO(TypedValue result, ExecutionTrace trace) {
        this.result = result;
        this.success = true;
        this.trace = trace;
    }

    public EvalResultDTO(String errmsg) {
        this.success = false;
        this.errmsg = errmsg;
    }
}

