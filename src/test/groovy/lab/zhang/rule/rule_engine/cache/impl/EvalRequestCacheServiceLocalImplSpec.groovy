package lab.zhang.rule.rule_engine.cache.impl


import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.util.HashUtil
import spock.lang.Specification

/**
 * EvalCacheServiceImpl unit test
 */
class EvalRequestCacheServiceLocalImplSpec extends Specification {

    def cacheService = new EvalCacheServiceLocalImpl()

    def setup() {
        // Initialize with test configuration
        cacheService.cacheCapacity = 1000
        cacheService.cacheTtl = 60
        cacheService.keyLength = 16
        cacheService.init()
    }

    def "test buildCacheKey - should generate consistent key for same inputs"() {
        given: "a rule and argument values"
        def rule = Rule.builder()
                .id(123L)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .contentArgList(["userId", "amount"])
                .build()

        def argValues = ["456", "100.50"]

        when: "build cache key multiple times"
        def key1 = cacheService.buildCacheKey(rule.getId().toString(), argValues)
        def key2 = cacheService.buildCacheKey(rule.getId().toString(), argValues)

        then: "keys should be identical"
        key1 == key2
        key1.startsWith("rule:eval:123:")
        key1.length() > "rule:eval:123:".length()
    }

    def "test buildCacheKey - should generate different keys for different arguments"() {
        given: "a rule with different argument values"
        def rule = Rule.builder()
                .id(123L)
                .contentArgList(["userId", "amount"])
                .build()

        def argValues1 = ["456", "100.50"]
        def argValues2 = ["789", "200.00"]

        when: "build cache keys"
        def key1 = cacheService.buildCacheKey(rule.getId().toString(), argValues1)
        def key2 = cacheService.buildCacheKey(rule.getId().toString(), argValues2)

        then: "keys should be different"
        key1 != key2
    }

    def "test put and get - should cache and retrieve values"() {
        given: "a rule and argument values"
        def rule = Rule.builder()
                .id(123L)
                .contentArgList(["userId"])
                .build()

        def argValues = ["456"]
        def result = new TypedValue("test result", ValueTypeEnum.STRING)

        when: "put value in cache and then get it"
        cacheService.put(rule, argValues, result)
        def cachedResult = cacheService.get(rule, argValues)

        then: "should retrieve the cached value"
        cachedResult == result
        cachedResult.getValue() == "test result"
        cachedResult.getType() == ValueTypeEnum.STRING
    }

    def "test get - should return null for non-existent key"() {
        given: "a rule and argument values that haven't been cached"
        def rule = Rule.builder()
                .id(999L)
                .contentArgList(["userId"])
                .build()

        def argValues = ["999"]

        when: "try to get non-existent value"
        def result = cacheService.get(rule, argValues)

        then: "should return null"
        result == null
    }

    def "test buildCacheKey with null keyPrefix - should throw exception"() {
        given: "a null keyPrefix and argument values"
        def keyPrefix = null
        def argValues = ["456"]

        when: "build cache key"
        def result = cacheService.buildCacheKey(keyPrefix, argValues)

        then: "should throw exception"
        thrown(IllegalArgumentException)
    }

    def "test buildCacheKey with blank keyPrefix - should throw exception"() {
        given: "a null keyPrefix and argument values"
        def keyPrefix = " "
        def argValues = ["456"]

        when: "build cache key"
        def result = cacheService.buildCacheKey(keyPrefix, argValues)

        then: "should throw exception"
        thrown(IllegalArgumentException)
    }

    def "test buildCacheKey with null argValues - should throw exception"() {
        given: "a rule with null argument values"
        def rule = Rule.builder()
                .id(123L)
                .contentArgList(["userId"])
                .build()

        when: "build cache key with null argValues"
        cacheService.buildCacheKey(rule.getId().toString(), null)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test buildCacheKey with empty argValues - should throw exception"() {
        given: "a rule with null argument values"
        def rule = Rule.builder()
                .id(123L)
                .contentArgList(["userId"])
                .build()

        when: "build cache key with null argValues"
        cacheService.buildCacheKey(rule.getId().toString(), Collections.emptyList())

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }
}
