package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrace implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * List of execution steps
     * Each step represents an executed rule
     * The last element in the list represents the last executed rule
     */
    @Builder.Default
    private List<ExecutionStep> steps = new ArrayList<>();

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
         * Execution item type (RULE or RULE_GROUP)
         */
        private ExecutionItemTypeEnum itemType;

        /**
         * Item ID (rule ID if itemType is RULE, rule group ID if itemType is RULE_GROUP)
         */
        private Long itemId;

        /**
         * Selected rule ID from group (if itemType is RULE_GROUP)
         */
        private Long abTestedRuleId;

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

