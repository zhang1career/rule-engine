package lab.zhang.rule.rule_engine.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.dto.EvalRequest;
import lab.zhang.rule.rule_engine.service.MessageQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Message queue service implementation
 * 
 * @author rule-engine
 */
@Slf4j
@Service
public class MessageQueueServiceImpl implements MessageQueueService {
    
    @Autowired(required = false)
    private RabbitTemplate rabbitTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${rule.engine.rabbitmq.exchange:rule.engine.exchange}")
    private String exchange;
    
    @Value("${rule.engine.rabbitmq.routing-key:rule.eval.result}")
    private String routingKey;
    
    @Override
    public void sendEvalResult(EvalRequest request, TypedValue result) {
        if (rabbitTemplate == null) {
            log.warn("RabbitTemplate not configured, skip sending message");
            return;
        }
        
        try {
            // Build message body
            Map<String, Object> message = new HashMap<>();
            message.put("userId", request.getUserId());
            message.put("eventId", request.getEventId());
            message.put("traceId", request.getTraceId());
            message.put("dataMap", request.getDataMap());
            message.put("result", result);
            message.put("timestamp", System.currentTimeMillis());
            
            // Send message
            String messageBody = objectMapper.writeValueAsString(message);
            rabbitTemplate.convertAndSend(exchange, routingKey, messageBody);
            
            log.info("Message sent to RabbitMQ: traceId={}", request.getTraceId());
        } catch (Exception e) {
            log.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to RabbitMQ", e);
        }
    }
}

