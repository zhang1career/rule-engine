package lab.zhang.rule.rule_engine.pojo.dto;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution trace information
 * Records information about executed rules
 * Only rules that have been executed are recorded in the trace
 *
 * @author Rongjin Zhang
 */
@Data
public class ExecutionTraceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * List of execution steps
     */
    private List<ExecutionStepDTO> steps = new ArrayList<>();

    /**
     * Execution step information
     * Execution order is implied by the position in the steps list
     */
    @Data
    public static class ExecutionStepDTO implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Rule ID
         */
        private Long ruleId;

        /**
         * Rule group ID (0 if standalone rule, non-zero if rule belongs to a group)
         */
        private Long groupId;

        /**
         * Execution result
         */
        private TypedValue result;

        /**
         * Error message (if execution failed)
         */
        private String errmsg;
    }
}

