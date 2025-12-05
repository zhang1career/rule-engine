package lab.zhang.rule.rule_engine.controller

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.engine.ExecutionTrace
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.model.EvalResult
import lab.zhang.rule.rule_engine.pojo.dto.EvalDTO
import lab.zhang.rule.rule_engine.pojo.qo.EvalQO
import lab.zhang.rule.rule_engine.service.EvalService
import lab.zhang.rule.rule_engine.struct_mapper.EvalStructMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification

/**
 * RuleController unit test
 */
class EvalControllerSpec extends Specification {

    def evalService = Mock(EvalService)
    def evalStructMapper = Mock(EvalStructMapper)
    def controller = new EvalController()

    def setup() {
        controller.evalService = evalService
        controller.evalStructMapper = evalStructMapper
    }

    def "test eval interface - normal case"() {
        given: "prepare request parameters"
        def request = new EvalQO()
        request.userId = 123L
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [
            "amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)
        ]

        and: "prepare DTO for service call"
        def dto = new EvalDTO()
        dto.userId = 123L
        dto.eventId = 1001L
        dto.traceId = 999L
        dto.arguments = [
            "amount": new TypedValue(1000.0, ValueTypeEnum.DECIMAL)
        ]

        and: "prepare execution result"
        def expectedResult = new TypedValue(true, ValueTypeEnum.BOOLEAN)
        def trace = new ExecutionTrace()
        def evalResult = new EvalResult(expectedResult, trace)

        when: "call eval interface"
        def response = controller.eval(request)

        then: "should return success response"
        1 * evalStructMapper.qoToDto(request) >> dto
        1 * evalService.eval(dto) >> evalResult
        response.statusCode == HttpStatus.OK
        response.body.result == expectedResult
        response.body.briefSteps == evalResult.getBriefSteps()
    }

    def "test eval interface - service throws exception"() {
        given: "prepare request parameters"
        def request = new EvalQO()
        request.userId = 123L
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [:]

        and: "prepare DTO for service call"
        def dto = new EvalDTO()
        dto.userId = 123L
        dto.eventId = 1001L
        dto.traceId = 999L
        dto.arguments = [:]

        when: "call eval interface"
        controller.eval(request)

        then: "should throw exception"
        1 * evalStructMapper.qoToDto(request) >> dto
        1 * evalService.eval(dto) >> {
            throw new RuntimeException("Rule execution failed")
        }
        thrown(RuntimeException)
    }

    def "test eval interface - verify request parameter passing"() {
        given: "prepare request parameters"
        def request = new EvalQO()
        request.userId = 123L
        request.eventId = 1001L
        request.traceId = 999L
        request.arguments = [
            "amount": new TypedValue(2000.0, ValueTypeEnum.DECIMAL),
            "age": new TypedValue(25, ValueTypeEnum.INTEGER)
        ]

        and: "prepare DTO for service call"
        def dto = new EvalDTO()
        dto.userId = 123L
        dto.eventId = 1001L
        dto.traceId = 999L
        dto.arguments = [
            "amount": new TypedValue(2000.0, ValueTypeEnum.DECIMAL),
            "age": new TypedValue(25, ValueTypeEnum.INTEGER)
        ]

        and: "prepare execution result"
        def expectedResult = new TypedValue(0.05, ValueTypeEnum.DECIMAL)
        def trace = new ExecutionTrace()
        def evalResult = new EvalResult(expectedResult, trace)

        when: "call eval interface"
        def response = controller.eval(request)

        then: "should correctly pass request parameters"
        1 * evalStructMapper.qoToDto(request) >> dto
        1 * evalService.eval({ EvalDTO req ->
            req.userId == 123L &&
            req.eventId == 1001L &&
            req.traceId == 999L &&
            req.arguments.size() == 2
        }) >> evalResult
        response.body.result == expectedResult
        response.body.briefSteps == evalResult.getBriefSteps()
    }
}

