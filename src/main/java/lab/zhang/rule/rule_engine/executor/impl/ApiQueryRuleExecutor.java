package lab.zhang.rule.rule_engine.executor.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.validation.constraints.NotBlank;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API query rule executor
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Component
public class ApiQueryRuleExecutor implements RuleExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiQueryRuleExecutor(@Lazy RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Parse rule content (JSON format, contains url, method, headers, body, etc.)
            Map<String, Object> apiConfig = objectMapper.readValue(
                    rule.getContent(),
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
            return new TypedValue(response.getBody(), ValueTypeEnum.STRING);
        } catch (Exception e) {
            log.error("API query rule execution failed, ruleId: {}, error: {}",
                    rule.getId(), e.getMessage(), e);
            throw new RuntimeException("API query execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ContentTypeEnum getSupportedRuleType() {
        return ContentTypeEnum.API_QUERY;
    }

    @Override
    public void validate(String content) {
        try {
            // Parse JSON format and validate required fields
            Map<String, Object> apiConfig = objectMapper.readValue(content, Map.class);

            // Validate required field: url
            if (!apiConfig.containsKey("url") || apiConfig.get("url") == null) {
                throw new IllegalArgumentException("API query content must contain 'url' field");
            }

            String url = (String) apiConfig.get("url");
            if (url.trim().isEmpty()) {
                throw new IllegalArgumentException("API query 'url' field cannot be empty");
            }

            // Validate method if provided
            if (apiConfig.containsKey("method")) {
                String method = (String) apiConfig.get("method");
                if (method != null) {
                    try {
                        HttpMethod.valueOf(method.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("Invalid HTTP method: " + method, e);
                    }
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for API query: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("Invalid API query content: " + e.getMessage(), e);
        }
    }

    @Override
    public Set<String> extractArgs(@NotBlank String content) {
        Set<String> args = new HashSet<>();

        try {
            // Parse JSON to extract string values that may contain placeholders
            Map<String, Object> apiConfig = objectMapper.readValue(content, Map.class);

            // Extract placeholders from URL
            String url = (String) apiConfig.get("url");
            if (url != null) {
                extractPlaceholders(url, args);
            }

            // Extract placeholders from headers
            Map<String, String> headers = (Map<String, String>) apiConfig.get("headers");
            if (headers != null) {
                headers.values().forEach(headerValue -> extractPlaceholders(headerValue, args));
            }

            // Extract placeholders from body (if it's a string)
            Object body = apiConfig.get("body");
            if (body instanceof String) {
                extractPlaceholders((String) body, args);
            }

        } catch (Exception e) {
            // If JSON parsing fails, try to extract placeholders from raw content
            extractPlaceholders(content, args);
        }

        return args;
    }

    /**
     * Extract parameter placeholders from a string
     */
    private void extractPlaceholders(String text, Set<String> args) {
        if (text == null) return;

        // Pattern for ${param} format
        Pattern dollarBracePattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher dollarBraceMatcher = dollarBracePattern.matcher(text);
        while (dollarBraceMatcher.find()) {
            args.add(dollarBraceMatcher.group(1));
        }

        // Pattern for :param format
        Pattern colonPattern = Pattern.compile(":([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher colonMatcher = colonPattern.matcher(text);
        while (colonMatcher.find()) {
            args.add(colonMatcher.group(1));
        }
    }
}

