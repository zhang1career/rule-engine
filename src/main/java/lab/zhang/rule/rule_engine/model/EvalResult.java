package lab.zhang.rule.rule_engine.model;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule evaluation result model
 * Contains evaluation result and execution trace
 *
 * @author Rongjin Zhang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvalResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Evaluation result
     */
    private TypedValue result;

    /**
     * Execution trace
     */
    private ExecutionTrace trace;


    public Map<TypedValue, List<ExecutionTrace.ExecutionStep>> getBriefSteps() {
        if (this.trace == null || this.trace.getSteps() == null) {
            return new HashMap<>();
        }
        Map<TypedValue, List<ExecutionTrace.ExecutionStep>> briefSteps = new HashMap<>();
        for (ExecutionTrace.ExecutionStep step : this.trace.getSteps()) {
            TypedValue stepResult = step.getResult();
            briefSteps.computeIfAbsent(stepResult, k -> new ArrayList<>()).add(step);
        }

        return briefSteps;
    }
}

