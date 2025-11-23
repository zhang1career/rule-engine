package lab.zhang.rule.rule_engine.service.impl

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.dto.EvalRequest
import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.service.MessageQueueService
import spock.lang.Specification

/**
 * EvalService unit test
 */
class EvalServiceImplSpec extends Specification {

    def ruleExecutionEngine = Mock(RuleExecutionEngine)
    def messageQueueService = Mock(MessageQueueService)
    def evalService = new EvalServiceImpl()

    def setup() {
        evalService.ruleExecutionEngine = ruleExecutionEngine
        evalService.messageQueueService = messageQueueService
    }

    def "test eval method - normal execution"() {
        given: "prepare request parameters"
        def request = new EvalRequest()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.dataMap = [
            "amount": new TypedValue(1000.0, TypedValue.ValueType.DECIMAL),
            "age": new TypedValue(25, TypedValue.ValueType.INTEGER)
        ]

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute eval method"
        def result = evalService.eval(request)

        then: "should call rule execution engine and return result"
        1 * ruleExecutionEngine.execute(1001, _ as RuleExecutionContext) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult)
        result == expectedResult
    }

    def "test eval method - message queue send failure should not affect main flow"() {
        given: "prepare request parameters"
        def request = new EvalRequest()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.dataMap = [:]

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute eval method"
        def result = evalService.eval(request)

        then: "should return result even if message queue send fails"
        1 * ruleExecutionEngine.execute(1001, _ as RuleExecutionContext) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult) >> {
            throw new RuntimeException("RabbitMQ connection failed")
        }
        result == expectedResult
    }

    def "test eval method - verify execution context construction"() {
        given: "prepare request parameters"
        def request = new EvalRequest()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.dataMap = [
            "amount": new TypedValue(1000.0, TypedValue.ValueType.DECIMAL)
        ]

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, TypedValue.ValueType.BOOLEAN)

        when: "execute eval method"
        evalService.eval(request)

        then: "verify execution context parameters"
        1 * ruleExecutionEngine.execute(1001, { RuleExecutionContext context ->
            context.userId == 123L &&
            context.eventId == 1001 &&
            context.traceId == 999L &&
            context.dataMap.size() == 1 &&
            context.dataMap["amount"].value == 1000.0
        }) >> expectedResult
    }

    def "test eval method - rule execution engine throws exception"() {
        given: "prepare request parameters"
        def request = new EvalRequest()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.dataMap = [:]

        when: "execute eval method"
        evalService.eval(request)

        then: "should throw exception"
        1 * ruleExecutionEngine.execute(1001, _ as RuleExecutionContext) >> {
            throw new RuntimeException("Rule execution failed")
        }
        thrown(RuntimeException)
    }
}

