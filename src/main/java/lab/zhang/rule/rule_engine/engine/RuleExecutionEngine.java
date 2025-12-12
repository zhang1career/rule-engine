package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.cache.EvalCacheService;
import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.constant.CommonConst;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Rule execution engine
 * Responsible for rule sequence execution, flow control, etc.
 *
 * @author Rongjin Zhang
 */
@Slf4j
@Component
public class RuleExecutionEngine {

    @Autowired
    @Lazy
    private RuleService ruleService;

    @Autowired
    private EvalCacheService evalCacheService;

    /**
     * Rule executor mapping (RuleTypeEnum -> RuleExecutor)
     */
    private final Map<ContentTypeEnum, RuleExecutor> executorMap = new HashMap<>();

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
     * Execute rule sequence with execution trace
     *
     * @param eventId event ID
     * @param context execution context
     * @param trace   execution trace to record execution process
     * @return execution result
     */
    public TypedValue execute(Long eventId, RuleExecutionContext context, ExecutionTrace trace) {
        if (log.isDebugEnabled()) {
            log.debug("[eval] execution param: eventId={}, userId={}", eventId, context.getUserId());
        }

        // Get execution items (rules) directly
        List<ExecutionItem> executionItemList = ruleService.getExecutionItemsByEventId(eventId != null ? eventId.intValue() : null, context);
        if (executionItemList.isEmpty()) {
            log.warn("[eval] no execution items found for eventId: {}, returning null result", eventId);
            return TypedValue.nullValue();
        }

        // Execute items sequentially
        // Note: All items are now rules (rule selection from groups is done in getExecutionItemsByEventId)
        TypedValue lastResult = null;
        for (ExecutionItem item : executionItemList) {
            Rule ruleToExecute = item.getRule();
            if (ruleToExecute == null) {
                log.warn("[eval] skipping execution item with null rule: item={}",  item);
                continue;
            }

            ExecutionTrace.ExecutionStep step = ExecutionTrace.ExecutionStep.builder()
                    .ruleId(ruleToExecute.getId())
                    .groupId(item.getGroupId())
                    .build();
            try {
                // Execute rule
                lastResult = executeRule(ruleToExecute, context);
                // Store rule execution result in context for subsequent rules
                context.putArgument("lastResult", lastResult);
                context.putArgument("rule:" + ruleToExecute.getId() + ":result", lastResult);
                if (log.isDebugEnabled()) {
                    log.debug("[eval] rule executed: ruleId={}, result={}", ruleToExecute.getId(), lastResult);
                }

                // Record execution step (only executed rules are recorded)
                step.setResult(lastResult);
                trace.addStep(step);

                // Determine whether to break early based on rule's internal logic
                boolean breakEarly = shouldBreakExecution(ruleToExecute, lastResult);
                if (breakEarly) {
                    log.info("[eval] break execution after rule: ruleId={}", ruleToExecute.getId());
                    break;
                }
            } catch (Exception e) {
                log.error("[eval] execution failed: ruleId={}, error={}", ruleToExecute.getId(), e.getMessage(), e);
                // Record execution step even if execution failed
                step.setErrmsg(e.getMessage());
                trace.addStep(step);
                throw new RuntimeException("Rule execution failed: " + ruleToExecute.getId(), e);
            }
        }

        if (lastResult == null) {
            log.warn("[eval] no rule executed");
            lastResult = TypedValue.nullValue();
        }

        if (log.isDebugEnabled()) {
            log.debug("[eval] execution completed: eventId={}, result={}", eventId, lastResult);
        }
        return lastResult;
    }


    /**
     * Execute single rule
     */
    private TypedValue executeRule(Rule rule, RuleExecutionContext context) {
        RuleExecutor executor = executorMap.get(rule.getContentType());
        if (executor == null) {
            throw new RuntimeException("No executor found for rule type: " + rule.getContentType());
        }

        // Try to get result from cache first
        if (rule.getContentArgList() != null && !rule.getContentArgList().isEmpty()) {
            List<String> argValues = extractArgValues(rule, context);
            TypedValue cachedResult = evalCacheService.get(rule, argValues);
            if (cachedResult != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Cache hit for rule {} with args {}", rule.getId(), argValues);
                }
                return cachedResult;
            }
        }

        // Execute rule
        TypedValue result = executor.execute(rule, context);

        // Cache the result if we have arg values
        if (rule.getContentArgList() != null && !rule.getContentArgList().isEmpty()) {
            List<String> argValues = extractArgValues(rule, context);
            evalCacheService.put(rule, argValues, result);
            if (log.isDebugEnabled()) {
                log.debug("Cached result for rule {} with args {}", rule.getId(), argValues);
            }
        }

        return result;
    }

    /**
     * Extract argument values from context based on rule's contentArgList
     * null value is treated as empty string
     *
     * @param rule    the rule
     * @param context execution context
     * @return List of argument values in order
     */
    private List<String> extractArgValues(Rule rule, RuleExecutionContext context) {
        if (rule.getContentArgList() == null || rule.getContentArgList().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> argValueList = new ArrayList<>();
        Map<String, TypedValue> variableMap = context.getVariables();

        for (String argName : rule.getContentArgList()) {
            TypedValue argTypedValue = variableMap.get(argName);
            if (argTypedValue == null) {
                throw new IllegalStateException("Argument value not found in context: " + argName);
            }
            argValueList.add(argTypedValue.getValue() != null
                    ? argTypedValue.getValue().toString()
                    : CommonConst.EMPTY_STRING);
        }
        // todo: consider including rule version in cache key if needed

        return argValueList;
    }

    /**
     * Determine whether to break execution early
     * Can decide whether to continue executing subsequent rules based on rule return result
     */
    private boolean shouldBreakExecution(Rule rule, TypedValue result) {
        // Can implement early break logic based on business requirements here
        // For example: if rule returns false, then break
        if (result == null || result.getType() != ValueTypeEnum.BOOLEAN) {
            return false;
        }
        Boolean boolValue = (Boolean) result.getValue();
        if (boolValue == null) {
            return false;
        }
        return !boolValue;
    }
}

