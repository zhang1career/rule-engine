package lab.zhang.rule.rule_engine.executor.impl

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.RuleType
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import spock.lang.Specification

/**
 * ScriptRuleExecutor unit test
 */
class ScriptRuleExecutorSpec extends Specification {

    def executor = new ScriptRuleExecutor()

    def "test getSupportedRuleType"() {
        expect: "should return SCRIPT type"
        executor.supportedRuleType == RuleType.SCRIPT
    }

    def "test execute simple Groovy script"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "return amount > 1000"

        def context = new RuleExecutionContext()
        context.dataMap = [
            "amount": new TypedValue(2000.0, TypedValue.ValueType.DECIMAL)
        ]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return boolean value true"
        result.getBooleanValue() == true
    }

    def "test execute Groovy script calculating discount"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = """
            def discount = 0.0
            if (amount > 5000) {
                discount = 0.1
            } else if (amount > 2000) {
                discount = 0.05
            }
            return discount
        """

        def context = new RuleExecutionContext()
        context.dataMap = [
            "amount": new TypedValue(3000.0, TypedValue.ValueType.DECIMAL)
        ]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return discount value"
        result.getDecimalValue() == 0.05
    }

    def "test execute Groovy script using system variables"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "return userId == 123L && eventId == 1001"

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        context.traceId = 999L

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return true"
        result.getBooleanValue() == true
    }

    def "test execute Groovy script using context variables"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "return lastResult == true"

        def context = new RuleExecutionContext()
        context.setVariable("lastResult", new TypedValue(true, TypedValue.ValueType.BOOLEAN))

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return true"
        result.getBooleanValue() == true
    }

    def "test execute Groovy script accessing context object"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "return context.userId == 123L"

        def context = new RuleExecutionContext()
        context.userId = 123L

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return true"
        result.getBooleanValue() == true
    }

    def "test execute invalid Groovy script should throw exception"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "invalid groovy syntax {"

        def context = new RuleExecutionContext()

        when: "execute rule"
        executor.execute(rule, context)

        then: "should throw RuntimeException"
        thrown(RuntimeException)
    }
}

