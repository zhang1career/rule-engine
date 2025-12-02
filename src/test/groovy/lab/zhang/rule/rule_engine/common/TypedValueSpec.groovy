package lab.zhang.rule.rule_engine.common

import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import spock.lang.Specification
import spock.lang.Unroll

/**
 * TypedValue unit test
 */
class TypedValueSpec extends Specification {

    @Unroll
    def "test basic creation and getValue method - value: #value, type: #type, expectedValue: #expectedValue"() {
        when: "create a TypedValue instance"
        def typedValue = new TypedValue(value, type)

        then: "can correctly get value"
        typedValue.getValue() == expectedValue
        typedValue.getType() == expectedType

        where:
        value   | type                         | expectedValue | expectedType
        "test"  | ValueTypeEnum.STRING  | "test"  | ValueTypeEnum.STRING
        "hello" | ValueTypeEnum.STRING  | "hello" | ValueTypeEnum.STRING
        100     | ValueTypeEnum.INTEGER | 100     | ValueTypeEnum.INTEGER
        1000L   | ValueTypeEnum.LONG    | 1000L   | ValueTypeEnum.LONG
        99.99   | ValueTypeEnum.DECIMAL | 99.99   | ValueTypeEnum.DECIMAL
        true    | ValueTypeEnum.BOOLEAN | true    | ValueTypeEnum.BOOLEAN
    }

    @Unroll
    def "test getValue() automatically converts based on internal type - type: #type, value: #value, expectedType: #expectedType, expectedValue: #expectedValue"() {
        given: "create TypedValue instance"
        def typedValue = new TypedValue(value, type)

        when: "call getValue() without parameter"
        def result = typedValue.getValue()

        then: "should return correct type automatically"
        if (value != null) {
            result.class == expectedType
            result == expectedValue
        } else {
            result == null
        }

        where:
        type                         | value   | expectedType  | expectedValue
        ValueTypeEnum.STRING  | "hello" | String.class  | "hello"
        ValueTypeEnum.INTEGER | 100     | Integer.class | 100
        ValueTypeEnum.LONG    | 1000L   | Long.class    | 1000L
        ValueTypeEnum.DECIMAL | 99.99   | Double.class  | 99.99
        ValueTypeEnum.BOOLEAN | true    | Boolean.class | true
        ValueTypeEnum.OBJECT  | null    | null          | null
    }

    @Unroll
    def "test getValue() returns correct type for INTEGER and related types - type: #type, value: #value, expected: #expected"() {
        given: "create TypedValue instance"
        def typedValue = new TypedValue(value, type)

        expect: "getValue() returns value of correct type based on internal type"
        typedValue.getValue() == expected

        where:
        type                         | value | expected
        ValueTypeEnum.INTEGER | 123   | 123
        ValueTypeEnum.LONG    | 456   | 456L
        ValueTypeEnum.DECIMAL | 789.5 | 789.5
        ValueTypeEnum.STRING  | "789" | "789"
        ValueTypeEnum.OBJECT  | null  | null
    }

    @Unroll
    def "test getValue() returns correct type for LONG and related types - type: #type, value: #value, expected: #expected"() {
        given: "create TypedValue instance"
        def typedValue = new TypedValue(value, type)

        expect: "getValue() returns value of correct type based on internal type"
        typedValue.getValue() == expected

        where:
        type                         | value   | expected
        ValueTypeEnum.LONG    | 1000L   | 1000L
        ValueTypeEnum.INTEGER | 500L    | 500
        ValueTypeEnum.DECIMAL | 2000.5  | 2000.5
        ValueTypeEnum.STRING  | "2000L" | "2000L"
    }

    @Unroll
    def "test getValue() returns correct type for DECIMAL and related types - type: #type, value: #value, expected: #expected"() {
        given: "create TypedValue instance"
        def typedValue = new TypedValue(value, type)

        expect: "getValue() returns value of correct type based on internal type"
        typedValue.getValue() == expected

        where:
        type                         | value   | expected
        ValueTypeEnum.DECIMAL | 99.99   | 99.99
        ValueTypeEnum.INTEGER | 100.0   | 100
        ValueTypeEnum.LONG    | 200.0   | 200L
        ValueTypeEnum.STRING  | "88.88" | "88.88"
    }

    @Unroll
    def "test getValue() returns correct type for BOOLEAN and related types - type: #type, value: #value, expected: #expected"() {
        given: "create TypedValue instance"
        def typedValue = new TypedValue(value, type)

        expect: "getValue() returns value of correct type based on internal type"
        typedValue.getValue() == expected

        where:
        type                         | value   | expected
        ValueTypeEnum.BOOLEAN | true    | true
        ValueTypeEnum.BOOLEAN | false   | false
        ValueTypeEnum.STRING  | "true"  | "true"
        ValueTypeEnum.STRING  | "false" | "false"
    }
}
