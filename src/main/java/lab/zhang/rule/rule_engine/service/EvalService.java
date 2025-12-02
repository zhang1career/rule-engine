package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.engine.ExecutionTrace;
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO;

/**
 * Rule evaluation service interface
 *
 * @author Rongjin Zhang
 */
public interface EvalService {

    /**
     * Execute rule evaluation with execution trace
     *
     * @param request evaluation request
     * @param trace execution trace to record execution process
     * @return evaluation result
     */
    TypedValue eval(EvalDTO request, ExecutionTrace trace);
}

