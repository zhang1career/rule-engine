package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.enums.Environment;
import lab.zhang.rule.rule_engine.enums.RuleStatus;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.service.ABTestService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule execution engine
 * Responsible for rule sequence execution, flow control, etc.
 * 
 * @author rule-engine
 */
@Slf4j
@Component
public class RuleExecutionEngine {
    
    @Autowired
    private RuleService ruleService;
    
    @Autowired
    private ABTestService abTestService;
    
    /**
     * Rule executor mapping (RuleType -> RuleExecutor)
     */
    private final Map<RuleType, RuleExecutor> executorMap = new HashMap<>();
    
    /**
     * Current environment (read from configuration, default is test environment)
     */
    @Value("${rule.engine.environment:TEST}")
    private String environment;
    
    /**
     * Initialize rule executors
     */
    @PostConstruct
    public void init() {
        // Register various rule executors
        // Note: Need to inject specific executor implementations here
        // For simplicity, leave it empty first, register later through Spring's auto-injection
    }
    
    /**
     * Register rule executor
     */
    public void registerExecutor(RuleExecutor executor) {
        executorMap.put(executor.getSupportedRuleType(), executor);
        log.info("Rule executor registered: type={}", executor.getSupportedRuleType());
    }
    
    /**
     * Execute rule sequence
     * 
     * @param eventId event ID
     * @param context execution context
     * @return execution result
     */
    public TypedValue execute(Integer eventId, RuleExecutionContext context) {
        log.info("Starting rule execution: eventId={}, userId={}, traceId={}", 
                eventId, context.getUserId(), context.getTraceId());
        
        // Get execution sequence
        List<Rule> rules = ruleService.getRulesByEventId(eventId);
        if (rules.isEmpty()) {
            log.warn("No rules found for eventId: {}", eventId);
            throw new RuntimeException("No rules found for eventId: " + eventId);
        }
        
        // Execute rules sequentially
        TypedValue lastResult = null;
        for (Rule rule : rules) {
            // Check rule status
            if (!shouldExecuteRule(rule, context)) {
                log.debug("Rule skipped: ruleId={}, status={}", rule.getRuleId(), rule.getStatus());
                continue;
            }
            
            try {
                // Execute rule
                context.incrementDepth();
                lastResult = executeRule(rule, context);
                context.decrementDepth();
                
                // Store rule execution result in context for subsequent rules
                context.setVariable("lastResult", lastResult);
                context.setVariable("rule_" + rule.getRuleId() + "_result", lastResult);
                
                log.info("Rule executed: ruleId={}, result={}", rule.getRuleId(), lastResult);
                
                // Determine whether to break early based on rule's internal logic
                // Can decide based on the result type and value returned by the rule
                // For example: if Boolean type and false, can break early
                if (shouldBreakExecution(rule, lastResult)) {
                    log.info("Breaking execution after rule: ruleId={}", rule.getRuleId());
                    break;
                }
            } catch (Exception e) {
                log.error("Rule execution failed: ruleId={}, error={}", 
                         rule.getRuleId(), e.getMessage(), e);
                throw new RuntimeException("Rule execution failed: " + rule.getRuleId(), e);
            }
        }
        
        if (lastResult == null) {
            log.warn("No rule executed, returning null result");
            lastResult = new TypedValue(null, TypedValue.ValueType.OBJECT);
        }
        
        log.info("Rule execution completed: eventId={}, result={}", eventId, lastResult);
        return lastResult;
    }
    
    /**
     * Execute single rule
     */
    private TypedValue executeRule(Rule rule, RuleExecutionContext context) {
        RuleExecutor executor = executorMap.get(rule.getRuleType());
        if (executor == null) {
            throw new RuntimeException("No executor found for rule type: " + rule.getRuleType());
        }
        
        return executor.execute(rule, context);
    }
    
    /**
     * Determine whether rule should be executed
     */
    private boolean shouldExecuteRule(Rule rule, RuleExecutionContext context) {
        RuleStatus status = rule.getStatus();
        Environment env = Environment.valueOf(environment);
        
        switch (status) {
            case OFFLINE:
                return false;
                
            case TEST:
                // Test status: only allow test environment
                return env == Environment.TEST;
                
            case AB_TEST:
                // A/B test status: only allow production environment, and need to meet A/B test conditions
                if (env != Environment.PRODUCTION) {
                    return false;
                }
                return abTestService.shouldExecuteABTest(
                    context.getUserId(), 
                    context.getEventId(), 
                    rule.getRuleId(), 
                    rule.getAbTestRatio()
                );
                
            case FULL:
                // Full status: only allow production environment
                return env == Environment.PRODUCTION;
                
            default:
                return false;
        }
    }
    
    /**
     * Determine whether to break execution early
     * Can decide whether to continue executing subsequent rules based on rule return result
     */
    private boolean shouldBreakExecution(Rule rule, TypedValue result) {
        // Can implement early break logic based on business requirements here
        // For example: if rule returns false, then break
        if (result != null && result.getType() == TypedValue.ValueType.BOOLEAN) {
            Boolean boolValue = result.getBooleanValue();
            if (boolValue != null && !boolValue) {
                return true;
            }
        }
        return false;
    }
}

