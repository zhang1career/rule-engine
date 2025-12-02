package lab.zhang.rule.rule_engine.executor.impl;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Groovy script rule executor
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Component
public class ScriptRuleExecutor implements RuleExecutor {

    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Create Groovy binding
            Binding binding = new Binding();

            // Add input parameters to binding
            if (context.getArguments() != null) {
                context.getArguments().forEach((key, typedValue) -> {
                    binding.setVariable(key, typedValue.getValue());
                });
            }

            // Add variables during execution to binding
            if (context.getVariables() != null) {
                context.getVariables().forEach((key, typedValue) -> {
                    binding.setVariable(key, typedValue.getValue());
                });
            }

            // Add system variables
            binding.setVariable("userId", context.getUserId());
            binding.setVariable("eventId", context.getEventId());
            binding.setVariable("traceId", context.getTraceId());
            binding.setVariable("context", context);

            // Create GroovyShell and execute script
            GroovyShell shell = new GroovyShell(binding);
            Object result = shell.evaluate(rule.getContent());

            // Convert result to TypedValue
            return convertToTypedValue(result);
        } catch (Exception e) {
            log.error("Script rule execution failed, ruleId: {}, error: {}",
                    rule.getId(), e.getMessage(), e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ContentTypeEnum getSupportedRuleType() {
        return ContentTypeEnum.SCRIPT;
    }

    @Override
    public void validate(String content) {
        try {
            // Try to parse the script to validate syntax
            GroovyShell shell = new GroovyShell();
            shell.parse(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Groovy script: " + e.getMessage(), e);
        }
    }

    /**
     * Convert result to TypedValue
     */
    private TypedValue convertToTypedValue(Object result) {
        if (result == null) {
            return TypedValue.nullValue();
        }

        if (result instanceof String) {
            return new TypedValue(result, ValueTypeEnum.STRING);
        } else if (result instanceof Integer) {
            return new TypedValue(result, ValueTypeEnum.INTEGER);
        } else if (result instanceof Long) {
            return new TypedValue(result, ValueTypeEnum.LONG);
        } else if (result instanceof Double || result instanceof Float) {
            return new TypedValue(result, ValueTypeEnum.DECIMAL);
        } else if (result instanceof Boolean) {
            return new TypedValue(result, ValueTypeEnum.BOOLEAN);
        } else {
            return new TypedValue(result, ValueTypeEnum.OBJECT);
        }
    }
}

