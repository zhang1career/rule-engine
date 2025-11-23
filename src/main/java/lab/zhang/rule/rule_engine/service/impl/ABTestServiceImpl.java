package lab.zhang.rule.rule_engine.service.impl;

import lab.zhang.rule.rule_engine.service.ABTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A/B test service implementation
 * Note: Currently using in-memory storage, can be replaced with database storage later
 * 
 * @author rule-engine
 */
@Slf4j
@Service
public class ABTestServiceImpl implements ABTestService {
    
    /**
     * A/B test record storage
     * Key: userId_eventId_ruleId
     */
    private final Set<String> abTestRecords = ConcurrentHashMap.newKeySet();
    
    @Override
    public boolean shouldExecuteABTest(Long userId, Integer eventId, Long ruleId, Integer abTestRatio) {
        if (userId == null || eventId == null || ruleId == null || abTestRatio == null) {
            return false;
        }
        
        // Check if record already exists
        String recordKey = buildRecordKey(userId, eventId, ruleId);
        if (hasABTestRecord(userId, eventId, ruleId)) {
            return true;
        }
        
        // Distribute evenly based on userId, then decide by ratio
        // Use hashCode of userId for distribution
        int hash = (userId.toString() + eventId.toString() + ruleId.toString()).hashCode();
        int bucket = Math.abs(hash) % 100;
        
        boolean shouldExecute = bucket < abTestRatio;
        
        if (shouldExecute) {
            recordABTestDecision(userId, eventId, ruleId);
        }
        
        return shouldExecute;
    }
    
    @Override
    public void recordABTestDecision(Long userId, Integer eventId, Long ruleId) {
        String recordKey = buildRecordKey(userId, eventId, ruleId);
        abTestRecords.add(recordKey);
        log.info("AB test decision recorded: userId={}, eventId={}, ruleId={}", 
                userId, eventId, ruleId);
    }
    
    @Override
    public boolean hasABTestRecord(Long userId, Integer eventId, Long ruleId) {
        String recordKey = buildRecordKey(userId, eventId, ruleId);
        return abTestRecords.contains(recordKey);
    }
    
    /**
     * Build record key
     */
    private String buildRecordKey(Long userId, Integer eventId, Long ruleId) {
        return userId + "_" + eventId + "_" + ruleId;
    }
}

