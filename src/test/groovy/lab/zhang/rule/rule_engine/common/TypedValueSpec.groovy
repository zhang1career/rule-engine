package lab.zhang.rule.rule_engine.common

import spock.lang.Specification
import spock.lang.Unroll

/**
 * TypedValue unit test
 */
class TypedValueSpec extends Specification {

    def "test basic creation and getValue method of TypedValue"() {
        when: "create a TypedValue instance"
        def typedValue = new TypedValue("test", TypedValue.ValueType.STRING)

        then: "can correctly get value"
        typedValue.getValue() == "test"
        typedValue.getType() == TypedValue.ValueType.STRING
    }

    @Unroll
    def "test type conversion of TypedValue - type: #type, value: #value, expected: #expected"() {
        given: "create TypedValue instance"
        def typedValue = new TypedValue(value, type)

        expect: "type conversion result is correct"
        typedValue.getValue()== expected

        where:
        type                          | value      | expected
        TypedValue.ValueType.STRING   | "hello"    | "hello"
        TypedValue.ValueType.INTEGER  | 100        | 100
        TypedValue.ValueType.LONG     | 1000L      | 1000L
        TypedValue.ValueType.DECIMAL  | 99.99      | 99.99
        TypedValue.ValueType.BOOLEAN  | true       | true
        TypedValue.ValueType.OBJECT   | null       | null
    }

    def "test getStringValue method"() {
        given: "create TypedValue of different types"
        def stringValue = new TypedValue("test", TypedValue.ValueType.STRING)
        def intValue = new TypedValue(123, TypedValue.ValueType.INTEGER)
        def nullValue = new TypedValue(null, TypedValue.ValueType.OBJECT)

        expect: "can correctly convert to string"
        stringValue.getStringValue() == "test"
        intValue.getStringValue() == "123"
        nullValue.getStringValue() == null
    }

    def "test getIntegerValue method"() {
        given: "create TypedValue of different types"
        def intValue = new TypedValue(123, TypedValue.ValueType.INTEGER)
        def longValue = new TypedValue(456L, TypedValue.ValueType.LONG)
        def stringValue = new TypedValue("789", TypedValue.ValueType.STRING)
        def nullValue = new TypedValue(null, TypedValue.ValueType.OBJECT)

        expect: "can correctly convert to integer"
        intValue.getIntegerValue() == 123
        longValue.getIntegerValue() == 456
        stringValue.getIntegerValue() == 789
        nullValue.getIntegerValue() == null
    }

    def "test getLongValue method"() {
        given: "create TypedValue of different types"
        def longValue = new TypedValue(1000L, TypedValue.ValueType.LONG)
        def intValue = new TypedValue(500, TypedValue.ValueType.INTEGER)
        def stringValue = new TypedValue("2000", TypedValue.ValueType.STRING)

        expect: "can correctly convert to long"
        longValue.getLongValue() == 1000L
        intValue.getLongValue() == 500L
        stringValue.getLongValue() == 2000L
    }

    def "test getDecimalValue method"() {
        given: "create TypedValue of different types"
        def decimalValue = new TypedValue(99.99, TypedValue.ValueType.DECIMAL)
        def intValue = new TypedValue(100, TypedValue.ValueType.INTEGER)
        def stringValue = new TypedValue("88.88", TypedValue.ValueType.STRING)

        expect: "can correctly convert to decimal"
        decimalValue.getDecimalValue() == 99.99
        intValue.getDecimalValue() == 100.0
        stringValue.getDecimalValue() == 88.88
    }

    def "test getBooleanValue method"() {
        given: "create TypedValue of different types"
        def boolTrue = new TypedValue(true, TypedValue.ValueType.BOOLEAN)
        def boolFalse = new TypedValue(false, TypedValue.ValueType.BOOLEAN)
        def stringTrue = new TypedValue("true", TypedValue.ValueType.STRING)
        def stringFalse = new TypedValue("false", TypedValue.ValueType.STRING)

        expect: "can correctly convert to boolean"
        boolTrue.getBooleanValue() == true
        boolFalse.getBooleanValue() == false
        stringTrue.getBooleanValue() == true
        stringFalse.getBooleanValue() == false
    }
}

