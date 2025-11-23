package lab.zhang.rule.rule_engine.executor.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * API query rule executor
 * 
 * @author rule-engine
 */
@Slf4j
public class ApiQueryRuleExecutor implements RuleExecutor {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public ApiQueryRuleExecutor() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Parse rule content (JSON format, contains url, method, headers, body, etc.)
            Map<String, Object> apiConfig = objectMapper.readValue(
                rule.getRuleContent(), 
                Map.class
            );
            
            String url = (String) apiConfig.get("url");
            String method = (String) apiConfig.getOrDefault("method", "GET");
            Map<String, String> headers = (Map<String, String>) apiConfig.get("headers");
            Object body = apiConfig.get("body");
            
            // Build request
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            HttpEntity<Object> requestEntity = new HttpEntity<>(body, httpHeaders);
            
            // Execute HTTP request
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(method.toUpperCase()),
                requestEntity,
                String.class
            );
            
            // Return response result
            return new TypedValue(response.getBody(), TypedValue.ValueType.STRING);
        } catch (Exception e) {
            log.error("API query rule execution failed, ruleId: {}, error: {}", 
                     rule.getRuleId(), e.getMessage(), e);
            throw new RuntimeException("API query execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public RuleType getSupportedRuleType() {
        return RuleType.API_QUERY;
    }
}

