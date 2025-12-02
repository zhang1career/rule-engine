package lab.zhang.rule.rule_engine.engine;

import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lombok.Getter;

/**
 * Execution item - represents either a single rule or a rule group
 * 
 * @author Rongjin Zhang
 */
@Getter
public class ExecutionItem {

    private final ExecutionItemTypeEnum type;
    private Rule rule;
    private RuleGroup ruleGroup;
    
    private ExecutionItem(ExecutionItemTypeEnum type) {
        this.type = type;
    }
    
    /**
     * Create execution item for a single rule
     */
    public static ExecutionItem forRule(Rule rule) {
        ExecutionItem item = new ExecutionItem(ExecutionItemTypeEnum.RULE);
        item.rule = rule;
        return item;
    }
    
    /**
     * Create execution item for a rule group
     */
    public static ExecutionItem forRuleGroup(RuleGroup ruleGroup) {
        ExecutionItem item = new ExecutionItem(ExecutionItemTypeEnum.RULE_GROUP);
        item.ruleGroup = ruleGroup;
        return item;
    }

}

