package lab.zhang.rule.rule_engine.service;

/**
 * A/B test service interface
 * 
 * @author rule-engine
 */
public interface ABTestService {
    
    /**
     * Determine whether user should execute A/B test rule
     * 
     * @param userId user ID
     * @param eventId event ID
     * @param ruleId rule ID
     * @param abTestRatio A/B test ratio
     * @return whether should execute
     */
    boolean shouldExecuteABTest(Long userId, Integer eventId, Long ruleId, Integer abTestRatio);
    
    /**
     * Record A/B test decision
     */
    void recordABTestDecision(Long userId, Integer eventId, Long ruleId);
    
    /**
     * Check if user has A/B test record
     */
    boolean hasABTestRecord(Long userId, Integer eventId, Long ruleId);
}

