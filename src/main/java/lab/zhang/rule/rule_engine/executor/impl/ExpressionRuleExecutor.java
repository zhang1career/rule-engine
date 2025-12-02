package lab.zhang.rule.rule_engine.executor.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Logical expression rule executor
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Component
public class ExpressionRuleExecutor implements RuleExecutor {

    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Build expression execution context
            Map<String, Object> evalContext = buildEvalContext(context);

            // Execute expression
            Serializable compiledExpression = MVEL.compileExpression(rule.getContent());
            Object result = MVEL.executeExpression(compiledExpression, evalContext);

            // Convert result to TypedValue
            return convertToTypedValue(result);
        } catch (Exception e) {
            log.error("Expression rule execution failed, ruleId: {}, error: {}",
                    rule.getId(), e.getMessage(), e);
            throw new RuntimeException("Expression execution failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ContentTypeEnum getSupportedRuleType() {
        return ContentTypeEnum.EXPRESSION;
    }

    @Override
    public void validate(@NotBlank String content) {
        try {
            MVEL.compileExpression(content);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid MVEL expression: " + e.getMessage(), e);
        }
    }

    /**
     * Build expression execution context
     */
    private Map<String, Object> buildEvalContext(RuleExecutionContext context) {
        Map<String, Object> evalContext = new HashMap<>();

        // Add input parameters
        if (context.getArguments() != null) {
            context.getArguments().forEach((key, typedValue) -> {
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

