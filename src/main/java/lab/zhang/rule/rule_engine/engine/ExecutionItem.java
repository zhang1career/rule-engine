package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.model.Rule;
import lombok.Getter;

/**
 * Execution item - represents a rule to be executed
 * 
 * @author Rongjin Zhang
 */
@Getter
public class ExecutionItem {

    private final Rule rule;

    private final Long groupId;


    private ExecutionItem(Rule rule, Long groupId) {
        this.rule = rule;
        this.groupId = groupId;
    }
    
    /**
     * Create execution item for a rule
     */
    public static ExecutionItem forRule(Rule rule, Long groupId) {
        return new ExecutionItem(rule, groupId);
    }
}

