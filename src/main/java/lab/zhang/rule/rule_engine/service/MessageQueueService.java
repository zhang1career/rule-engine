package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.dto.EvalRequest;

/**
 * Message queue service interface
 * 
 * @author rule-engine
 */
public interface MessageQueueService {
    
    /**
     * Send rule evaluation result to RabbitMQ
     * 
     * @param request request parameters
     * @param result evaluation result
     */
    void sendEvalResult(EvalRequest request, TypedValue result);
}

