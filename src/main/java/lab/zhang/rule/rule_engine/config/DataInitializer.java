package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Data initializer
 * Used to initialize sample rules and execution sequences (for demonstration only, production should load from database)
 * 
 * @author Rongjin Zhang
 */
@Slf4j
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private RuleService ruleService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing sample rules and execution sequences...");
        
        // Initialize sample rules and get the generated IDs
        Long[] ruleIds = initSampleRules();
        
        // Initialize sample execution sequences using the generated rule IDs
        initSampleSequences(ruleIds);
        
        log.info("Data initialization completed");
    }
    
    /**
     * Initialize sample rules
     * @return array of generated rule IDs [rule1Id, rule2Id, rule3Id]
     */
    private Long[] initSampleRules() {
        // Sample rule 1: Expression rule
        // Create rule with OFFLINE status (default), then transition to FULL
        // Note: id should be null to let database auto-generate it
        Rule rule1 = Rule.builder()
                .id(null) // Let database auto-generate ID
                .name("Amount Check Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("amount > 1000 && age >= 18")
                .ruleStatus(RuleStatusEnum.OFFLINE) // Start with OFFLINE
                .description("Check if amount is greater than 1000 and age is greater than or equal to 18")
                .build();
        ruleService.createRule(rule1);
        Long rule1Id = rule1.getId(); // Get the generated ID
        
        // Transition: OFFLINE -> TEST -> GRAY -> AB_TEST -> FULL
        rule1.setRuleStatus(RuleStatusEnum.TEST);
        ruleService.updateRule(rule1Id, rule1);
        rule1.setRuleStatus(RuleStatusEnum.GRAY);
        ruleService.updateRule(rule1Id, rule1);
        rule1.setRuleStatus(RuleStatusEnum.AB_TEST);
        ruleService.updateRule(rule1Id, rule1);
        rule1.setRuleStatus(RuleStatusEnum.FULL);
        ruleService.updateRule(rule1Id, rule1);
        
        // Sample rule 2: Groovy script rule
        // Create rule with OFFLINE status (default), then transition to FULL
        Rule rule2 = Rule.builder()
                .id(null) // Let database auto-generate ID
                .name("Discount Calculation Rule")
                .contentType(ContentTypeEnum.SCRIPT)
                .content("def discount = 0.0; if (amount > 5000) { discount = 0.1 } else if (amount > 2000) { discount = 0.05 }; return discount;")
                .ruleStatus(RuleStatusEnum.OFFLINE) // Start with OFFLINE
                .description("Calculate discount based on amount")
                .build();
        ruleService.createRule(rule2);
        Long rule2Id = rule2.getId(); // Get the generated ID
        
        // Transition: OFFLINE -> TEST -> GRAY -> AB_TEST -> FULL
        rule2.setRuleStatus(RuleStatusEnum.TEST);
        ruleService.updateRule(rule2Id, rule2);
        rule2.setRuleStatus(RuleStatusEnum.GRAY);
        ruleService.updateRule(rule2Id, rule2);
        rule2.setRuleStatus(RuleStatusEnum.AB_TEST);
        ruleService.updateRule(rule2Id, rule2);
        rule2.setRuleStatus(RuleStatusEnum.FULL);
        ruleService.updateRule(rule2Id, rule2);
        
        // Sample rule 3: A/B test rule
        // Create rule with OFFLINE status (default), then transition to AB_TEST
        Rule rule3 = Rule.builder()
                .id(null) // Let database auto-generate ID
                .name("A/B Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("amount > 500")
                .ruleStatus(RuleStatusEnum.OFFLINE) // Start with OFFLINE
                .description("A/B test rule example")
                .build();
        ruleService.createRule(rule3);
        Long rule3Id = rule3.getId(); // Get the generated ID
        
        // Transition: OFFLINE -> TEST -> GRAY -> AB_TEST
        rule3.setRuleStatus(RuleStatusEnum.TEST);
        ruleService.updateRule(rule3Id, rule3);
        rule3.setRuleStatus(RuleStatusEnum.GRAY);
        ruleService.updateRule(rule3Id, rule3);
        rule3.setRuleStatus(RuleStatusEnum.AB_TEST);
        ruleService.updateRule(rule3Id, rule3);
        
        // Note: abTestRatio is now managed via rule groups, not directly on the rule
        
        log.info("Sample rules initialized: rule1Id={}, rule2Id={}, rule3Id={}", rule1Id, rule2Id, rule3Id);
        
        return new Long[]{rule1Id, rule2Id, rule3Id};
    }
    
    /**
     * Initialize sample execution sequences
     * @param ruleIds array of rule IDs [rule1Id, rule2Id, rule3Id]
     */
    private void initSampleSequences(Long[] ruleIds) {
        if (ruleIds == null || ruleIds.length < 3) {
            log.warn("Cannot initialize execution sequences: rule IDs not available");
            return;
        }
        
        Long rule1Id = ruleIds[0];
        Long rule2Id = ruleIds[1];
        Long rule3Id = ruleIds[2];
        
        // Create execution sequence for eventId=1001
        ruleService.saveExecutionSequence(1001L, Arrays.asList(rule1Id, rule2Id));
        
        // Create execution sequence for eventId=1002
        ruleService.saveExecutionSequence(1002L, Arrays.asList(rule1Id, rule3Id));
        
        log.info("Sample execution sequences initialized: eventId=1001 with rules [{}, {}], eventId=1002 with rules [{}, {}]",
                rule1Id, rule2Id, rule1Id, rule3Id);
    }
}

