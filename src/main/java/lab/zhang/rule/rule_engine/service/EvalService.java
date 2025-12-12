package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;

import java.math.BigInteger;

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
     * @param traceId trace ID
     * @return evaluation result with execution trace
     */
    EvalResult eval(EvalRequest request, BigInteger traceId);
}

