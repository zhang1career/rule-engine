package lab.zhang.rule.rule_engine.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.model.EvalResult;
import lab.zhang.rule.rule_engine.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


/**
 * Kafka service implementation
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Service
public class KafkaServiceImpl implements KafkaService {

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rule.engine.kafka.topic:rule.eval.result}")
    private String topic;

    @Override
    public void sendEvalResult(EvalRequest request, EvalResult result) {
        if (kafkaTemplate == null) {
            log.warn("[eval] KafkaTemplate not configured, skip sending message");
            return;
        }

        try {
            // Build message body
            Map<String, Object> message = new HashMap<>();
            message.put("userId", request.getUserId());
            message.put("eventId", request.getEventId());
            message.put("traceId", result.getTrace().getTraceId());
            message.put("arguments", request.getArguments());
            message.put("result", result.getValue());
            message.put("timestamp", System.currentTimeMillis());

            // Send message
            String messageBody = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, messageBody);

            log.info("[eval] message sent to kafka, topic={}, messageSize={}", topic, messageBody.length());
        } catch (Exception e) {
            log.error("[eval] failed to send message to kafka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}
