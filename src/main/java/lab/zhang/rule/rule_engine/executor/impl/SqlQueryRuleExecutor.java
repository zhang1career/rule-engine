package lab.zhang.rule.rule_engine.executor.impl;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

/**
 * SQL query rule executor
 * 
 * @author rule-engine
 */
@Slf4j
public class SqlQueryRuleExecutor implements RuleExecutor {
    
    private final JdbcTemplate jdbcTemplate;
    
    public SqlQueryRuleExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public TypedValue execute(Rule rule, RuleExecutionContext context) {
        try {
            // Execute SQL query
            List<Map<String, Object>> results = jdbcTemplate.queryForList(rule.getRuleContent());
            
            // Return query result
            // If only one record, return that record; otherwise return the entire result set
            if (results.size() == 1) {
                Map<String, Object> singleResult = results.get(0);
                if (singleResult.size() == 1) {
                    // If only one column, return value directly
                    Object value = singleResult.values().iterator().next();
                    return convertToTypedValue(value);
                } else {
                    // Multiple columns, return Map
                    return new TypedValue(singleResult, TypedValue.ValueType.OBJECT);
                }
            } else {
                // Multiple records, return List
                return new TypedValue(results, TypedValue.ValueType.OBJECT);
            }
        } catch (Exception e) {
            log.error("SQL query rule execution failed, ruleId: {}, error: {}", 
                     rule.getRuleId(), e.getMessage(), e);
            throw new RuntimeException("SQL query execution failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public RuleType getSupportedRuleType() {
        return RuleType.SQL_QUERY;
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

