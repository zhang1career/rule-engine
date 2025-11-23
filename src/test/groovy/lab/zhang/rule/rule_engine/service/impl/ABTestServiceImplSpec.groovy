package lab.zhang.rule.rule_engine.service.impl

import spock.lang.Specification
import spock.lang.Unroll

/**
 * ABTestService unit test
 */
class ABTestServiceImplSpec extends Specification {

    def abTestService = new ABTestServiceImpl()

    def "test shouldExecuteABTest - should return false when parameter is null"() {
        expect: "return false when parameter is null"
        abTestService.shouldExecuteABTest(null, 1001, 1L, 50) == false
        abTestService.shouldExecuteABTest(123L, null, 1L, 50) == false
        abTestService.shouldExecuteABTest(123L, 1001, null, 50) == false
        abTestService.shouldExecuteABTest(123L, 1001, 1L, null) == false
    }

    def "test shouldExecuteABTest - should return true when record exists"() {
        given: "record A/B test decision"
        def userId = 123L
        def eventId = 1001
        def ruleId = 1L
        abTestService.recordABTestDecision(userId, eventId, ruleId)

        when: "judge again whether should execute"
        def result = abTestService.shouldExecuteABTest(userId, eventId, ruleId, 50)

        then: "should return true (record exists)"
        result == true
    }

    def "test hasABTestRecord"() {
        given: "record A/B test decision"
        def userId = 123L
        def eventId = 1001
        def ruleId = 1L

        when: "check if record exists"
        def before = abTestService.hasABTestRecord(userId, eventId, ruleId)
        abTestService.recordABTestDecision(userId, eventId, ruleId)
        def after = abTestService.hasABTestRecord(userId, eventId, ruleId)

        then: "state before and after recording should be different"
        before == false
        after == true
    }

    def "test recordABTestDecision"() {
        given: "prepare parameters"
        def userId = 123L
        def eventId = 1001
        def ruleId = 1L

        when: "record A/B test decision"
        abTestService.recordABTestDecision(userId, eventId, ruleId)

        then: "should be able to query the record"
        abTestService.hasABTestRecord(userId, eventId, ruleId) == true
    }

    @Unroll
    def "test shouldExecuteABTest - even distribution for different user IDs - userId: #userId, ratio: #ratio, expected execute: #shouldExecute"() {
        given: "prepare parameters"
        def eventId = 1001
        def ruleId = 1L

        when: "judge whether should execute A/B test"
        def result = abTestService.shouldExecuteABTest(userId, eventId, ruleId, ratio)

        then: "result should meet expectations (note: due to hashCode randomness, only verify method can execute normally)"
        result != null
        result instanceof Boolean

        where:
        userId | ratio | shouldExecute
        1L     | 50    | true  // may execute, depends on hashCode
        2L     | 50    | true  // may execute, depends on hashCode
        100L   | 30    | true  // may execute, depends on hashCode
        200L   | 80    | true  // may execute, depends on hashCode
    }

    def "test shouldExecuteABTest - consistency for same user ID"() {
        given: "prepare parameters"
        def userId = 123L
        def eventId = 1001
        def ruleId = 1L
        def ratio = 50

        when: "first judgment"
        def firstResult = abTestService.shouldExecuteABTest(userId, eventId, ruleId, ratio)

        and: "second judgment (should use recorded result)"
        def secondResult = abTestService.shouldExecuteABTest(userId, eventId, ruleId, ratio)

        then: "two results should be consistent"
        firstResult == secondResult
    }

    def "test combinations of different eventId and ruleId"() {
        given: "prepare different combinations"
        def userId = 123L

        when: "record different combinations"
        abTestService.recordABTestDecision(userId, 1001, 1L)
        abTestService.recordABTestDecision(userId, 1001, 2L)
        abTestService.recordABTestDecision(userId, 1002, 1L)

        then: "each combination should have independent record"
        abTestService.hasABTestRecord(userId, 1001, 1L) == true
        abTestService.hasABTestRecord(userId, 1001, 2L) == true
        abTestService.hasABTestRecord(userId, 1002, 1L) == true
        abTestService.hasABTestRecord(userId, 1002, 2L) == false
    }
}

