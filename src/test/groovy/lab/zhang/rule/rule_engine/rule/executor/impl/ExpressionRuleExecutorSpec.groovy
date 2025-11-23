package lab.zhang.rule.rule_engine.executor.impl

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.RuleType
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * ExpressionRuleExecutor unit test
 */
class ExpressionRuleExecutorSpec extends Specification {

    def executor = new ExpressionRuleExecutor()

    def "test getSupportedRuleType"() {
        expect: "should return EXPRESSION type"
        executor.supportedRuleType == RuleType.EXPRESSION
    }

    def "test execute simple boolean expression"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "amount > 1000 && age >= 18"

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        context.traceId = 999L
        context.dataMap = [
            "amount": new TypedValue(2000.0, TypedValue.ValueType.DECIMAL),
            "age": new TypedValue(25, TypedValue.ValueType.INTEGER)
        ]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return boolean value true"
        result != null
        result.type == TypedValue.ValueType.BOOLEAN
        result.getBooleanValue() == true
    }

    def "test execute expression returning false"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "amount > 1000"

        def context = new RuleExecutionContext()
        context.dataMap = [
            "amount": new TypedValue(500.0, TypedValue.ValueType.DECIMAL)
        ]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return boolean value false"
        result.getBooleanValue() == false
    }

    def "test execute arithmetic expression"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "amount * 0.1"

        def context = new RuleExecutionContext()
        context.dataMap = [
            "amount": new TypedValue(1000.0, TypedValue.ValueType.DECIMAL)
        ]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return calculation result"
        result.getDecimalValue() == 100.0
    }

    def "test execute expression using system variables"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "userId == 123L && eventId == 1001"

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        context.traceId = 999L

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return true"
        result.getBooleanValue() == true
    }

    def "test execute expression using context variables"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "lastResult == true"

        def context = new RuleExecutionContext()
        context.setVariable("lastResult", new TypedValue(true, TypedValue.ValueType.BOOLEAN))

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return true"
        result.getBooleanValue() == true
    }

    def "test execute invalid expression should throw exception"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = "invalid expression syntax {"

        def context = new RuleExecutionContext()

        when: "execute rule"
        executor.execute(rule, context)

        then: "should throw RuntimeException"
        thrown(RuntimeException)
    }

    @Unroll
    def "test result type conversion for different types - expression: #expression, expected type: #expectedType"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.ruleId = 1L
        rule.ruleContent = expression

        def context = new RuleExecutionContext()
        context.dataMap = dataMap ?: [:]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "result type should be correct"
        result.type == expectedType

        where:
        expression          | dataMap                                                              | expectedType
        "true"              | [:]                                                                  | TypedValue.ValueType.BOOLEAN
        "100"               | [:]                                                                  | TypedValue.ValueType.INTEGER
        "1000L"             | [:]                                                                  | TypedValue.ValueType.LONG
        "99.99"             | [:]                                                                  | TypedValue.ValueType.DECIMAL
        "'hello'"           | [:]                                                                  | TypedValue.ValueType.STRING
        "amount > 100"      | ["amount": new TypedValue(200.0, TypedValue.ValueType.DECIMAL)]     | TypedValue.ValueType.BOOLEAN
    }
}

