package lab.zhang.rule.rule_engine.executor.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;

/**
 * Groovy script rule executor
 * 
 * @author rule-engine
 */
@Slf4j
public class ScriptRuleExecutor implements RuleExecutor {
    
    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Create Groovy binding
            Binding binding = new Binding();

            // Add input parameters to binding
            if (context.getDataMap() != null) {
                context.getDataMap().forEach((key, typedValue) -> {
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
            Object result = shell.evaluate(rule.getRuleContent());

            // Convert result to TypedValue
            return convertToTypedValue(result);
        } catch (Exception e) {
            log.error("Script rule execution failed, ruleId: {}, error: {}", 
                     rule.getRuleId(), e.getMessage(), e);
            throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public RuleType getSupportedRuleType() {
        return RuleType.SCRIPT;
    }
    
    /**
     * Convert result to TypedValue
     */
    private TypedValue convertToTypedValue(Object result) {
        if (result == null) {
            return new TypedValue(null, TypedValue.ValueType.OBJECT);
        }
        
        if (result instanceof String) {
            return new TypedValue(result, TypedValue.ValueType.STRING);
        } else if (result instanceof Integer) {
            return new TypedValue(result, TypedValue.ValueType.INTEGER);
        } else if (result instanceof Long) {
            return new TypedValue(result, TypedValue.ValueType.LONG);
        } else if (result instanceof Double || result instanceof Float) {
            return new TypedValue(result, TypedValue.ValueType.DECIMAL);
        } else if (result instanceof Boolean) {
            return new TypedValue(result, TypedValue.ValueType.BOOLEAN);
        } else {
            return new TypedValue(result, TypedValue.ValueType.OBJECT);
        }
    }
}

