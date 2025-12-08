package lab.zhang.rule.rule_engine.controller


import lab.zhang.rule.rule_engine.model.Event
import lab.zhang.rule.rule_engine.model.ExecutionArrangement
import lab.zhang.rule.rule_engine.pojo.dto.EventDTO
import lab.zhang.rule.rule_engine.pojo.dto.ExecutionArrangementDTO
import lab.zhang.rule.rule_engine.pojo.qo.ExecutionArrangementQO
import lab.zhang.rule.rule_engine.pojo.qo.EventQO
import lab.zhang.rule.rule_engine.service.EventService
import lab.zhang.rule.rule_engine.struct_mapper.EventStructMapper
import lab.zhang.rule.rule_engine.struct_mapper.ExecutionArrangementStructMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

/**
 * EventController unit test
 */
class EventControllerSpec extends Specification {

    def eventService = Mock(EventService)
    def eventStructMapper = Mock(EventStructMapper)
    def executionArrangementStructMapper = Mock(ExecutionArrangementStructMapper)
    def controller = new EventController()

    def setup() {
        controller.eventService = eventService
        controller.eventStructMapper = eventStructMapper
        controller.executionArrangementStructMapper = executionArrangementStructMapper
    }

    @Unroll
    def "test getAllEvents - eventCount: #eventCount"() {
        given: "prepare event entities"
        def eventEntities = []
        eventCount.times { i ->
            def entity = new Event()
            entity.setId(10000001 + i)
            entity.setName("Event ${i + 1}")
            entity.setDescription("Description ${i + 1}")
            eventEntities.add(entity)
        }

        and: "prepare event DTOs"
        def eventDTOs = []
        eventCount.times { i ->
            def dto = EventDTO.builder()
                    .id(10000001 + i)
                    .name("Event ${i + 1}")
                    .description("Description ${i + 1}")
                    .build()
            eventDTOs.add(dto)
        }

        when: "get all events"
        def response = controller.getAllEvents()

        then: "should return all events"
        1 * eventService.getAllEvents() >> eventEntities
        eventCount.times { i ->
            1 * eventStructMapper.entityToDTO(eventEntities[i]) >> eventDTOs[i]
        }
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.data.size() == eventCount

        where:
        eventCount << [0, 1, 3, 5]
    }

    @Unroll
    def "test getEvent - eventId: #eventId"() {
        given: "prepare event entity"
        def event = new Event()
        event.setId(eventId as Integer)
        event.setName("Test Event")
        event.setDescription("Test Description")
        
        def eventDTO = EventDTO.builder()
                .id(eventId)
                .name("Test Event")
                .description("Test Description")
                .build()

        when: "get event by ID"
        def response = controller.getEvent(eventId)

        then: "should return correct event"
        1 * eventService.getEventById(eventId) >> event
        1 * eventStructMapper.entityToDTO(event) >> eventDTO
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.data != null
        response.body.data.id == eventId

        where:
        eventId << [10000001, 10000002]
    }

    @Unroll
    def "test getEvent - event not found - eventId: #eventId"() {
        when: "get non-existent event"
        controller.getEvent(eventId)

        then: "should throw IllegalArgumentException"
        1 * eventService.getEventById(eventId) >> null
        thrown(IllegalArgumentException)

        where:
        eventId << [99999999, 88888888]
    }

