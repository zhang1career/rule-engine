package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;

/**
 * Kafka service interface
 *
 * @author Rongjin Zhang
 */
public interface KafkaService {

    /**
     * Send evaluation request and result to Kafka
     *
     * @param request evaluation request
     * @param result  evaluation result
     */
    void sendEvalResult(EvalRequest request, EvalResult result);
}

