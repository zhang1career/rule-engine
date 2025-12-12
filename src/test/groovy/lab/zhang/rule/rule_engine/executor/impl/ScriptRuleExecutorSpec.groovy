package lab.zhang.rule.rule_engine.executor.impl

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import spock.lang.Specification
import spock.lang.Unroll

/**
 * ScriptRuleExecutor unit test
 */
class ScriptRuleExecutorSpec extends Specification {

    def executor = new ScriptRuleExecutor()

    @Unroll
    def "test getSupportedRuleType - expected: #expected"() {
        expect: "should return SCRIPT type"
        executor.supportedRuleType == expected

        where:
        expected << [ContentTypeEnum.SCRIPT]
    }

    @Unroll
    def "test execute Groovy script - scriptContent: #scriptContent, userId: #userId, eventId: #eventId, arguments: #arguments, contextVariables: #contextVariables, expectedValue: #expectedValue"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.content = scriptContent

        def context = new RuleExecutionContext()
        context.userId = userId
        context.eventId = eventId
        context.arguments = arguments ?: [:]
        if (contextVariables != null) {
            contextVariables.each { key, value ->
                context.putArgument(key, value)
            }
        }

        when: "execute rule"
        def result = executor.execute(rule, context)

        then: "should return correct result"
        result.getValue() == expectedValue

        where:
        scriptContent                              | userId | eventId | arguments                                                 | contextVariables                                            | expectedValue
        "return amount > 1000"                     | null   | null    | ["amount": new TypedValue(2000.0, ValueTypeEnum.DECIMAL)] | null                                                        | true
        """
            def discount = 0.0
            if (amount > 5000) {
                discount = 0.1
            } else if (amount > 2000) {
                discount = 0.05
            }
            return discount
        """                                        | null   | null    | ["amount": new TypedValue(3000.0, ValueTypeEnum.DECIMAL)] | null                                                        | 0.05
        "return userId == 123L && eventId == 1001" | 123L   | 1001    | [:]                                                       | null                                                        | true
        "return lastResult == true"                | null   | null    | [:]                                                       | ["lastResult": new TypedValue(true, ValueTypeEnum.BOOLEAN)] | true
        "return context.userId == 123L"            | 123L   | null    | [:]                                                       | null                                                        | true
    }

    @Unroll
    def "test execute invalid Groovy script should throw exception - scriptContent: #scriptContent"() {
        given: "create rule and execution context"
        def rule = new Rule()
        rule.content = scriptContent

        def context = new RuleExecutionContext()

        when: "execute rule"
        executor.execute(rule, context)

        then: "should throw RuntimeException"
        thrown(RuntimeException)

        where:
        scriptContent << [
            "invalid groovy syntax {",
            "unclosed bracket (",
            "undefined syntax error",
            "return invalid expression"
        ]
    }

    @Unroll
    def "test extractArgs - should extract variable names from Groovy script - scriptContent: #scriptContent, expectedArgs: #expectedArgs"() {
        when: "extract arguments from script"
        def result = executor.extractArgs(scriptContent)

        then: "should return correct argument set"
        result == expectedArgs as Set

        where:
        scriptContent                               | expectedArgs
        "return amount > 1000"                      | ["amount"]
        "return amount > 1000 && age >= 18"         | ["amount", "age"]
        "return userId == 123L && eventId == 1001"  | ["userId", "eventId"]
        "return lastResult == true"                 | ["lastResult"]
        """
        def discount = 0.0
        if (amount > 5000) {
            discount = 0.1
        }
        return discount
        """                                         | ["amount"]
        "return context.userId == 123L"             | ["context"]
        "return true"                               | []
        ""                                          | []
    }
}
