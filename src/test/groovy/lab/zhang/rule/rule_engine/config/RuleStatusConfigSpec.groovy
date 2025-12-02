package lab.zhang.rule.rule_engine.config

import lab.zhang.rule.rule_engine.enums.EnvironmentEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.util.EnvUtil
import spock.lang.Specification
import spock.lang.Unroll

/**
 * RuleStatusConfig unit test
 *
 * @author Rongjin Zhang
 */
class RuleStatusConfigSpec extends Specification {

    def envUtil = Mock(EnvUtil)
    def ruleStatusConfig = new RuleStatusConfig()

    def setup() {
        ruleStatusConfig.envUtil = envUtil
    }

    @Unroll
    def "test getAllowedRuleStatuses - should return correct statuses for environment - environment: #environment, expectedStatuses: #expectedStatuses"() {
        when: "get allowed rule statuses"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return correct statuses for the environment"
        1 * envUtil.getEnvEnum() >> environment
        result != null
        result.size() == expectedStatuses.size()
        result.containsAll(expectedStatuses)
        expectedStatuses.every { result.contains(it) }

        where:
        environment              | expectedStatuses
        EnvironmentEnum.TEST    | [RuleStatusEnum.TEST]
        EnvironmentEnum.GRAY    | [RuleStatusEnum.GRAY, RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL]
        EnvironmentEnum.PRODUCTION | [RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL]
    }

    def "test getAllowedRuleStatuses - should return default statuses for UNDEFINED environment"() {
        when: "get allowed rule statuses for UNDEFINED environment"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return default OFFLINE status"
        1 * envUtil.getEnvEnum() >> EnvironmentEnum.UNDEFINED
        result != null
        result.size() == 1
        result.contains(RuleStatusEnum.OFFLINE)
    }

    def "test getAllowedRuleStatuses - should return default statuses when environment is null"() {
        when: "get allowed rule statuses when environment is null"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return default OFFLINE status"
        1 * envUtil.getEnvEnum() >> null
        result != null
        result.size() == 1
        result.contains(RuleStatusEnum.OFFLINE)
    }

    def "test getAllowedRuleStatuses - should return immutable set"() {
        when: "get allowed rule statuses"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return a set"
        1 * envUtil.getEnvEnum() >> EnvironmentEnum.TEST
        result != null
        result instanceof Set

        and: "should be able to check contains"
        result.contains(RuleStatusEnum.TEST)
        !result.contains(RuleStatusEnum.FULL)
    }

    def "test getAllowedRuleStatuses - TEST environment should only allow TEST status"() {
        when: "get allowed rule statuses for TEST environment"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should only return TEST status"
        1 * envUtil.getEnvEnum() >> EnvironmentEnum.TEST
        result.size() == 1
        result.contains(RuleStatusEnum.TEST)
        !result.contains(RuleStatusEnum.FULL)
        !result.contains(RuleStatusEnum.AB_TEST)
        !result.contains(RuleStatusEnum.GRAY)
        !result.contains(RuleStatusEnum.OFFLINE)
    }

    def "test getAllowedRuleStatuses - GRAY environment should allow GRAY, AB_TEST, and FULL statuses"() {
        when: "get allowed rule statuses for GRAY environment"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return GRAY, AB_TEST, and FULL statuses"
        1 * envUtil.getEnvEnum() >> EnvironmentEnum.GRAY
        result.size() == 3
        result.contains(RuleStatusEnum.GRAY)
        result.contains(RuleStatusEnum.AB_TEST)
        result.contains(RuleStatusEnum.FULL)
        !result.contains(RuleStatusEnum.TEST)
        !result.contains(RuleStatusEnum.OFFLINE)
    }

    def "test getAllowedRuleStatuses - PRODUCTION environment should allow AB_TEST and FULL statuses"() {
        when: "get allowed rule statuses for PRODUCTION environment"
        def result = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return AB_TEST and FULL statuses"
        1 * envUtil.getEnvEnum() >> EnvironmentEnum.PRODUCTION
        result.size() == 2
        result.contains(RuleStatusEnum.AB_TEST)
        result.contains(RuleStatusEnum.FULL)
        !result.contains(RuleStatusEnum.TEST)
        !result.contains(RuleStatusEnum.GRAY)
        !result.contains(RuleStatusEnum.OFFLINE)
    }

    @Unroll
    def "test getAllowedRuleStatuses - multiple calls should return same result - environment: #environment"() {
        when: "get allowed rule statuses multiple times"
        def result1 = ruleStatusConfig.getAllowedRuleStatuses()
        def result2 = ruleStatusConfig.getAllowedRuleStatuses()
        def result3 = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return consistent results"
        3 * envUtil.getEnvEnum() >> environment
        result1 == result2
        result2 == result3
        result1.size() == result2.size()
        result1.containsAll(result2)

        where:
        environment << [EnvironmentEnum.TEST, EnvironmentEnum.GRAY, EnvironmentEnum.PRODUCTION]
    }

    def "test getAllowedRuleStatuses - should handle all environment types"() {
        when: "get allowed rule statuses for each environment"
        def testResult = ruleStatusConfig.getAllowedRuleStatuses()
        def grayResult = ruleStatusConfig.getAllowedRuleStatuses()
        def productionResult = ruleStatusConfig.getAllowedRuleStatuses()
        def undefinedResult = ruleStatusConfig.getAllowedRuleStatuses()

        then: "should return correct statuses for each environment"
        1 * envUtil.getEnvEnum() >> EnvironmentEnum.TEST
        testResult.contains(RuleStatusEnum.TEST)

        1 * envUtil.getEnvEnum() >> EnvironmentEnum.GRAY
        grayResult.containsAll([RuleStatusEnum.GRAY, RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL])

        1 * envUtil.getEnvEnum() >> EnvironmentEnum.PRODUCTION
        productionResult.containsAll([RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL])

        1 * envUtil.getEnvEnum() >> EnvironmentEnum.UNDEFINED
        undefinedResult.contains(RuleStatusEnum.OFFLINE)
    }
}

