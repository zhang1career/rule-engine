package lab.zhang.rule.rule_engine.engine

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.Environment
import lab.zhang.rule.rule_engine.enums.RuleStatus
import lab.zhang.rule.rule_engine.enums.RuleType
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.executor.RuleExecutor
import lab.zhang.rule.rule_engine.service.ABTestService
import lab.zhang.rule.rule_engine.service.RuleService
import spock.lang.Specification

import java.lang.reflect.Field

/**
 * RuleExecutionEngine unit test
 */
class RuleExecutionEngineSpec extends Specification {

    def ruleService = Mock(RuleService)
    def abTestService = Mock(ABTestService)
    def ruleExecutor = Mock(RuleExecutor)
    def engine = new RuleExecutionEngine()

    def setup() {
        engine.ruleService = ruleService
        engine.abTestService = abTestService
        setPrivateField(engine, "environment", "TEST")
        // Mock getSupportedRuleType for executor registration (use _ * for default behavior)
        _ * ruleExecutor.getSupportedRuleType() >> RuleType.EXPRESSION
        engine.registerExecutor(ruleExecutor)
    }

    def setPrivateField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }

    def "test execute rule sequence - normal case"() {
        given: "prepare rules and execution context"
        // Use TEST status since environment is TEST
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )
        def rule2 = new Rule(
            ruleId: 2L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        context.traceId = 999L

        def result1 = new TypedValue(true, TypedValue.ValueType.BOOLEAN)
        def result2 = new TypedValue(100, TypedValue.ValueType.INTEGER)

        when: "execute rule sequence"
        def result = engine.execute(1001, context)

        then: "should execute rules in order"
        1 * ruleService.getRulesByEventId(1001) >> [rule1, rule2]
        1 * ruleExecutor.execute(rule1, context) >> result1
        1 * ruleExecutor.execute(rule2, context) >> result2
        result == result2
    }

    def "test execute rule sequence - should throw exception when no rules"() {
        given: "prepare execution context"
        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        when: "execute rule sequence"
        engine.execute(1001, context)

        then: "should throw exception"
        1 * ruleService.getRulesByEventId(1001) >> []
        thrown(RuntimeException)
    }

    def "test execute rule sequence - skip offline status rules"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.OFFLINE
        )
        def rule2 = new Rule(
            ruleId: 2L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result2 = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute rule sequence"
        def result = engine.execute(1001, context)

        then: "should skip offline status rules"
        1 * ruleService.getRulesByEventId(1001) >> [rule1, rule2]
        0 * ruleExecutor.execute(rule1, _)
        1 * ruleExecutor.execute(rule2, context) >> result2
        result == result2
    }

    def "test execute rule sequence - test environment only executes TEST status rules"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )
        def rule2 = new Rule(
            ruleId: 2L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.FULL
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute rule sequence"
        def result = engine.execute(1001, context)

        then: "should only execute TEST status rules"
        1 * ruleService.getRulesByEventId(1001) >> [rule1, rule2]
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleExecutor.execute(rule2, _)
        result == result1
    }

    def "test execute rule sequence - production environment executes FULL status rules"() {
        given: "set environment to production"
        setPrivateField(engine, "environment", "PRODUCTION")

        and: "prepare rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.FULL
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute rule sequence"
        def result = engine.execute(1001, context)

        then: "should execute FULL status rules"
        1 * ruleService.getRulesByEventId(1001) >> [rule1]
        1 * ruleExecutor.execute(rule1, context) >> result1
        result == result1
    }

    def "test execute rule sequence - A/B test rules"() {
        given: "set environment to production"
        setPrivateField(engine, "environment", "PRODUCTION")

        and: "prepare A/B test rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.AB_TEST,
            abTestRatio: 50
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute rule sequence"
        def result = engine.execute(1001, context)

        then: "should decide whether to execute based on A/B test result"
        1 * ruleService.getRulesByEventId(1001) >> [rule1]
        1 * abTestService.shouldExecuteABTest(123L, 1001, 1L, 50) >> true
        1 * ruleExecutor.execute(rule1, context) >> result1
        result == result1
    }

    def "test execute rule sequence - early break (return false)"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )
        def rule2 = new Rule(
            ruleId: 2L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(false, TypedValue.ValueType.BOOLEAN)

        when: "execute rule sequence"
        def result = engine.execute(1001, context)

        then: "should break early when returning false"
        1 * ruleService.getRulesByEventId(1001) >> [rule1, rule2]
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleExecutor.execute(rule2, _)
        result == result1
    }

    def "test execute rule sequence - rule execution failure should throw exception"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.EXPRESSION,
            status: RuleStatus.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        when: "execute rule sequence"
        engine.execute(1001, context)

        then: "should throw exception"
        1 * ruleService.getRulesByEventId(1001) >> [rule1]
        1 * ruleExecutor.execute(rule1, context) >> {
            throw new RuntimeException("Rule execution failed")
        }
        thrown(RuntimeException)
    }

    def "test execute rule sequence - should throw exception when executor not found"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
            ruleId: 1L,
            ruleType: RuleType.SCRIPT,
            status: RuleStatus.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        when: "execute rule sequence"
        engine.execute(1001, context)

        then: "should throw exception"
        1 * ruleService.getRulesByEventId(1001) >> [rule1]
        thrown(RuntimeException)
    }
}

