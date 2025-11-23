package lab.zhang.rule.rule_engine.service.impl

import lab.zhang.rule.rule_engine.enums.RuleStatus
import lab.zhang.rule.rule_engine.enums.RuleType
import lab.zhang.rule.rule_engine.model.Rule
import spock.lang.Specification

/**
 * RuleService unit test
 */
class RuleServiceImplSpec extends Specification {

    def ruleService = new RuleServiceImpl()

    def "test save and get rule"() {
        given: "create a rule"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleName = "Test Rule"
        rule.ruleType = RuleType.EXPRESSION
        rule.ruleContent = "amount > 100"
        rule.status = RuleStatus.FULL

        when: "save rule"
        ruleService.saveRule(rule)

        then: "can correctly get rule"
        def retrievedRule = ruleService.getRuleById(1L)
        retrievedRule != null
        retrievedRule.ruleId == 1L
        retrievedRule.ruleName == "Test Rule"
    }

    def "test save rule with null ruleId should throw exception"() {
        given: "create a rule with null ruleId"
        def rule = new Rule()
        rule.ruleId = null
        rule.ruleName = "Test Rule"

        when: "save rule"
        ruleService.saveRule(rule)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test save null rule should throw exception"() {
        when: "save null rule"
        ruleService.saveRule(null)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test save execution sequence"() {
        given: "create rules and execution sequence"
        def rule1 = new Rule(ruleId: 1L, status: RuleStatus.FULL)
        def rule2 = new Rule(ruleId: 2L, status: RuleStatus.FULL)
        ruleService.saveRule(rule1)
        ruleService.saveRule(rule2)

        when: "save execution sequence"
        ruleService.saveExecutionSequence(1001, [1L, 2L])

        then: "can correctly get rule list"
        def rules = ruleService.getRulesByEventId(1001)
        rules.size() == 2
        rules[0].ruleId == 1L
        rules[1].ruleId == 2L
    }

    def "test save execution sequence with null eventId should throw exception"() {
        when: "save execution sequence with null eventId"
        ruleService.saveExecutionSequence(null, [1L])

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test get rules for non-existent eventId should return empty list"() {
        when: "get rules for non-existent eventId"
        def rules = ruleService.getRulesByEventId(9999)

        then: "should return empty list"
        rules != null
        rules.isEmpty()
    }

    def "test filter offline status rules when getting rules"() {
        given: "create rules with different statuses"
        def rule1 = new Rule(ruleId: 1L, status: RuleStatus.FULL)
        def rule2 = new Rule(ruleId: 2L, status: RuleStatus.OFFLINE)
        def rule3 = new Rule(ruleId: 3L, status: RuleStatus.TEST)
        ruleService.saveRule(rule1)
        ruleService.saveRule(rule2)
        ruleService.saveRule(rule3)
        ruleService.saveExecutionSequence(1001, [1L, 2L, 3L])

        when: "get rule list"
        def rules = ruleService.getRulesByEventId(1001)

        then: "should filter out offline status rules"
        rules.size() == 2
        rules.every { it.status != RuleStatus.OFFLINE }
    }

    def "test execution sequence contains non-existent rule ID"() {
        given: "create execution sequence containing non-existent rule ID"
        def rule1 = new Rule(ruleId: 1L, status: RuleStatus.FULL)
        ruleService.saveRule(rule1)
        ruleService.saveExecutionSequence(1001, [1L, 999L])

        when: "get rule list"
        def rules = ruleService.getRulesByEventId(1001)

        then: "should only return existing rules"
        rules.size() == 1
        rules[0].ruleId == 1L
    }
}

