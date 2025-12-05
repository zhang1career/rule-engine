package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.EvalResult;
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
     * @return evaluation result with execution trace
     */
    EvalResult eval(EvalDTO request);
}

