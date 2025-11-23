package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.enums.RuleStatus;
import lab.zhang.rule.rule_engine.enums.RuleType;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.service.RuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Data initializer
 * Used to initialize sample rules and execution sequences (for demonstration only, production should load from database)
 * 
 * @author rule-engine
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private RuleService ruleService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing sample rules and execution sequences...");
        
        // Initialize sample rules
        initSampleRules();
        
        // Initialize sample execution sequences
        initSampleSequences();
        
        log.info("Data initialization completed");
    }
    
    /**
     * Initialize sample rules
     */
    private void initSampleRules() {
        // Sample rule 1: Expression rule
        Rule rule1 = new Rule();
        rule1.setRuleId(1L);
        rule1.setRuleName("Amount Check Rule");
        rule1.setRuleType(RuleType.EXPRESSION);
        rule1.setRuleContent("amount > 1000 && age >= 18");
        rule1.setStatus(RuleStatus.FULL);
        rule1.setDescription("Check if amount is greater than 1000 and age is greater than or equal to 18");
        ruleService.saveRule(rule1);
        
        // Sample rule 2: Groovy script rule
        Rule rule2 = new Rule();
        rule2.setRuleId(2L);
        rule2.setRuleName("Discount Calculation Rule");
        rule2.setRuleType(RuleType.SCRIPT);
        rule2.setRuleContent("def discount = 0.0; if (amount > 5000) { discount = 0.1 } else if (amount > 2000) { discount = 0.05 }; return discount;");
        rule2.setStatus(RuleStatus.FULL);
        rule2.setDescription("Calculate discount based on amount");
        ruleService.saveRule(rule2);
        
        // Sample rule 3: A/B test rule
        Rule rule3 = new Rule();
        rule3.setRuleId(3L);
        rule3.setRuleName("A/B Test Rule");
        rule3.setRuleType(RuleType.EXPRESSION);
        rule3.setRuleContent("amount > 500");
        rule3.setStatus(RuleStatus.AB_TEST);
        rule3.setAbTestRatio(50); // 50% of users execute
        rule3.setDescription("A/B test rule example");
        ruleService.saveRule(rule3);
        
        log.info("Sample rules initialized");
    }
    
    /**
     * Initialize sample execution sequences
     */
    private void initSampleSequences() {
        // Create execution sequence for eventId=1001
        ruleService.saveExecutionSequence(1001, Arrays.asList(1L, 2L));
        
        // Create execution sequence for eventId=1002
        ruleService.saveExecutionSequence(1002, Arrays.asList(1L, 3L));
        
        log.info("Sample execution sequences initialized");
    }
}

