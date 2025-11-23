package lab.zhang.rule.rule_engine.service.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.dto.EvalRequest;
import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.service.EvalService;
import lab.zhang.rule.rule_engine.service.MessageQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Rule evaluation service implementation
 * 
 * @author rule-engine
 */
@Slf4j
@Service
public class EvalServiceImpl implements EvalService {
    
    @Autowired
    private RuleExecutionEngine ruleExecutionEngine;
    
    @Autowired
    private MessageQueueService messageQueueService;
    
    @Override
    public TypedValue eval(EvalRequest request) {
        log.info("Eval request received: userId={}, eventId={}, traceId={}", 
                request.getUserId(), request.getEventId(), request.getTraceId());
        
        // Build execution context
        RuleExecutionContext context = new RuleExecutionContext(
            request.getUserId(),
            request.getEventId(),
            request.getTraceId(),
            request.getDataMap()
        );
        
        // Execute rule evaluation
        TypedValue result = ruleExecutionEngine.execute(request.getEventId(), context);
        
        // Asynchronously send message to RabbitMQ
        try {
            messageQueueService.sendEvalResult(request, result);
        } catch (Exception e) {
            // Message sending failure does not affect main flow, only log
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
        }
        
        return result;
    }
}

