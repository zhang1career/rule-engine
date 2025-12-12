package lab.zhang.rule.rule_engine.executor.impl

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import spock.lang.Specification
import spock.lang.Unroll

/**
 * ApiQueryRuleExecutor unit test
 */
class ApiQueryRuleExecutorSpec extends Specification {

    def executor = new ApiQueryRuleExecutor()

    @Unroll
    def "test getSupportedRuleType - expected: #expected"() {
        expect: "should return API_QUERY type"
        executor.supportedRuleType == expected

        where:
        expected << [ContentTypeEnum.API_QUERY]
    }

    @Unroll
    def "test extractArgs - should extract parameter names from API query JSON - jsonContent: #jsonContent, expectedArgs: #expectedArgs"() {
        when: "extract arguments from JSON content"
        def result = executor.extractArgs(jsonContent)

        then: "should return correct argument set"
        result == expectedArgs as Set

        where:
        jsonContent | expectedArgs
        '{"url": "http://api.example.com/users/${userId}", "method": "GET"}' | ["userId"]
        '{"url": "http://api.example.com/users/:userId/orders/:orderId", "method": "GET"}' | ["userId", "orderId"]
        '{"url": "http://api.example.com/search", "method": "POST", "body": "{\\"query\\": \\"${searchTerm}\\", \\"limit\\": ${limit}}"}' | ["searchTerm", "limit"]
        '{"url": "http://api.example.com/data", "method": "GET", "headers": {"Authorization": "Bearer ${token}", "X-API-Key": ":apiKey"}}' | ["token", "apiKey"]
        '{"url": "http://api.example.com/simple", "method": "GET"}' | []
        'invalid json' | []
    }
}