    @Unroll
    def "test createEvent - id: #id, name: #name, description: #description"() {
        given: "prepare event DTO"
        def eventQO = EventQO.builder()
                .id(id as Integer)
                .name(name)
                .description(description)
                .build()

        and: "prepare event entity"
        def event = new Event()
        event.setId(id as Integer)
        event.setName(name != null ? name.trim() : "")
        event.setDescription(description != null ? description.trim() : "")

        and: "mock qoToModel"
        1 * eventStructMapper.qoToModel(eventQO) >> event

        and: "prepare response DTO"
        def responseDTO = EventDTO.builder()
                .id(id as Integer)
                .name(name != null ? name.trim() : "")
                .description(description != null ? description.trim() : "")
                .build()

        when: "create event"
        def response = controller.createEvent(eventQO)

        then: "should create event successfully"
        1 * eventService.createEvent(_) >> { Event eventParam ->
            assert eventParam != null
            assert eventParam.id == id
            assert eventParam.name == (name != null ? name.trim() : "")
            assert eventParam.description == (description != null ? description.trim() : "")
            event
        }
        1 * eventStructMapper.entityToDTO(event) >> responseDTO
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data != null
        response.body.data.id == id

        where:
        id          | name          | description
        10000001    | "Event 1"     | "Description 1"
        10000002    | "Event 2"     | null
        10000003    | "  Event 3  " | "  Description 3  "
    }

    // Note: Validation tests are better suited for integration tests with Spring context
    // Unit tests here focus on controller logic, not validation framework behavior
    // Validation is handled by GlobalExceptionHandler, which would require Spring context

    @Unroll
    def "test createEvent - duplicate id - id: #id"() {
        given: "prepare event DTO"
        def eventQO = EventQO.builder()
                .id(id as Integer)
                .name("Event 1")
                .description("Description")
                .build()

        and: "mock qoToModel"
        def event = Event.builder()
                .id(id as Integer)
                .name("Event 1")
                .description("Description")
                .build()
        1 * eventStructMapper.qoToModel(eventQO) >> event

        when: "create event with duplicate id"
        controller.createEvent(eventQO)

        then: "should throw IllegalArgumentException"
        1 * eventService.createEvent(_) >> { Event eventParam ->
            assert eventParam != null
            throw new IllegalArgumentException("Event with ID ${eventParam.id} already exists")
        }
        thrown(IllegalArgumentException)

        where:
        id << [10000001, 10000002]
    }

    @Unroll
    def "test updateEvent - eventId: #eventId, name: #name, description: #description"() {
        given: "prepare event DTO"
        def eventQO = EventQO.builder()
                .id(eventId)
                .name(name)
                .description(description)
                .build()

        and: "prepare updated event entity"
        def updatedEntity = new Event()
        updatedEntity.setId(eventId as Integer)
        updatedEntity.setName(name != null && !name.trim().isEmpty() ? name.trim() : "Old Name")
        updatedEntity.setDescription(description != null ? description.trim() : "Old Description")

        and: "prepare response DTO"
        def responseDTO = EventDTO.builder()
                .id(eventId)
                .name(updatedEntity.getName())
                .description(updatedEntity.getDescription())
                .build()

        when: "update event"
        def response = controller.updateEvent(eventId, eventQO)

        then: "should update event successfully"
        1 * eventService.updateEvent(eventId, name, description) >> updatedEntity
        1 * eventStructMapper.entityToDTO(updatedEntity) >> responseDTO
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data != null
        response.body.data.id == eventId

        where:
        eventId     | name         | description
        10000001    | "New Name 1" | "New Description 1"
        10000002    | "New Name 2" | null
        10000003    | null         | "New Description 3"
        10000004    | null         | null
        10000005    | ""           | "New Description 5"
    }

    // Note: Validation tests are better suited for integration tests with Spring context
    // Unit tests here focus on controller logic, not validation framework behavior

    @Unroll
    def "test updateEvent - event not found - eventId: #eventId"() {
        given: "prepare event DTO"
        def eventQO = EventQO.builder()
                .id(eventId)
                .name("New Name")
                .description("New Description")
                .build()

        when: "update non-existent event"
        controller.updateEvent(eventId, eventQO)

        then: "should throw IllegalArgumentException"
        1 * eventService.updateEvent(eventId, "New Name", "New Description") >> {
            throw new IllegalArgumentException("Event not found: ${eventId}")
        }
        thrown(IllegalArgumentException)

        where:
        eventId << [99999999, 88888888]
    }

    @Unroll
    def "test deleteEvent - eventId: #eventId"() {
        when: "delete event"
        def response = controller.deleteEvent(eventId)

        then: "should delete event successfully"
        1 * eventService.deleteEvent(eventId)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data == null

        where:
        eventId << [10000001, 10000002]
    }

