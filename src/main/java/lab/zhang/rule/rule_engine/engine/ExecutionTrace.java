package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.model.EvalRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Execution trace information
 * Records information about executed rules
 * Only rules that have been executed are recorded in the trace
 *
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Business request trace ID
     */
    private BigInteger traceId;

    /**
     * User ID
     */
    private Long userId;

    /**
     * Event ID (unsigned long integer)
     */
    private Long eventId;

    /**
     * Parameter dictionary for rule calculation
     */
    private Map<String, TypedValue> arguments;

    /**
     * List of execution steps
     * Each step represents an executed rule
     * The last element in the list represents the last executed rule
     */
    @Builder.Default
    private List<ExecutionStep> steps = new ArrayList<>();


    public ExecutionTrace(@NotNull EvalRequest request, BigInteger traceId) {
        this.traceId = traceId;
        this.userId = request.getUserId();
        this.eventId = request.getEventId();
        this.arguments = request.getArguments();
    }

    /**
     * Add an execution step
     * Only executed rules should be added to the trace
     *
     * @param step execution step to add
     */
    public void addStep(ExecutionStep step) {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        steps.add(step);
    }

    /**
     * Execution step information
     * Execution order is implied by the position in the steps list
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionStep implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Rule ID
         */
        private Long ruleId;

        /**
         * Rule group ID (0 if standalone rule, non-zero if rule belongs to a group)
         */
        private Long groupId;

        // todo: Consider adding version of the executed rule content

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

