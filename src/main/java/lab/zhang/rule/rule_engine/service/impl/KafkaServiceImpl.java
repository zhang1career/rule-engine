package lab.zhang.rule.rule_engine.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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
    public void sendEvalResult(EvalRequest request, TypedValue result) {
//        if (kafkaTemplate == null) {
//            log.warn("KafkaTemplate not configured, skip sending message");
//            return;
//        }
//
//        try {
//            // Build message body
//            Map<String, Object> message = new HashMap<>();
//            message.put("userId", request.getUserId());
//            message.put("eventId", request.getEventId());
//            message.put("traceId", request.getTraceId());
//            message.put("arguments", request.getArguments());
//            message.put("result", result);
//            message.put("timestamp", System.currentTimeMillis());
//
//            // Send message
//            String messageBody = objectMapper.writeValueAsString(message);
//            kafkaTemplate.send(topic, messageBody);
//
//            log.info("Message sent to Kafka: traceId={}", request.getTraceId());
//        } catch (Exception e) {
//            log.error("Failed to send message to Kafka: {}", e.getMessage(), e);
//            throw new RuntimeException("Failed to send message to Kafka", e);
//        }
    }
}

