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

import javax.validation.constraints.NotBlank;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

            // Add input parameters and variables to binding
            if (context.getArguments() != null) {
                context.getArguments().forEach((key, typedValue) -> {
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

    @Override
    public Set<String> extractArgs(@NotBlank String content) {
        Set<String> args = new HashSet<>();

        // Remove string literals to avoid matching words inside strings
        String contentWithoutStrings = content.replaceAll("'([^']*)'", "''").replaceAll("\"([^\"]*)\"", "\"\"");

        // First, identify variables declared with 'def' to exclude them
        Set<String> declaredVars = new HashSet<>();
        Pattern defPattern = Pattern.compile("\\bdef\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher defMatcher = defPattern.matcher(contentWithoutStrings);
        while (defMatcher.find()) {
            declaredVars.add(defMatcher.group(1));
        }

        // Pattern to match all potential identifiers (not containing dots)
        Pattern variablePattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*(?:\\.[a-zA-Z_][a-zA-Z0-9_]*)*)\\b");
        Matcher matcher = variablePattern.matcher(contentWithoutStrings);

        // Groovy keywords and common constructs to exclude
        Set<String> keywords = new HashSet<>(Arrays.asList(
            "true", "false", "null", "def", "var", "String", "Integer", "Long", "Double",
            "Boolean", "List", "Map", "Set", "if", "else", "for", "while", "in", "return",
            "break", "continue", "switch", "case", "default", "try", "catch", "finally",
            "throw", "throws", "new", "this", "super", "class", "interface", "enum",
            "package", "import", "extends", "implements", "public", "private", "protected",
            "static", "final", "abstract", "synchronized", "volatile", "transient",
            "void", "int", "long", "double", "float", "boolean", "char", "byte", "short",
            "println", "print", "printf", "assert", "as", "instanceof"
        ));

        while (matcher.find()) {
            String potentialVar = matcher.group(1);

            // Skip keywords and declared variables
            if (keywords.contains(potentialVar) || declaredVars.contains(potentialVar)) {
                continue;
            }

            // For qualified names like obj.prop.method, only include the base object name
            String baseVar = potentialVar.split("\\.")[0];
            if (!keywords.contains(baseVar) && !declaredVars.contains(baseVar)) {
                args.add(baseVar);
            }
        }

        return args;
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

