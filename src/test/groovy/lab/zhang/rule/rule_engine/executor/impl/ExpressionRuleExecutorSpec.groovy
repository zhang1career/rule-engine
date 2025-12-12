package lab.zhang.rule.rule_engine.executor.impl

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * ExpressionRuleExecutor unit test
 */
class ExpressionRuleExecutorSpec extends Specification {

    def executor = new ExpressionRuleExecutor()

    @Unroll
    def "test getSupportedRuleType - expected: #expected"() {
        expect: "should return EXPRESSION type"
        executor.supportedRuleType == expected

        where:
        expected << [ContentTypeEnum.EXPRESSION]
    }

    @Unroll
    def "test execute expression - expression: #expression, userId: #userId, eventId: #eventId, arguments: #arguments, expectedValue: #expectedValue, expectedType: #expectedType"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.content = expression

        def context = new RuleExecutionContext()
        context.userId = userId
        context.eventId = eventId
        context.arguments = arguments ?: [:]
        
        when: "execute rule"
        def result = executor.execute(rule, context)
        
        then: "should return correct result"
        result != null
        result.getType() == expectedType
        result.getValue() == expectedValue
        
        where:
        expression                          | userId | eventId | arguments                                                                                                   | expectedValue | expectedType
        "amount > 1000 && age >= 18"        | 123L   | 1001    | ["amount": new TypedValue(2000.0, ValueTypeEnum.DECIMAL), "age": new TypedValue(25, ValueTypeEnum.INTEGER)] | true          | ValueTypeEnum.BOOLEAN
        "amount > 1000"                     | null   | null    | ["amount": new TypedValue(500.0, ValueTypeEnum.DECIMAL)]                                                    | false         | ValueTypeEnum.BOOLEAN
        "amount * 0.1"                      | null   | null    | ["amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)]                                                   | 100.0         | ValueTypeEnum.DECIMAL
        "userId == 123L && eventId == 1001" | 123L   | 1001    | [:]                                                                                                         | true          | ValueTypeEnum.BOOLEAN
    }

    @Unroll
    def "test execute expression using context variables - expression: #expression, contextVarName: #contextVarName, contextVarValue: #contextVarValue, expectedValue: #expectedValue"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.content = expression

        def context = new RuleExecutionContext()
        if (contextVarName != null && contextVarValue != null) {
            context.putArgument(contextVarName, contextVarValue)
        }

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return correct result"
        result.getValue() == expectedValue

        where:
        expression           | contextVarName | contextVarValue                                     | expectedValue
        "lastResult == true" | "lastResult"   | new TypedValue(true, ValueTypeEnum.BOOLEAN)  | true
        "!lastResult"        | "lastResult"   | new TypedValue(false, ValueTypeEnum.BOOLEAN) | true
        "lastResult != true" | "lastResult"   | new TypedValue(false, ValueTypeEnum.BOOLEAN) | true
        "lastAmount > 0"     | "lastAmount"   | new TypedValue(100.0, ValueTypeEnum.DECIMAL) | true
    }

    @Unroll
    def "test execute invalid expression should throw exception - expression: #expression"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.content = expression

        def context = new RuleExecutionContext()

        when: "execute rule"
        executor.execute(rule, context)

        then: "should throw RuntimeException"
        thrown(RuntimeException)

        where:
        expression << [
            "invalid expression syntax {",
            "unclosed bracket (",
            "undefined variable xyz",
            "syntax error invalid chars"
        ]
    }

    @Unroll
    def "test result type conversion for different types - expression: #expression, arguments: #arguments, expectedType: #expectedType"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.content = expression

        def context = new RuleExecutionContext()
        context.arguments = arguments ?: [:]

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "result type should be correct"
        result.getType() == expectedType

        where:
        expression     | arguments                                                       | expectedType
        "true"         | [:]                                                                    | ValueTypeEnum.BOOLEAN
        "100"          | [:]                                                                    | ValueTypeEnum.INTEGER
        "1000L"        | [:]                                                                    | ValueTypeEnum.LONG
        "99.99"        | [:]                                                                    | ValueTypeEnum.DECIMAL
        "'hello'"      | [:]                                                                    | ValueTypeEnum.STRING
        "amount > 100" | ["amount": new TypedValue(200.0, ValueTypeEnum.DECIMAL)] | ValueTypeEnum.BOOLEAN
    }

    @Unroll
    def "test extractArgs - should extract variable names from expression - expression: #expression, expectedArgs: #expectedArgs"() {
        when: "extract arguments from expression"
        def result = executor.extractArgs(expression)

        then: "should return correct argument set"
        result == expectedArgs as Set

        where:
        expression                          | expectedArgs
        "amount > 1000"                     | ["amount"]
        "amount > 1000 && age >= 18"        | ["amount", "age"]
        "userId == 123L && eventId == 1001" | ["userId", "eventId"]
        "lastResult == true"                | ["lastResult"]
        "amount * 0.1"                      | ["amount"]
        "true"                              | []
        "100 > 50"                          | []
        ""                                  | []
    }
}
