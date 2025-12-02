package lab.zhang.rule.rule_engine.model

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import spock.lang.Specification
import spock.lang.Unroll

/**
 * RuleExecutionContext unit test
 */
class RuleExecutionContextSpec extends Specification {

    @Unroll
    def "test create RuleExecutionContext - should have default values"() {
        when: "create execution context"
        def context = new RuleExecutionContext()

        then: "should have default values"
        context.variables != null
        context.arguments != null

        where:
        scenario << [1]
    }

    @Unroll
    def "test create RuleExecutionContext with parameters - userId: #userId, eventId: #eventId, traceId: #traceId, arguments: #arguments"() {
        when: "create execution context"
        def context = new RuleExecutionContext(userId, eventId, traceId, arguments)

        then: "should correctly set parameters"
        context.userId == userId
        context.eventId == eventId
        context.traceId == traceId
        context.arguments == (arguments != null ? arguments : [:])
        context.variables != null

        where:
        userId | eventId | traceId | arguments
        123L   | 1001    | 999L    | ["amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)]
        456L   | 1002    | 888L    | ["age": new TypedValue(25, ValueTypeEnum.INTEGER), "name": new TypedValue("John", ValueTypeEnum.STRING)]
        null   | null    | null    | [:]
        789L   | 1003    | 777L    | null
    }

    @Unroll
    def "test setVariable and getVariable - key: #key, value: #value, expectedValue: #expectedValue"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "set variable"
        if (value != null) {
            context.setVariable(key, value)
        }

        then: "can get variable"
        context.getVariable(key) == expectedValue

        where:
        key    | value                                               | expectedValue
        "key1" | new TypedValue("test", ValueTypeEnum.STRING) | new TypedValue("test", ValueTypeEnum.STRING)
        "key2" | new TypedValue(100, ValueTypeEnum.INTEGER)   | new TypedValue(100, ValueTypeEnum.INTEGER)
        "key3" | new TypedValue(true, ValueTypeEnum.BOOLEAN)  | new TypedValue(true, ValueTypeEnum.BOOLEAN)
        "key4" | null                                                | null
    }
}
