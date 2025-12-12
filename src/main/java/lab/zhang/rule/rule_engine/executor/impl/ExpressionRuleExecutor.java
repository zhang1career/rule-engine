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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Override
    public Set<String> extractArgs(@NotBlank String content) {
        Set<String> args = new HashSet<>();

        // Remove string literals to avoid matching words inside strings
        String contentWithoutStrings = content.replaceAll("'([^']*)'", "''").replaceAll("\"([^\"]*)\"", "\"\"");

        // Pattern to match variable names: word characters starting with letter or underscore
        // This is a simplified approach - in a real implementation, you might want to use
        // AST parsing for more accurate results
        Pattern variablePattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher matcher = variablePattern.matcher(contentWithoutStrings);

        // Common keywords and operators that should be excluded
        Set<String> keywords = new HashSet<>(Arrays.asList(
            "true", "false", "null", "and", "or", "not", "in", "instanceof",
            "new", "return", "if", "else", "for", "while", "do", "switch", "case",
            "break", "continue", "default", "try", "catch", "finally", "throw",
            "throws", "this", "super", "class", "interface", "enum", "package",
            "import", "extends", "implements", "public", "private", "protected",
            "static", "final", "abstract", "synchronized", "volatile", "transient"
        ));

        while (matcher.find()) {
            String potentialVar = matcher.group(1);
            // Exclude keywords and common method names that might look like variables
            if (!keywords.contains(potentialVar) &&
                !potentialVar.startsWith("get") && !potentialVar.startsWith("set") &&
                !potentialVar.startsWith("is") && !potentialVar.startsWith("has")) {
                args.add(potentialVar);
            }
        }

        return args;
    }

    /**
     * Build expression execution context
     */
    private Map<String, Object> buildEvalContext(RuleExecutionContext context) {
        Map<String, Object> evalContext = new HashMap<>();

        // Add input parameters and variables
        if (context.getArguments() != null) {
            context.getArguments().forEach((key, typedValue) -> {
                evalContext.put(key, typedValue.getValue());
            });
        }

        // Add system variables
        evalContext.put("userId", context.getUserId());
        evalContext.put("eventId", context.getEventId());

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

