package lab.zhang.rule.rule_engine.service.impl

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst
import lab.zhang.rule.rule_engine.engine.ExecutionTrace
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO
import lab.zhang.rule.rule_engine.engine.RuleExecutionEngine
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.service.MessageQueueService
import spock.lang.Specification
import spock.lang.Unroll

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

    @Unroll
    def "test eval method - normal execution - userId: #userId, eventId: #eventId, traceId: #traceId"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        request.userId = userId
        request.eventId = eventId
        request.traceId = traceId
        request.arguments = [
            "amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL),
            "age": new TypedValue(25, ValueTypeEnum.INTEGER)
        ]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        def result = evalService.eval(request, trace)

        then: "should call rule execution engine and return result"
        1 * ruleExecutionEngine.execute(eventId, _ as RuleExecutionContext, trace) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult)
        result == expectedResult

        where:
        userId | eventId | traceId
        123L   | 1001    | 999L
        456L   | 1002    | 888L
        789L   | 1003    | 777L
    }

    @Unroll
    def "test eval method - message queue send failure should not affect main flow - errorMessage: #errorMessage"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.arguments = [:]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        def result = evalService.eval(request, trace)

        then: "should return result even if message queue send fails"
        1 * ruleExecutionEngine.execute(1001, _ as RuleExecutionContext, trace) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult) >> {
            throw new RuntimeException(errorMessage)
        }
        result == expectedResult

        where:
        errorMessage << [
            "RabbitMQ connection failed",
            "Message queue timeout",
            "Serialization error"
        ]
    }

    @Unroll
    def "test eval method - verify execution context construction - userId: #userId, eventId: #eventId, traceId: #traceId, argKey: #argKey, argValue: #argValue"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        request.userId = userId
        request.eventId = eventId
        request.traceId = traceId
        request.arguments = [
            (argKey): new TypedValue(argValue, ValueTypeEnum.DECIMAL)
        ]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        evalService.eval(request, trace)

        then: "verify execution context parameters"
        1 * ruleExecutionEngine.execute(eventId, { RuleExecutionContext context ->
            context.userId == userId &&
            context.eventId == eventId &&
            context.traceId == traceId &&
            context.arguments.size() == (userId != null ? 3 : 1) && // userId != null adds userHash and userHashInt
            context.arguments[argKey].value == argValue
        }, trace) >> expectedResult

        where:
        userId | eventId | traceId | argKey   | argValue
        123L   | 1001    | 999L    | "amount" | 1000.0
        456L   | 1002    | 888L    | "price"  | 2000.0
        789L   | 1003    | 777L    | "total"  | 3000.0
    }

    @Unroll
    def "test eval method - rule execution engine throws exception - eventId: #eventId, errorMessage: #errorMessage"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        request.userId = 123L
        request.eventId = eventId
        request.traceId = 999L
        request.arguments = [:]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        when: "execute eval method"
        evalService.eval(request, trace)

        then: "should throw exception"
        1 * ruleExecutionEngine.execute(eventId, _ as RuleExecutionContext, trace) >> {
            throw new RuntimeException(errorMessage)
        }
        thrown(RuntimeException)

        where:
        eventId | errorMessage
        1001    | "Rule execution failed"
        1002    | "No rules found for eventId"
        1003    | "Rule execution timeout"
    }

    def "test eval method - userId is null should not calculate userHash"() {
        given: "prepare request parameters with null userId"
        def request = new EvalDTO()
        request.userId = null
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [
            "amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)
        ]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        def result = evalService.eval(request, trace)

        then: "should not calculate userHash and userHashInt"
        1 * ruleExecutionEngine.execute(1001L, { RuleExecutionContext context ->
            context.userId == null &&
            context.eventId == 1001L &&
            context.traceId == 999L &&
            context.arguments.size() == 1 && // Only the amount argument, no userHash
            !context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH) &&
            !context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH_INT)
        }, trace) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult)
        result == expectedResult
    }

    def "test eval method - userId is not null should calculate userHash and userHashInt"() {
        given: "prepare request parameters with userId"
        def request = new EvalDTO()
        def userId = 12345L
        request.userId = userId
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [
            "amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)
        ]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        def result = evalService.eval(request, trace)

        then: "should calculate userHash and userHashInt and add to context"
        1 * ruleExecutionEngine.execute(1001L, { RuleExecutionContext context ->
            context.userId == userId &&
            context.eventId == 1001L &&
            context.traceId == 999L &&
            context.arguments.size() == 3 && // amount + userHash + userHashInt
            context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH) &&
            context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH_INT) &&
            context.arguments[EvalArgumentConst.ARG_USER_HASH].type == ValueTypeEnum.STRING &&
            context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].type == ValueTypeEnum.INTEGER &&
            context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].value instanceof Integer &&
            (Integer) context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].value >= 1 &&
            (Integer) context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].value <= 100
        }, trace) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult)
        result == expectedResult
    }

    @Unroll
    def "test eval method - verify userHash calculation for different userIds - userId: #userId"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        request.userId = userId
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [:]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        evalService.eval(request, trace)

        then: "should calculate consistent userHash for same userId"
        1 * ruleExecutionEngine.execute(1001L, { RuleExecutionContext context ->
            context.userId == userId &&
            context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH) &&
            context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH_INT) &&
            context.arguments[EvalArgumentConst.ARG_USER_HASH].value instanceof String &&
            !context.arguments[EvalArgumentConst.ARG_USER_HASH].value.toString().isEmpty() &&
            context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].value instanceof Integer
        }, trace) >> expectedResult

        where:
        userId << [1L, 100L, 1000L, 999999L, 123456789L]
    }

    def "test eval method - verify userHashInt is in range 1-100"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        request.userId = 12345L
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [:]

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        and: "collect hashInt values"
        def hashIntValues = []

        when: "execute eval method multiple times"
        10.times {
            def trace = new ExecutionTrace()
            evalService.eval(request, trace)
        }

        then: "userHashInt should always be in range 1-100"
        10 * ruleExecutionEngine.execute(1001L, { RuleExecutionContext context ->
            def hashInt = (Integer) context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].value
            hashIntValues.add(hashInt)
            hashInt >= 1 && hashInt <= 100
        }, _ as ExecutionTrace) >> expectedResult
        10 * messageQueueService.sendEvalResult(request, expectedResult)

        and: "all hashInt values should be in valid range"
        hashIntValues.every { it >= 1 && it <= 100 }
        hashIntValues.size() == 10
    }

    def "test eval method - verify same userId produces same userHash"() {
        given: "prepare request parameters"
        def request = new EvalDTO()
        def userId = 12345L
        request.userId = userId
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [:]

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        and: "prepare lists to collect userHash values"
        def userHashList = []
        def userHashIntList = []

        when: "execute eval method twice with same userId"
        def trace1 = new ExecutionTrace()
        evalService.eval(request, trace1)
        def trace2 = new ExecutionTrace()
        evalService.eval(request, trace2)

        then: "userHash should be consistent for same userId"
        2 * ruleExecutionEngine.execute(_ as Long, { RuleExecutionContext context ->
            userHashList << context.arguments[EvalArgumentConst.ARG_USER_HASH].value.toString()
            userHashIntList << (Integer) context.arguments[EvalArgumentConst.ARG_USER_HASH_INT].value
            true
        }, _ as ExecutionTrace) >> expectedResult
        2 * messageQueueService.sendEvalResult(request, expectedResult)

        and: "userHash and userHashInt should be the same for same userId"
        userHashList.size() == 2
        userHashIntList.size() == 2
        userHashList[0] == userHashList[1]
        userHashIntList[0] == userHashIntList[1]
    }

    def "test eval method - verify arguments are correctly passed to context"() {
        given: "prepare request parameters with multiple arguments"
        def request = new EvalDTO()
        request.userId = 123L
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [
            "amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL),
            "age": new TypedValue(25, ValueTypeEnum.INTEGER),
            "name": new TypedValue("John", ValueTypeEnum.STRING),
            "isVip": new TypedValue(true, ValueTypeEnum.BOOLEAN)
        ]

        and: "prepare execution trace"
        def trace = new ExecutionTrace()

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)

        when: "execute eval method"
        evalService.eval(request, trace)

        then: "all arguments should be correctly passed to context"
        1 * ruleExecutionEngine.execute(1001L, { RuleExecutionContext context ->
            context.arguments.size() == 6 && // 4 custom args + userHash + userHashInt
            context.arguments["amount"].value == 1000.0 &&
            context.arguments["age"].value == 25 &&
            context.arguments["name"].value == "John" &&
            context.arguments["isVip"].value == true &&
            context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH) &&
            context.arguments.containsKey(EvalArgumentConst.ARG_USER_HASH_INT)
        }, trace) >> expectedResult
        1 * messageQueueService.sendEvalResult(request, expectedResult)
    }
}

