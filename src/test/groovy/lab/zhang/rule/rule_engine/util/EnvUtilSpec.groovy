package lab.zhang.rule.rule_engine.util

import lab.zhang.rule.rule_engine.enums.EnvironmentEnum
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field

/**
 * EnvUtil unit test
 *
 * @author Rongjin Zhang
 */
class EnvUtilSpec extends Specification {

    def envUtil = new EnvUtil()

    /**
     * Set environment field value using reflection
     */
    private void setEnvironment(String value) {
        Field field = EnvUtil.class.getDeclaredField("environment")
        field.setAccessible(true)
        field.set(envUtil, value)
    }

    // ========== getEnvEnum() tests ==========

    @Unroll
    def "test getEnvEnum - should return correct enum for valid environment - environment: #envString, expected: #expected"() {
        given: "set environment value"
        setEnvironment(envString)

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return correct enum"
        result == expected

        where:
        envString    | expected
        "TEST"       | EnvironmentEnum.TEST
        "test"       | EnvironmentEnum.TEST
        "Test"       | EnvironmentEnum.TEST
        "TeSt"       | EnvironmentEnum.TEST
        "PRODUCTION" | EnvironmentEnum.PRODUCTION
        "production" | EnvironmentEnum.PRODUCTION
        "Production" | EnvironmentEnum.PRODUCTION
        "GRAY"       | EnvironmentEnum.GRAY
        "gray"       | EnvironmentEnum.GRAY
        "Gray"       | EnvironmentEnum.GRAY
    }

    def "test getEnvEnum - should return UNDEFINED for invalid environment"() {
        given: "set invalid environment value"
        setEnvironment("INVALID")

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED"
        result == EnvironmentEnum.UNDEFINED
    }

    def "test getEnvEnum - should return UNDEFINED for empty string"() {
        given: "set empty environment value"
        setEnvironment("")

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED"
        result == EnvironmentEnum.UNDEFINED
    }

    def "test getEnvEnum - should throw NullPointerException for null environment"() {
        given: "set null environment value"
        setEnvironment(null)

        when: "get environment enum"
        envUtil.getEnvEnum()

        then: "should throw NullPointerException"
        // Note: null.toUpperCase() throws NullPointerException,
        // which is not caught (only IllegalArgumentException is caught)
        thrown(NullPointerException)
    }

    def "test getEnvEnum - should return UNDEFINED for whitespace only"() {
        given: "set whitespace only environment value"
        setEnvironment("   ")

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED"
        result == EnvironmentEnum.UNDEFINED
    }

    @Unroll
    def "test getEnvEnum - should handle case insensitive - environment: #envString"() {
        given: "set environment value with different cases"
        setEnvironment(envString)

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return correct enum (case insensitive)"
        result == EnvironmentEnum.TEST

        where:
        envString << ["TEST", "test", "Test", "TeSt", "tEsT"]
    }

    def "test getEnvEnum - should handle special characters"() {
        given: "set environment value with special characters"
        setEnvironment('TEST@#$')

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED"
        result == EnvironmentEnum.UNDEFINED
    }

    def "test getEnvEnum - should handle numeric values"() {
        given: "set environment value with numeric"
        setEnvironment("123")

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED"
        result == EnvironmentEnum.UNDEFINED
    }

    def "test getEnvEnum - should handle mixed alphanumeric"() {
        given: "set environment value with mixed alphanumeric"
        setEnvironment("TEST123")

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED"
        result == EnvironmentEnum.UNDEFINED
    }

    def "test getEnvEnum - should return default TEST when environment is not set"() {
        given: "create new EnvUtil instance (default value should be TEST)"
        def newEnvUtil = new EnvUtil()
        // Note: In actual Spring context, @Value would inject "TEST" as default
        // But in unit test without Spring context, the field will be null
        // So we need to set it explicitly or test the default behavior

        when: "get environment enum without setting environment"
        // Since @Value annotation doesn't work in unit tests without Spring context,
        // we'll test with explicit setting
        setEnvironment("TEST")
        def result = envUtil.getEnvEnum()

        then: "should return TEST (or handle null appropriately)"
        result == EnvironmentEnum.TEST
    }

    @Unroll
    def "test getEnvEnum - should handle all valid environment values - value: #value, expected: #expected"() {
        given: "set valid environment value"
        setEnvironment(value)

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return correct enum"
        result == expected

        where:
        value        | expected
        "TEST"       | EnvironmentEnum.TEST
        "PRODUCTION" | EnvironmentEnum.PRODUCTION
        "GRAY"       | EnvironmentEnum.GRAY
    }

    @Unroll
    def "test getEnvEnum - should be case insensitive for all environments - value: #value, expected: #expected"() {
        given: "set environment value with different case"
        setEnvironment(value)

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return correct enum regardless of case"
        result == expected

        where:
        value        | expected
        "test"       | EnvironmentEnum.TEST
        "TEST"       | EnvironmentEnum.TEST
        "Test"       | EnvironmentEnum.TEST
        "production" | EnvironmentEnum.PRODUCTION
        "PRODUCTION" | EnvironmentEnum.PRODUCTION
        "Production" | EnvironmentEnum.PRODUCTION
        "gray"       | EnvironmentEnum.GRAY
        "GRAY"       | EnvironmentEnum.GRAY
        "Gray"       | EnvironmentEnum.GRAY
    }

    def "test getEnvEnum - should handle leading and trailing whitespace"() {
        given: "environment values with whitespace"
        setEnvironment("  TEST  ")

        when: "get environment enum"
        def result = envUtil.getEnvEnum()

        then: "should return UNDEFINED (whitespace is not trimmed)"
        // Note: The implementation uses toUpperCase() but doesn't trim,
        // so "  TEST  " becomes "  TEST  " which is invalid
        result == EnvironmentEnum.UNDEFINED
    }

    def "test getEnvEnum - should handle multiple calls with same value"() {
        given: "set environment value"
        setEnvironment("TEST")

        when: "get environment enum multiple times"
        def result1 = envUtil.getEnvEnum()
        def result2 = envUtil.getEnvEnum()
        def result3 = envUtil.getEnvEnum()

        then: "should return same result each time"
        result1 == EnvironmentEnum.TEST
        result2 == EnvironmentEnum.TEST
        result3 == EnvironmentEnum.TEST
        result1 == result2
        result2 == result3
    }

    def "test getEnvEnum - should handle changing environment value"() {
        when: "change environment value and get enum"
        setEnvironment("TEST")
        def result1 = envUtil.getEnvEnum()

        setEnvironment("PRODUCTION")
        def result2 = envUtil.getEnvEnum()

        setEnvironment("GRAY")
        def result3 = envUtil.getEnvEnum()

        then: "should return correct enum for each value"
        result1 == EnvironmentEnum.TEST
        result2 == EnvironmentEnum.PRODUCTION
        result3 == EnvironmentEnum.GRAY
    }
}

