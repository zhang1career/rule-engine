package lab.zhang.rule.rule_engine.service.impl;

import lab.zhang.rule.rule_engine.enums.RuleStatus;
import lab.zhang.rule.rule_engine.model.ExecutionSequence;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Rule service implementation
 * Note: Currently using in-memory storage, can be replaced with database storage later
 * 
 * @author rule-engine
 */
@Slf4j
@Service
public class RuleServiceImpl implements RuleService {
    
    /**
     * Rule storage (ruleId -> Rule)
     */
    private final Map<Long, Rule> ruleMap = new ConcurrentHashMap<>();
    
    /**
     * Execution sequence storage (eventId -> ExecutionSequence)
     */
    private final Map<Integer, ExecutionSequence> sequenceMap = new ConcurrentHashMap<>();
    
    @Override
    public Rule getRuleById(Long ruleId) {
        return ruleMap.get(ruleId);
    }
    
    @Override
    public List<Rule> getRulesByEventId(Integer eventId) {
        ExecutionSequence sequence = sequenceMap.get(eventId);
        if (sequence == null || sequence.getRuleIds() == null) {
            return Collections.emptyList();
        }
        
        return sequence.getRuleIds().stream()
                .map(this::getRuleById)
                .filter(Objects::nonNull)
                .filter(rule -> rule.getStatus() != RuleStatus.OFFLINE)
                .collect(Collectors.toList());
    }
    
    @Override
    public void saveRule(Rule rule) {
        if (rule == null || rule.getRuleId() == null) {
            throw new IllegalArgumentException("Rule or ruleId cannot be null");
        }
        ruleMap.put(rule.getRuleId(), rule);
        log.info("Rule saved: ruleId={}, ruleName={}", rule.getRuleId(), rule.getRuleName());
    }
    
    @Override
    public void saveExecutionSequence(Integer eventId, List<Long> ruleIds) {
        if (eventId == null) {
            throw new IllegalArgumentException("EventId cannot be null");
        }
        
        ExecutionSequence sequence = new ExecutionSequence();
        sequence.setEventId(eventId);
        sequence.setRuleIds(ruleIds != null ? ruleIds : Collections.emptyList());
        sequenceMap.put(eventId, sequence);
        log.info("Execution sequence saved: eventId={}, ruleIds={}", eventId, ruleIds);
    }
}

