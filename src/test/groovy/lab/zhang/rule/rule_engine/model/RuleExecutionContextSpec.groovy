package lab.zhang.rule.rule_engine.model

import lab.zhang.rule.rule_engine.common.TypedValue
import spock.lang.Specification

/**
 * RuleExecutionContext unit test
 */
class RuleExecutionContextSpec extends Specification {

    def "test create RuleExecutionContext"() {
        when: "create execution context"
        def context = new RuleExecutionContext()

        then: "should have default values"
        context.variables != null
        context.executionDepth == 0
    }

    def "test create RuleExecutionContext with parameters"() {
        given: "prepare parameters"
        def userId = 123L
        def eventId = 1001
        def traceId = 999L
        def dataMap = [
            "amount": new TypedValue(1000.0, TypedValue.ValueType.DECIMAL)
        ]

        when: "create execution context"
        def context = new RuleExecutionContext(userId, eventId, traceId, dataMap)

        then: "should correctly set parameters"
        context.userId == userId
        context.eventId == eventId
        context.traceId == traceId
        context.dataMap == dataMap
        context.variables != null
        context.executionDepth == 0
    }

    def "test incrementDepth and decrementDepth"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "increment execution depth"
        context.incrementDepth()
        context.incrementDepth()

        then: "execution depth should increase"
        context.executionDepth == 2

        when: "decrement execution depth"
        context.decrementDepth()

        then: "execution depth should decrease"
        context.executionDepth == 1
    }

    def "test execution depth exceeding maximum should throw exception"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "increment execution depth beyond maximum"
        for (int i = 0; i <= 100; i++) {
            context.incrementDepth()
        }

        then: "should throw RuntimeException"
        thrown(RuntimeException)
    }

    def "test setVariable and getVariable"() {
        given: "create execution context"
        def context = new RuleExecutionContext()
        def value = new TypedValue("test", TypedValue.ValueType.STRING)

        when: "set variable"
        context.setVariable("key1", value)

        then: "can get variable"
        context.getVariable("key1") == value
        context.getVariable("key2") == null
    }

    def "test decrementDepth should not be less than 0"() {
        given: "create execution context"
        def context = new RuleExecutionContext()

        when: "decrement execution depth"
        context.decrementDepth()
        context.decrementDepth()

        then: "execution depth should remain 0"
        context.executionDepth == 0
    }
}

