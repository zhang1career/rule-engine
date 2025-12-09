package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;

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
     * @return evaluation result with execution trace
     */
    EvalResult eval(EvalRequest request);
}

