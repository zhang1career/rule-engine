package lab.zhang.rule.rule_engine.config;

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.model.Event;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.service.EventService;
import lab.zhang.rule.rule_engine.service.RuleService;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Data initializer
 * Used to initialize sample rules and execution sequences (for demonstration only, production should load from database)
 * 
 * @author Rongjin Zhang
 */
@Slf4j
@Component
@Profile("gray")
public class DataInitializer implements CommandLineRunner {

    public static final int TEST_EVENT_ID = 10000000;
    public static final int DEMO_EVENT_ID = 10000001;


    @Autowired
    private EventService eventService;

    @Autowired
    private RuleService ruleService;
    
    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing data...");
        initData();
        log.info("Data initialization completed");
    }
    
    /**
     * Initialize data
     */
    private void initData() {
        // create event for test
        Event testEvent = eventService.getEventById(TEST_EVENT_ID);
        if (testEvent == null) {
            testEvent = Event.builder()
                    .id(TEST_EVENT_ID)
                    .name("Test Event")
                    .description("Event for testing rule engine")
                    .build();
            eventService.createEvent(testEvent);
        }

        // create event for demo
        Event demoEvent = eventService.getEventById(DEMO_EVENT_ID);
        if (demoEvent == null) {
            demoEvent = Event.builder()
                    .id(DEMO_EVENT_ID)
                    .name("Demo Event")
                    .description("Event for demonstrating rule engine")
                    .build();
            eventService.createEvent(demoEvent);
        }

        // sample rules
        // rule 1: expression rule
        Rule rule1 = Rule.builder()
                .name("Amount Check Rule")
                .description("Check if amount is greater than 1000 and age is greater than or equal to 18")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("amount > 1000 && age >= 18")
                .build();
        ruleService.createRule(rule1);
        Long rule1Id = rule1.getId(); // Get the generated ID
        // rule 2: script rule
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
        // rule 3: A/B test rule
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

        // associate rule with demo event before status transitions
        eventService.setExecutionArrangements(DEMO_EVENT_ID, Arrays.asList(rule1Id, rule2Id, rule3Id));

        // transition rule 1: OFFLINE -> TEST -> GRAY -> ONLINE
        rule1.setRuleStatus(RuleStatusEnum.TEST);
        ruleService.updateRule(rule1Id, rule1);
        rule1.setRuleStatus(RuleStatusEnum.GRAY);
        ruleService.updateRule(rule1Id, rule1);
        rule1.setRuleStatus(RuleStatusEnum.ONLINE);
        ruleService.updateRule(rule1Id, rule1);

        // transition rule 2: OFFLINE -> TEST -> GRAY -> ONLINE
        rule2.setRuleStatus(RuleStatusEnum.TEST);
        ruleService.updateRule(rule2Id, rule2);
        rule2.setRuleStatus(RuleStatusEnum.GRAY);
        ruleService.updateRule(rule2Id, rule2);
        rule2.setRuleStatus(RuleStatusEnum.ONLINE);
        ruleService.updateRule(rule2Id, rule2);

        // transition rule 3: OFFLINE -> TEST -> GRAY -> ONLINE
        rule3.setRuleStatus(RuleStatusEnum.TEST);
        ruleService.updateRule(rule3Id, rule3);
        rule3.setRuleStatus(RuleStatusEnum.GRAY);
        ruleService.updateRule(rule3Id, rule3);
        rule3.setRuleStatus(RuleStatusEnum.ONLINE);
        ruleService.updateRule(rule3Id, rule3);

        // Note: abTestRatio is now managed via rule groups, not directly on the rule

        log.info("Sample rules initialized: rule1Id={}, rule2Id={}, rule3Id={}", rule1Id, rule2Id, rule3Id);
    }
    
}

