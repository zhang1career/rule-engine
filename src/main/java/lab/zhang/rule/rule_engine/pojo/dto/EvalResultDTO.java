package lab.zhang.rule.rule_engine.pojo.dto;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Rule evaluation response DTO
 *
 * @author Rongjin Zhang
 */
@Data
public class EvalResultDTO {

    /**
     * Calculation result
     */
    private TypedValue result;

    private Map<TypedValue, List<ExecutionTrace.ExecutionStep>> briefSteps;


    public EvalResultDTO(TypedValue result, Map<TypedValue, List<ExecutionTrace.ExecutionStep>> briefStepMap) {
        this.result = result;
        this.briefSteps = briefStepMap;
    }
}

