package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.dto.EvalRequest;

/**
 * Rule evaluation service interface
 * 
 * @author rule-engine
 */
public interface EvalService {
    
    /**
     * Execute rule evaluation
     * 
     * @param request evaluation request
     * @return evaluation result
     */
    TypedValue eval(EvalRequest request);
}

