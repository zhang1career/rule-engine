package lab.zhang.rule.rule_engine.service;

import lab.zhang.rule.rule_engine.model.Rule;

import java.util.List;

/**
 * Rule service interface
 * 
 * @author rule-engine
 */
public interface RuleService {
    
    /**
     * Get rule by ruleId
     */
    Rule getRuleById(Long ruleId);
    
    /**
     * Get rule list in execution sequence by eventId
     */
    List<Rule> getRulesByEventId(Integer eventId);
    
    /**
     * Save rule
     */
    void saveRule(Rule rule);
    
    /**
     * Save execution sequence
     */
    void saveExecutionSequence(Integer eventId, List<Long> ruleIds);
}

