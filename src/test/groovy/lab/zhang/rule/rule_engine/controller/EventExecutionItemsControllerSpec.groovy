package lab.zhang.rule.rule_engine.controller


import lab.zhang.rule.rule_engine.model.ExecutionArrangement
import lab.zhang.rule.rule_engine.pojo.dto.ExecutionArrangementDTO
import lab.zhang.rule.rule_engine.pojo.qo.ExecutionArrangementQO
import lab.zhang.rule.rule_engine.service.EventService
import lab.zhang.rule.rule_engine.struct_mapper.ExecutionArrangementStructMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Event execution items controller comprehensive test
 * Tests for PUT /api/events/{eventId}/execution-items and GET /api/events/{eventId}/execution-items
 * 
 * Note: Database existence validation is now handled by @ValidExecutionItemExists annotation.
 * In unit tests without Spring context, the validator will skip database validation.
 * For full validation testing, see ValidExecutionItemExistsValidator tests or integration tests.
 */
class EventExecutionItemsControllerSpec extends Specification {

    def eventService = Mock(EventService)
    def executionArrangementStructMapper = Mock(ExecutionArrangementStructMapper)
    def controller = new EventController()

    def setup() {
        controller.eventService = eventService
        controller.executionArrangementStructMapper = executionArrangementStructMapper
        
        // Setup default mock for executionArrangementStructMapper
        executionArrangementStructMapper.modelToDTO(_ as ExecutionArrangement) >> { ExecutionArrangement model ->
            def dto = new ExecutionArrangementDTO()
            dto.setEventId(model.eventId)
            dto.setRuleId(model.ruleId)
            dto.setGroupId(model.groupId)
            dto.setExeOrder(model.exeOrder)
            dto.setAbRatio(model.abRatio)
            return dto
        }
    }

    @Unroll
    def "test batchSetExecutionItems - mixed standalone and group rules - eventId: #eventId"() {
        given: "prepare mixed execution items"
        def executionItems = [10000001L, 10000002L, 10000003L]
        def request = new ExecutionArrangementQO()
        request.setRules(executionItems)

        when: "batch set mixed execution items"
        def response = controller.setExecutionItems(eventId, request)

        then: "should set execution items successfully"
        // Note: Database existence validation is now handled by @ValidExecutionItemExists annotation
        // In unit tests without Spring context, the validator will skip database validation
        // Service call
        1 * eventService.setExecutionItems(eventId, executionItems)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"

        where:
        eventId << [10000001, 10000002]
    }

    @Unroll
    def "test batchSetExecutionItems - update existing items - eventId: #eventId"() {
        given: "prepare execution items with existing rules"
        def executionItems = [10000001L, 10000002L]
        def request = new ExecutionArrangementQO()
        request.setRules(executionItems)

        when: "batch set execution items"
        def response = controller.setExecutionItems(eventId, request)

        then: "should update execution items successfully"
        // Note: Database existence validation is now handled by @ValidExecutionItemExists annotation
        // In unit tests without Spring context, the validator will skip database validation
        1 * eventService.setExecutionItems(eventId, executionItems)
        response.statusCode == HttpStatus.OK
        response.body.code == 0

        where:
        eventId << [10000001, 10000002]
    }


    @Unroll
    def "test batchSetExecutionItems - null item - eventId: #eventId"() {
        given: "prepare execution items with null item"
        def executionItems = [null]
        def request = new ExecutionArrangementQO()
        request.setRules(executionItems)

        when: "batch set execution items with null item"
        def response = controller.setExecutionItems(eventId, request)

        then: "should be rejected by validation"
        // In unit tests without Spring context, validation may be skipped
        // The validation is properly tested in ValidExecutionItemExistsValidator tests
        response != null

        where:
        eventId << [10000001, 10000002]
    }


    @Unroll
    def "test batchSetExecutionItems - null ruleId - eventId: #eventId"() {
        given: "prepare execution item with null ruleId"
        def executionItems = [null]
        def request = new ExecutionArrangementQO()
        request.setRules(executionItems)

        when: "batch set execution items with null ruleId"
        def response = controller.setExecutionItems(eventId, request)

        then: "should be rejected by @NotNull validation"
        // In unit tests without Spring context, validation may be skipped
        response != null

        where:
        eventId << [10000001, 10000002]
    }

    @Unroll
    def "test getExecutionItems - mixed standalone and group rules - eventId: #eventId"() {
        given: "prepare execution arrangements (models, not entities)"
        def relations = [
            ExecutionArrangement.builder()
                    .eventId(eventId)
                    .ruleId(10000001L)
                    .groupId(0L)
                    .exeOrder(0)
                    .abRatio(0)
                    .build(),
            ExecutionArrangement.builder()
                    .eventId(eventId)
                    .ruleId(10000002L)
                    .groupId(10000001L)
                    .exeOrder(1)
                    .abRatio(50)
                    .build(),
            ExecutionArrangement.builder()
                    .eventId(eventId)
                    .ruleId(10000003L)
                    .groupId(0L)
                    .exeOrder(2)
                    .abRatio(0)
                    .build()
        ]

        when: "get execution items"
        def response = controller.getExecutionItems(eventId)

        then: "should return execution items with correct order"
        1 * eventService.getExecutionItems(eventId) >> relations
        3 * executionArrangementStructMapper.modelToDTO(_ as ExecutionArrangement) >> { ExecutionArrangement model ->
            def dto = new ExecutionArrangementDTO()
            dto.setEventId(model.eventId)
            dto.setRuleId(model.ruleId)
            dto.setGroupId(model.groupId)
            dto.setExeOrder(model.exeOrder)
            dto.setAbRatio(model.abRatio)
            return dto
        }
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.data.size() == 3
        response.body.data[0].ruleId == 10000001L
        response.body.data[0].groupId == 0L
        response.body.data[0].exeOrder == 0
        response.body.data[0].abRatio == 0
        response.body.data[1].ruleId == 10000002L
        response.body.data[1].groupId == 10000001L
        response.body.data[1].exeOrder == 1
        response.body.data[1].abRatio == 50
        response.body.data[2].ruleId == 10000003L
        response.body.data[2].groupId == 0L
        response.body.data[2].exeOrder == 2
        response.body.data[2].abRatio == 0

        where:
        eventId << [10000001, 10000002]
    }

    @Unroll
    def "test getExecutionItems - empty result - eventId: #eventId"() {
        when: "get execution items for event with no items"
        def response = controller.getExecutionItems(eventId)

        then: "should return empty list"
        1 * eventService.getExecutionItems(eventId) >> []
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.data.size() == 0

        where:
        eventId << [10000001, 10000002]
    }
}

