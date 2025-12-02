package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO;

/**
 * Message queue service interface
 *
 * @author Rongjin Zhang
 */
public interface MessageQueueService {

    /**
     * Send rule evaluation result to RabbitMQ
     *
     * @param request request parameters
     * @param result  evaluation result
     */
    void sendEvalResult(EvalDTO request, TypedValue result);
}

