package lab.zhang.rule.rule_engine.executor.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Logical expression rule executor
 * 
 * @author rule-engine
 */
@Slf4j
public class ExpressionRuleExecutor implements RuleExecutor {
    
    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Build expression execution context
            Map<String, Object> evalContext = buildEvalContext(context);
            
            // Execute expression
            Serializable compiledExpression = MVEL.compileExpression(rule.getRuleContent());
            Object result = MVEL.executeExpression(compiledExpression, evalContext);
            
            // Convert result to TypedValue
            return convertToTypedValue(result);
        } catch (Exception e) {
            log.error("Expression rule execution failed, ruleId: {}, error: {}", 
                     rule.getRuleId(), e.getMessage(), e);
            throw new RuntimeException("Expression execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public RuleType getSupportedRuleType() {
        return RuleType.EXPRESSION;
    }
    
    /**
     * Build expression execution context
     */
    private Map<String, Object> buildEvalContext(RuleExecutionContext context) {
        Map<String, Object> evalContext = new HashMap<>();
        
        // Add input parameters
        if (context.getDataMap() != null) {
            context.getDataMap().forEach((key, typedValue) -> {
                evalContext.put(key, typedValue.getValue());
            });
        }
        
        // Add variables during execution
        if (context.getVariables() != null) {
            context.getVariables().forEach((key, typedValue) -> {
                evalContext.put(key, typedValue.getValue());
            });
        }
        
        // Add system variables
        evalContext.put("userId", context.getUserId());
        evalContext.put("eventId", context.getEventId());
        evalContext.put("traceId", context.getTraceId());
        
        return evalContext;
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

