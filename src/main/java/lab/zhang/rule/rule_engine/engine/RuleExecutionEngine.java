package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.common.TypedValue;
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum;
import lab.zhang.rule.rule_engine.executor.RuleExecutor;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleExecutionContext;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.service.RuleGroupService;
import lab.zhang.rule.rule_engine.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Lazy
    private RuleGroupService ruleGroupService;

    /**
     * Rule executor mapping (RuleTypeEnum -> RuleExecutor)
     */
    private final Map<ContentTypeEnum, RuleExecutor> executorMap = new HashMap<>();

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
     * Execute rule sequence with execution trace
     *
     * @param eventId event ID
     * @param context execution context
     * @param trace execution trace to record execution process
     * @return execution result
     */
    public TypedValue execute(Long eventId, RuleExecutionContext context, ExecutionTrace trace) {
        log.info("Starting rule execution: eventId={}, userId={}, traceId={}",
                eventId, context.getUserId(), context.getTraceId());

        // Get execution items (rules or rule groups) directly
        List<ExecutionItem> executionItems = ruleService.getExecutionItemsByEventId(eventId);
        if (executionItems.isEmpty()) {
            log.warn("No execution items found for eventId: {}, returning null result", eventId);
            return TypedValue.nullValue();
        }

        // Execute items sequentially
        TypedValue lastResult = null;
        for (ExecutionItem item : executionItems) {
            Rule ruleToExecute = null;

            ExecutionTrace.ExecutionStep step = ExecutionTrace.ExecutionStep.builder()
                    .itemType(item.getType())
                    .build();

            if (item.getType() == ExecutionItemTypeEnum.RULE) {
                ruleToExecute = item.getRule();
                step.setItemId(ruleToExecute != null ? ruleToExecute.getId() : null);
            } else if (item.getType() == ExecutionItemTypeEnum.RULE_GROUP) {
                Long ruleGroupId = item.getRuleGroup() != null ? item.getRuleGroup().getId() : null;
                if (ruleGroupId == null) {
                    throw new IllegalArgumentException("Rule group ID is null in execution item");
                }
                step.setItemId(ruleGroupId);
                ruleToExecute = selectRuleToExecuteFromGroup(item, context);
                if (ruleToExecute != null) {
                    Long abTestedRuleId = ruleToExecute.getId();
                    step.setAbTestedRuleId(abTestedRuleId);
                }
            }

            if (ruleToExecute == null) {
                // Skip if no rule to execute, do not record in trace
                continue;
            }

            // Check if rule status matches current environment
            if (!isRuleStatusAllowedForEnvironment(ruleToExecute.getRuleStatus())) {
                log.debug("Rule status {} does not match environment {}, skipping rule: ruleId={}",
                        ruleToExecute.getRuleStatus(), environment, ruleToExecute.getId());
                // Skip if status doesn't match, do not record in trace
                continue;
            }

            try {
                // Execute rule
                lastResult = executeRule(ruleToExecute, context);
                // Store rule execution result in context for subsequent rules
                context.setVariable("lastResult", lastResult);
                context.setVariable("rule:" + ruleToExecute.getId() + ":result", lastResult);
                log.info("Rule executed: ruleId={}, result={}", ruleToExecute.getId(), lastResult);
                
                // Record execution step (only executed rules are recorded)
                step.setResult(lastResult);
                trace.addStep(step);
                
                // Determine whether to break early based on rule's internal logic
                boolean breakEarly = shouldBreakExecution(ruleToExecute, lastResult);
                if (breakEarly) {
                    log.info("Breaking execution after rule: ruleId={}", ruleToExecute.getId());
                    break;
                }
            } catch (Exception e) {
                log.error("Rule execution failed: ruleId={}, error={}", ruleToExecute.getId(), e.getMessage(), e);
                // Record execution step even if execution failed
                step.setErrmsg(e.getMessage());
                trace.addStep(step);
                throw new RuntimeException("Rule execution failed: " + ruleToExecute.getId(), e);
            }
        }

        if (lastResult == null) {
            log.warn("No rule executed, returning null result");
            lastResult = TypedValue.nullValue();
        }

        log.info("Rule execution completed: eventId={}, result={}", eventId, lastResult);
        return lastResult;
    }

    private Rule selectRuleToExecuteFromGroup(ExecutionItem item, RuleExecutionContext context) {
        // Rule group - select one rule to execute
        RuleGroup group = item.getRuleGroup();
        if (group == null || group.getRuleIds() == null || group.getRuleIds().isEmpty()) {
            log.warn("Rule group is empty or not found: groupId={}", group != null ? group.getId() : null);
            return null;
        }
        if (context.getUserId() == null
                || context.getEventId() == null
                || context.getArgument(EvalArgumentConst.ARG_USER_HASH) == null
                || context.getArgument(EvalArgumentConst.ARG_USER_HASH).getValue() == null) {
            log.warn("Missing userId, eventId or userHash in context for rule group selection: groupId={}", group.getId());
            return null;
        }

        Rule ruleToExecute = ruleGroupService.selectRuleFromGroup(group, context);
        if (ruleToExecute == null) {
            log.warn("Rule selected from group skipped: groupId={}, no rule selected", group.getId());
            return null;
        }

        log.info("Rule selected from group: groupId={}, ruleId={}", group.getId(), ruleToExecute.getId());
        return ruleToExecute;
    }

    /**
     * Execute single rule
     */
    private TypedValue executeRule(Rule rule, RuleExecutionContext context) {
        RuleExecutor executor = executorMap.get(rule.getContentType());
        if (executor == null) {
            throw new RuntimeException("No executor found for rule type: " + rule.getContentType());
        }

        return executor.execute(rule, context);
    }

    /**
     * Check if rule status is allowed for current environment
     *
     * @param ruleStatus rule status to check
     * @return true if rule status is allowed for current environment, false otherwise
     */
    private boolean isRuleStatusAllowedForEnvironment(RuleStatusEnum ruleStatus) {
        if (ruleStatus == null) {
            return false;
        }

        // Map environment string to allowed rule statuses
        // TEST environment: only TEST status
        // GRAY environment: GRAY, AB_TEST, FULL status
        // PRODUCTION environment: AB_TEST, FULL status
        Set<RuleStatusEnum> allowedStatuses;
        try {
            String envUpper = environment != null ? environment.toUpperCase() : "TEST";
            switch (envUpper) {
                case "TEST":
                    allowedStatuses = new HashSet<>(Arrays.asList(RuleStatusEnum.TEST));
                    break;
                case "GRAY":
                    allowedStatuses = new HashSet<>(Arrays.asList(RuleStatusEnum.GRAY, RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL));
                    break;
                case "PRODUCTION":
                    allowedStatuses = new HashSet<>(Arrays.asList(RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL));
                    break;
                default:
                    log.warn("Unknown environment: {}, defaulting to TEST", environment);
                    allowedStatuses = new HashSet<>(Arrays.asList(RuleStatusEnum.TEST));
                    break;
            }
        } catch (Exception e) {
            log.warn("Error parsing environment: {}, defaulting to TEST", environment, e);
            allowedStatuses = new HashSet<>(Arrays.asList(RuleStatusEnum.TEST));
        }

        return allowedStatuses.contains(ruleStatus);
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

