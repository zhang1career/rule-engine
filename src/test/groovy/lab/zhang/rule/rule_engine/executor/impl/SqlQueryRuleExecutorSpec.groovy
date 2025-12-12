package lab.zhang.rule.rule_engine.executor.impl

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import spock.lang.Specification
import spock.lang.Unroll

/**
 * SqlQueryRuleExecutor unit test
 */
class SqlQueryRuleExecutorSpec extends Specification {

    def executor = new SqlQueryRuleExecutor(null) // null JdbcTemplate for testing

    @Unroll
    def "test getSupportedRuleType - expected: #expected"() {
        expect: "should return SQL_QUERY type"
        executor.supportedRuleType == expected

        where:
        expected << [ContentTypeEnum.SQL_QUERY]
    }

    @Unroll
    def "test extractArgs - should extract parameter names from SQL query - sqlContent: #sqlContent, expectedArgs: #expectedArgs"() {
        when: "extract arguments from SQL content"
        def result = executor.extractArgs(sqlContent)

        then: "should return correct argument set"
        result == expectedArgs as Set

        where:
        sqlContent                                                                 | expectedArgs
        "SELECT * FROM users WHERE id = :userId"                                   | ["userId"]
        "SELECT * FROM orders WHERE user_id = :userId AND status = :status"        | ["userId", "status"]
        "SELECT * FROM products WHERE price BETWEEN :minPrice AND :maxPrice"       | ["minPrice", "maxPrice"]
        "SELECT * FROM logs WHERE created_at >= '\${startDate}' AND level = :logLevel" | ["startDate", "logLevel"]
        "SELECT COUNT(*) FROM users"                                               | []
        "SELECT * FROM users WHERE active = 1"                                     | []
    }
}
