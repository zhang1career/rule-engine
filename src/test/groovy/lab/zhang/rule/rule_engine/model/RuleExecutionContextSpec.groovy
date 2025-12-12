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
        context.arguments != null

        where:
        scenario << [1]
    }

    @Unroll
    def "test create RuleExecutionContext with parameters - userId: #userId, eventId: #eventId, arguments: #arguments"() {
        when: "create execution context"
        def context = new RuleExecutionContext(userId, eventId, arguments)

        then: "should correctly set parameters"
        context.userId == userId
        context.eventId == eventId
        context.arguments == (arguments != null ? arguments : [:])

        where:
        userId | eventId  | arguments
        123L   | 1001     | ["amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)]
        456L   | 1002     | ["age": new TypedValue(25, ValueTypeEnum.INTEGER), "name": new TypedValue("John", ValueTypeEnum.STRING)]
        null   | null     | [:]
        789L   | 1003     | null
    }

    @Unroll
    def "test putArgument and getArgument - key: #key, value: #value, expectedValue: #expectedValue"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "set argument"
        if (value != null) {
            context.putArgument(key, value)
        }

        then: "can get argument"
        context.getArgument(key) == expectedValue

        where:
        key    | value                                               | expectedValue
        "key1" | new TypedValue("test", ValueTypeEnum.STRING) | new TypedValue("test", ValueTypeEnum.STRING)
        "key2" | new TypedValue(100, ValueTypeEnum.INTEGER)   | new TypedValue(100, ValueTypeEnum.INTEGER)
        "key3" | new TypedValue(true, ValueTypeEnum.BOOLEAN)  | new TypedValue(true, ValueTypeEnum.BOOLEAN)
        "key4" | null                                                | null
    }

    @Unroll
    def "test getVariable and setVariable for special properties - key: #key, value: #value, expectedValue: #expectedValue"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "set variable for special property"
        if (value != null) {
            context.setVariable(key, value)
        }

        then: "can get variable and verify special property value"
        context.getVariable(key) == expectedValue
        verifySpecialProperty(context, key, value)

        where:
        key       | value                                             | expectedValue
        "userId"  | new TypedValue(123L, ValueTypeEnum.LONG)      | new TypedValue(123L, ValueTypeEnum.LONG)
        "eventId" | new TypedValue(456, ValueTypeEnum.INTEGER)    | new TypedValue(456, ValueTypeEnum.INTEGER)
        "userId"  | null                                             | new TypedValue(null, ValueTypeEnum.LONG)
        "eventId" | null                                             | new TypedValue(null, ValueTypeEnum.INTEGER)
    }

    @Unroll
    def "test getVariable and setVariable for regular arguments - key: #key, value: #value, expectedValue: #expectedValue"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "set variable for regular argument"
        if (value != null) {
            context.setVariable(key, value)
        }

        then: "can get variable from arguments"
        context.getVariable(key) == expectedValue

        where:
        key       | value                                             | expectedValue
        "regular" | new TypedValue("test", ValueTypeEnum.STRING)  | new TypedValue("test", ValueTypeEnum.STRING)
        "number"  | new TypedValue(100, ValueTypeEnum.INTEGER)    | new TypedValue(100, ValueTypeEnum.INTEGER)
        "regular" | null                                             | null
    }

    private void verifySpecialProperty(RuleExecutionContext context, String key, TypedValue value) {
        switch (key) {
            case "userId":
                assert context.userId == (value != null ? value.getValue() : null)
                break
            case "eventId":
                assert context.eventId == (value != null ? value.getValue() : null)
                break
        }
    }
}