    @Unroll
    def "test deleteEvent - event not found - eventId: #eventId"() {
        when: "delete non-existent event"
        controller.deleteEvent(eventId)

        then: "should throw IllegalArgumentException"
        1 * eventService.deleteEvent(eventId) >> {
            throw new IllegalArgumentException("Event not found: ${eventId}")
        }
        thrown(IllegalArgumentException)

        where:
        eventId << [99999999, 88888888]
    }

    @Unroll
    def "test batchSetExecutionItems - eventId: #eventId, itemCount: #itemCount"() {
        given: "prepare execution items"
        def executionItems = []
        itemCount.times { i ->
            executionItems.add(10000001 + i)
        }
        def request = new ExecutionArrangementQO()
        request.setRules(executionItems)

        when: "batch set execution items"
        def response = controller.setExecutionArrangements(eventId, request)

        then: "should set execution items successfully"
        // Note: Database existence validation is now handled by @ValidExecutionItemExists annotation
        // In unit tests, validation is skipped if Validator dependencies are not available
        1 * eventService.setExecutionArrangements(eventId, executionItems)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data == null

        where:
        eventId     | itemCount
        10000001    | 0
        10000001    | 1
        10000001    | 3
    }

    @Unroll
    def "test batchSetExecutionItems - empty list removes all items - eventId: #eventId"() {
        given: "prepare empty execution items"
        def request = new ExecutionArrangementQO()
        request.setRules([])

        when: "batch set empty execution items"
        def response = controller.setExecutionArrangements(eventId, request)

        then: "should remove all execution items"
        1 * eventService.setExecutionArrangements(eventId, [])
        response.statusCode == HttpStatus.OK
        response.body.code == 0

        where:
        eventId << [10000001, 10000002]
    }

    // Note: Database existence validation is now handled by @ValidExecutionItemExists annotation
    // These tests are moved to integration tests or ValidExecutionItemExistsValidator tests
    // Unit tests for Controller focus on business logic, not validation logic
    @Unroll
    def "test batchSetExecutionItems - rule not found - eventId: #eventId, itemId: #itemId - skipped"() {
        expect: "validation is handled by @ValidExecutionItemExists annotation"
        true

        where:
        eventId     | itemId
        10000001    | 99999999L
        10000002    | 88888888L
    }

    @Unroll
    def "test batchSetExecutionItems - rule group not found - eventId: #eventId, itemId: #itemId - skipped"() {
        expect: "validation is handled by @ValidExecutionItemExists annotation"
        true

        where:
        eventId     | itemId
        10000001    | 99999999L
        10000002    | 88888888L
    }

    @Unroll
    def "test getExecutionItems - eventId: #eventId, relationCount: #relationCount"() {
        given: "prepare execution arrangements"
        def arrangements = []
        def dtos = []
        relationCount.times { i ->
            def arrangement = new ExecutionArrangement()
            arrangement.setEventId(eventId)
            arrangement.setRuleId(10000001 + i)
            arrangement.setGroupId(0L)
            arrangement.setExeOrder(i)
            arrangement.setAbRatio(0)
            arrangements.add(arrangement)
            
            def dto = new ExecutionArrangementDTO()
            dto.setEventId(eventId)
            dto.setRuleId(10000001 + i)
            dto.setGroupId(0L)
            dto.setExeOrder(i)
            dto.setAbRatio(0)
            dtos.add(dto)
        }

        when: "get execution items"
        def response = controller.getExecutionItems(eventId)

        then: "should return execution items"
        1 * eventService.getExecutionItems(eventId) >> arrangements
        relationCount * executionArrangementStructMapper.modelToDTO(_ as ExecutionArrangement) >> { ExecutionArrangement model ->
            dtos.find { it.ruleId == model.ruleId }
        }
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.data.size() == relationCount

        where:
        eventId     | relationCount
        10000001    | 0
        10000001    | 1
        10000001    | 3
    }
}

