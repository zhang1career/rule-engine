package lab.zhang.rule.rule_engine.controller

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.dto.EvalRequest
import lab.zhang.rule.rule_engine.dto.EvalResponse
import lab.zhang.rule.rule_engine.service.EvalService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

/**
 * RuleController unit test
 */
class RuleControllerSpec extends Specification {

    def evalService = Mock(EvalService)
    def controller = new RuleController()

    def setup() {
        controller.evalService = evalService
    }

    def "test eval interface - normal case"() {
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

        when: "call eval interface"
        def response = controller.eval(request)

        then: "should return success response"
        1 * evalService.eval(request) >> expectedResult
        response.statusCode == HttpStatus.OK
        response.body.success == true
        response.body.result == expectedResult
    }

    def "test eval interface - service throws exception"() {
        given: "prepare request parameters"
        def request = new EvalRequest()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.dataMap = [:]

        when: "call eval interface"
        def response = controller.eval(request)

        then: "should return error response"
        1 * evalService.eval(request) >> {
            throw new RuntimeException("Rule execution failed")
        }
        response.statusCode == HttpStatus.OK
        response.body.success == false
        response.body.errorMessage != null
    }

    def "test eval interface - verify request parameter passing"() {
        given: "prepare request parameters"
        def request = new EvalRequest()
        request.userId = 123L
        request.eventId = 1001
        request.traceId = 999L
        request.dataMap = [
            "amount": new TypedValue(2000.0, TypedValue.ValueType.DECIMAL),
            "age": new TypedValue(25, TypedValue.ValueType.INTEGER)
        ]

        and: "prepare execution result"
        def expectedResult = new TypedValue(0.05, TypedValue.ValueType.DECIMAL)

        when: "call eval interface"
        def response = controller.eval(request)

        then: "should correctly pass request parameters"
        1 * evalService.eval({ EvalRequest req ->
            req.userId == 123L &&
            req.eventId == 1001 &&
            req.traceId == 999L &&
            req.dataMap.size() == 2
        }) >> expectedResult
        response.body.result == expectedResult
    }
}

