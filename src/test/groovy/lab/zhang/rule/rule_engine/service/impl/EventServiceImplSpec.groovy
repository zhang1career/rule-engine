package lab.zhang.rule.rule_engine.service.impl


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import lab.zhang.rule.rule_engine.entity.EventEntity
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity
import lab.zhang.rule.rule_engine.entity.RuleEntity
import lab.zhang.rule.rule_engine.config.RuleStatusConfig
import lab.zhang.rule.rule_engine.enums.ExecutionArrangementTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.mapper.EventMapper
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper
import lab.zhang.rule.rule_engine.mapper.RuleMapper
import lab.zhang.rule.rule_engine.model.Event
import lab.zhang.rule.rule_engine.struct_mapper.EventStructMapper
import lab.zhang.rule.rule_engine.struct_mapper.ExecutionArrangementStructMapper
import spock.lang.Specification
import spock.lang.Unroll

/**
 * EventService unit test
 */
class EventServiceImplSpec extends Specification {

    def eventMapper = Mock(EventMapper)
    def executionArrangementMapper = Mock(ExecutionArrangementMapper)
    def ruleMapper = Mock(RuleMapper)
    def eventStructMapper = Mock(EventStructMapper)
    def executionArrangementStructMapper = Mock(ExecutionArrangementStructMapper)
    def ruleStatusConfig = Mock(RuleStatusConfig)
    def eventService = new EventServiceImpl()

    def setup() {
        eventService.eventMapper = eventMapper
        eventService.executionArrangementMapper = executionArrangementMapper
        eventService.ruleMapper = ruleMapper
        eventService.eventStructMapper = eventStructMapper
        eventService.executionArrangementStructMapper = executionArrangementStructMapper
        eventService.ruleStatusConfig = ruleStatusConfig

        // Setup default mock for eventStructMapper
        _ * eventStructMapper.modelToEntity(_ as Event) >> { Event event ->
            def entity = new EventEntity()
            entity.setId(event.id)
            entity.setName(event.name)
            entity.setDescription(event.description)
            return entity
        }
        _ * eventStructMapper.entityToModel(_ as EventEntity) >> { EventEntity entity ->
            if (entity == null) {
                return null
            }
            return Event.builder()
                    .id(entity.id)
                    .name(entity.name != null ? entity.name : "")
                    .description(entity.description != null ? entity.description : "")
                    .build()
        }
    }

    @Unroll
    def "test create event - id: #eventId, name: #name, description: #description"() {
        given: "event data"
        eventMapper.selectById(eventId) >> null

        when: "create event"
        def result = eventService.createEvent(eventId, name, description)

        then: "event should be created"
        1 * eventMapper.selectById(eventId) >> null
        1 * eventMapper.insert(_ as EventEntity) >> { EventEntity entity ->
            entity.setId(eventId)
            return 1
        }
        result != null
        result.id == eventId
        result.name == name.trim()
        result.description == (description != null ? description.trim() : "")

        where:
        eventId  | name          | description
        10000001 | "Event 1"     | "Description 1"
        10000002 | "Event 2"     | null
        10000003 | "  Event 3  " | "  Description 3  "
    }

    // Note: Validation for null id is now done at controller layer using @Validated,
    // so this test case is no longer applicable at service layer.
    // The service layer will handle null id as a business logic error if it reaches here.

    @Unroll
    def "test create event - duplicate id - id: #id, name: #name"() {
        given: "event with same id already exists"
        def existingEventEntity = new EventEntity()
        existingEventEntity.setId(id)
        existingEventEntity.setName("Existing Event")
        eventMapper.selectById(id) >> existingEventEntity

        when: "create event with duplicate id"
        eventService.createEvent(id, name, "Description")

        then: "should throw IllegalArgumentException"
        1 * eventMapper.selectById(id) >> existingEventEntity
        IllegalArgumentException e = thrown()
        e.message.contains("already exists")

        where:
        id       | name
        10000001 | "New Event"
    }

    @Unroll
    def "test get event by ID - eventId: #eventId, exists: #exists"() {
        given: "event data"
        if (exists) {
            def eventEntity = new EventEntity()
            eventEntity.setId(eventId)
            eventEntity.setName("Test Event")
            eventMapper.selectById(eventId) >> eventEntity
        } else {
            eventMapper.selectById(eventId) >> null
        }

        when: "get event"
        def result = eventService.getEventById(eventId)

        then: "should return correct event"
        if (exists) {
            result != null
            result.id == eventId
        } else {
            result == null
        }

        where:
        eventId   | exists
        10000001L | true
        10000002L | true
        99999999L | false
    }

    @Unroll
    def "test update event - eventId: #eventId, name: #name, description: #description"() {
        given: "existing event"
        def existingEventEntity = new EventEntity()
        existingEventEntity.setId(eventId)
        existingEventEntity.setName("Old Name")
        existingEventEntity.setDescription("Old Description")
        eventMapper.selectById(eventId) >> existingEventEntity
        eventMapper.updateById(_ as EventEntity) >> 1

        when: "update event"
        def result = eventService.updateEvent(eventId, name, description)

        then: "event should be updated"
        1 * eventMapper.selectById(eventId) >> existingEventEntity
        1 * eventMapper.updateById(_ as EventEntity) >> 1
        result != null
        result.id == eventId
        if (name != null && !name.trim().isEmpty()) {
            result.name == name.trim()
        } else {
            // If name is null or empty, it should remain unchanged
            result.name == "Old Name"
        }
        if (description != null) {
            result.description == description.trim()
        } else {
            // If description is null, it should remain unchanged
            result.description == "Old Description"
        }

        where:
        eventId   | name         | description
        10000001L | "New Name 1" | "New Description 1"
        10000002L | "New Name 2" | null
        10000003L | null         | "New Description 3"
        10000004L | null         | null
        10000005L | ""           | "New Description 5"
    }

    @Unroll
    def "test update event - event not found - eventId: #eventId"() {
        given: "event does not exist"
        eventMapper.selectById(eventId) >> null

        when: "update non-existent event"
        eventService.updateEvent(eventId, "New Name", "New Description")

        then: "should throw IllegalArgumentException"
        1 * eventMapper.selectById(eventId) >> null
        IllegalArgumentException e = thrown()
        e.message.contains("Event not found")

        where:
        eventId << [99999999L, 88888888L]
    }

    @Unroll
    def "test getAllEvents - eventCount: #eventCount"() {
        given: "prepare event entities"
        def eventEntities = []
        eventCount.times { i ->
            def entity = new EventEntity()
            entity.setId(10000001L + i)
            entity.setName("Event ${i + 1}")
            entity.setDescription("Description ${i + 1}")
            eventEntities.add(entity)
        }

        when: "get all events"
        def result = eventService.getAllEvents()

        then: "should return all events"
        1 * eventMapper.selectList(null) >> eventEntities
        result != null
        result.size() == eventCount

        where:
        eventCount << [0, 1, 3, 5]
    }

    @Unroll
    def "test getAllEvents - empty list - eventCount: #eventCount"() {
        when: "get all events when no events exist"
        def result = eventService.getAllEvents()

        then: "should return empty list"
        1 * eventMapper.selectList(null) >> null
        result != null
        result.isEmpty()

        where:
        eventCount << [0]
    }

    @Unroll
    def "test delete event - success - eventId: #eventId"() {
        given: "event exists"
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity
        executionArrangementMapper.delete(_ as LambdaQueryWrapper) >> 1
        eventMapper.deleteById(eventId) >> 1

        when: "delete event"
        eventService.deleteEvent(eventId)

        then: "event should be deleted"
        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * executionArrangementMapper.delete(_ as LambdaQueryWrapper) >> 1
        1 * eventMapper.deleteById(eventId) >> 1

        where:
        eventId << [10000001L, 10000002L]
    }

    @Unroll
    def "test delete event - not found - eventId: #eventId"() {
        given: "event does not exist"
        eventMapper.selectById(eventId) >> null

        when: "delete event"
        eventService.deleteEvent(eventId)

        then: "should throw IllegalArgumentException"
        1 * eventMapper.selectById(eventId) >> null
        IllegalArgumentException e = thrown()
        e.message.contains("Event not found")

        where:
        eventId << [99999999L, 88888888L]
    }

    @Unroll
    def "test batch set execution items - eventId: #eventId, hasExisting: #hasExisting, newItemCount: #newItemCount"() {
        given: "event and execution items"
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Mock existing relations
        def existingRelations = []
        if (hasExisting) {
            def existing1 = new ExecutionArrangementEntity()
            existing1.setEventId(eventId != null ? eventId.intValue() : null)
            existing1.setRuleId(999L) // This will be deleted if not in new list
            existing1.setGroupId(0L)
            existing1.setExeOrder(0)
            existing1.setAbRatio(0)
            existingRelations.add(existing1)
        }
        executionArrangementMapper.selectList(_ as LambdaQueryWrapper) >> existingRelations

        // Mock batch validation - rules
        def ruleEntities = []
        if (newItemCount >= 1) {
            def rule1 = new RuleEntity()
            rule1.setId(1L)
            ruleEntities.add(rule1)
        }
        if (newItemCount >= 2) {
            def rule3 = new RuleEntity()
            rule3.setId(10000001L)
            ruleEntities.add(rule3)
        }
        if (newItemCount >= 3) {
            def rule2 = new RuleEntity()
            rule2.setId(2L)
            ruleEntities.add(rule2)
        }
        ruleMapper.selectBatchIds(_) >> ruleEntities

        // Mock insert, update, and delete
        executionArrangementMapper.insert(_ as ExecutionArrangementEntity) >> 1
        executionArrangementMapper.updateById(_ as ExecutionArrangementEntity) >> 1
        executionArrangementMapper.delete(_ as LambdaQueryWrapper) >> 1

        // Build execution items DTO
        def executionItems = []
        if (newItemCount >= 1) {
            executionItems.add(1L)
        }
        if (newItemCount >= 2) {
            executionItems.add(10000001L)
        }
        if (newItemCount >= 3) {
            executionItems.add(2L)
        }

        when: "batch set execution items"
        Integer eventIdInt = eventId != null ? eventId.intValue() : null
        eventService.setExecutionItems(eventIdInt, executionItems)

        then: "execution items should be set correctly"
        1 * eventMapper.selectById(eventId) >> eventEntity

        // Batch validation calls
        if (newItemCount > 0) {
            // Count rules
            def ruleCount = newItemCount >= 1 ? 1 : 0
            ruleCount += newItemCount >= 2 ? 1 : 0
            ruleCount += newItemCount >= 3 ? 1 : 0

            if (ruleCount > 0) {
                1 * ruleMapper.selectBatchIds(_) >> ruleEntities
            }
        }

        1 * executionArrangementMapper.selectList(_ as LambdaQueryWrapper) >> existingRelations

        // All new items will be inserted (since they don't exist in existingRelations)
        if (newItemCount > 0) {
            newItemCount * executionArrangementMapper.insert(_ as ExecutionArrangementEntity) >> 1
        }

        // If there are existing relations not in new list, delete will be called
        if (hasExisting && newItemCount == 0) {
            // All existing relations will be deleted
            1 * executionArrangementMapper.delete(_ as LambdaQueryWrapper) >> 1
        } else if (hasExisting && newItemCount > 0) {
            // The existing relation (999L) is not in new list, so it will be deleted
            1 * executionArrangementMapper.delete(_ as LambdaQueryWrapper) >> 1
        }

        where:
        eventId   | hasExisting | newItemCount
        10000001L | false       | 0
        10000001L | false       | 1
        10000001L | false       | 2
        10000001L | true        | 0
        10000001L | true        | 1
        10000001L | true        | 2
    }

    @Unroll
    def "test batchSetExecutionItems - batch validation - existingRuleIds: #existingRuleIds, nonExistentRuleIds: #nonExistentRuleIds"() {
        given: "event exists"
        def eventId = 10000001L
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Build execution items
        def executionItems = []
        existingRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }

        // Mock existing rules for batch query
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        ruleMapper.selectBatchIds(_) >> existingRuleEntities

        // Mock existing relations (empty)
        executionArrangementMapper.selectList(_) >> []

        when: "batch set execution items"
        Integer eventIdInt = eventId != null ? eventId.intValue() : null
        eventService.setExecutionItems(eventIdInt, executionItems)

        then: "should succeed"
        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleMapper.selectBatchIds(_) >> existingRuleEntities
        1 * executionArrangementMapper.selectList(_) >> []
        executionItems.size() * executionArrangementMapper.insert(_) >> 1

        where:
        existingRuleIds | nonExistentRuleIds
        [1L, 2L, 3L]    | []
    }

    @Unroll
    def "test batchSetExecutionItems - batch validation failure - existingRuleIds: #existingRuleIds, nonExistentRuleIds: #nonExistentRuleIds"() {
        given: "event exists"
        def eventId = 10000001L
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Build execution items
        def executionItems = []
        existingRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }

        // Mock existing rules for batch query
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        ruleMapper.selectBatchIds(_) >> existingRuleEntities

        when: "batch set execution items"
        def exception = null
        try {
            Integer eventIdInt = eventId != null ? eventId.intValue() : null
            eventService.setExecutionItems(eventIdInt, executionItems)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with non-existent rule IDs"
        exception != null
        exception.message.contains("Rules not found in database")
        nonExistentRuleIds.each { ruleId ->
            assert exception.message.contains(ruleId.toString())
        }

        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleMapper.selectBatchIds(_)

        where:
        existingRuleIds | nonExistentRuleIds
        [1L, 2L]        | [999L]
        [1L]            | [999L, 1000L]
        []              | [999L, 1000L]
    }

    @Unroll
    def "test batchSetExecutionItems - batch validation with rule groups - existingGroupIds: #existingGroupIds, nonExistentGroupIds: #nonExistentGroupIds"() {
        given: "event exists"
        def eventId = 10000001L
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Build execution items with rule groups (these will be treated as rule IDs)
        def executionItems = []
        existingGroupIds.each { groupId ->
            executionItems.add(groupId)
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(groupId)
        }

        // Mock rule entities for group IDs (treating them as rule IDs)
        def ruleEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleEntity()
            entity.setId(groupId)
            return entity
        }
        ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing group IDs that are treated as rule IDs
            return ruleEntities.findAll { it.id in ids }
        }

        // Mock existing relations (empty)
        executionArrangementMapper.selectList(_) >> []

        when: "batch set execution items"
        Integer eventIdInt = eventId != null ? eventId.intValue() : null
        eventService.setExecutionItems(eventIdInt, executionItems)

        then: "should succeed if group IDs are treated as valid rule IDs"
        1 * eventMapper.selectById(eventId) >> eventEntity
        if (!executionItems.isEmpty()) {
            1 * ruleMapper.selectBatchIds(_) >> ruleEntities
        }
        1 * executionArrangementMapper.selectList(_) >> []
        executionItems.size() * executionArrangementMapper.insert(_) >> 1

        where:
        existingGroupIds | nonExistentGroupIds
        [10000001L]      | []
    }

    @Unroll
    def "test batchSetExecutionItems - batch validation failure with rule groups - existingGroupIds: #existingGroupIds, nonExistentGroupIds: #nonExistentGroupIds"() {
        given: "event exists"
        def eventId = 10000001L
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Build execution items with rule groups
        def executionItems = []
        existingGroupIds.each { groupId ->
            executionItems.add(groupId)
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(groupId)
        }

        when: "batch set execution items"
        def exception = null
        try {
            Integer eventIdInt = eventId != null ? eventId.intValue() : null
            eventService.setExecutionItems(eventIdInt, executionItems)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with non-existent rule IDs"
        exception != null
        exception.message.contains("Rules not found in database")
        nonExistentGroupIds.each { groupId ->
            assert exception.message.contains(groupId.toString())
        }

        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return empty list since these are group IDs, not rule IDs
            return []
        }

        where:
        existingGroupIds | nonExistentGroupIds
        [10000001L]      | [999999L]
        []               | [999999L, 999998L]
    }

    @Unroll
    def "test batchSetExecutionItems - batch validation with mixed types - existingRuleIds: #existingRuleIds, existingGroupIds: #existingGroupIds, nonExistentRuleIds: #nonExistentRuleIds, nonExistentGroupIds: #nonExistentGroupIds"() {
        given: "event exists"
        def eventId = 10000001L
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Build execution items with mixed types
        def executionItems = []
        existingRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }
        existingGroupIds.each { groupId ->
            executionItems.add(groupId)
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(groupId)
        }

        // Mock existing rules
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        
        // Mock rule entities for group IDs (treating them as rule IDs)
        def groupRuleEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleEntity()
            entity.setId(groupId)
            return entity
        }
        
        def allRuleEntities = existingRuleEntities + groupRuleEntities
        
        ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing rules and group IDs (treated as rule IDs)
            return allRuleEntities.findAll { it.id in ids }
        }

        // Mock existing relations (empty)
        executionArrangementMapper.selectList(_) >> []

        when: "batch set execution items"
        Integer eventIdInt = eventId != null ? eventId.intValue() : null
        eventService.setExecutionItems(eventIdInt, executionItems)

        then: "should succeed"
        1 * eventMapper.selectById(eventId) >> eventEntity
        if (!executionItems.isEmpty()) {
            1 * ruleMapper.selectBatchIds(_) >> allRuleEntities
        }
        1 * executionArrangementMapper.selectList(_) >> []
        executionItems.size() * executionArrangementMapper.insert(_) >> 1

        where:
        existingRuleIds | existingGroupIds | nonExistentRuleIds | nonExistentGroupIds
        [1L, 2L]        | [10000001L]      | []                 | []
    }

    @Unroll
    def "test batchSetExecutionItems - batch validation failure with mixed types - existingRuleIds: #existingRuleIds, existingGroupIds: #existingGroupIds, nonExistentRuleIds: #nonExistentRuleIds, nonExistentGroupIds: #nonExistentGroupIds"() {
        given: "event exists"
        def eventId = 10000001L
        def eventEntity = new EventEntity()
        eventEntity.setId(eventId)
        eventMapper.selectById(eventId) >> eventEntity

        // Build execution items with mixed types
        def executionItems = []
        existingRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }
        existingGroupIds.each { groupId ->
            executionItems.add(groupId)
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ruleId)
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(groupId)
        }

        // Mock existing rules
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        
        // Mock rule entities for group IDs (treating them as rule IDs)
        def groupRuleEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleEntity()
            entity.setId(groupId)
            return entity
        }
        
        def allRuleEntities = existingRuleEntities + groupRuleEntities
        
        ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing rules and group IDs (treated as rule IDs)
            return allRuleEntities.findAll { it.id in ids }
        }

        when: "batch set execution items"
        def exception = null
        try {
            Integer eventIdInt = eventId != null ? eventId.intValue() : null
            eventService.setExecutionItems(eventIdInt, executionItems)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with all non-existent IDs"
        exception != null
        exception.message.contains("Rules not found in database")
        if (!nonExistentRuleIds.isEmpty()) {
            nonExistentRuleIds.each { ruleId ->
                assert exception.message.contains(ruleId.toString())
            }
        }
        if (!nonExistentGroupIds.isEmpty()) {
            // Group IDs will also be treated as rule IDs and fail validation
            nonExistentGroupIds.each { groupId ->
                assert exception.message.contains(groupId.toString())
            }
        }

        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing rules and group IDs (treated as rule IDs)
            return allRuleEntities.findAll { it.id in ids }
        }

        where:
        existingRuleIds | existingGroupIds | nonExistentRuleIds | nonExistentGroupIds
        [1L]            | []               | [999L]             | []
        []              | [10000001L]      | []                 | [999999L]
        [1L, 2L]        | [10000001L]      | [999L]             | [999999L]
    }

    def "test getExecutionItems - should return relations ordered by execution order"() {
        given: "event with execution relations"
        def eventId = 10000001L
        def eventIdInt = eventId.intValue()
        // Create relations in unsorted order
        def relation1 = createRelation(eventIdInt, ExecutionArrangementTypeEnum.RULE, 1L, 2)
        def relation2 = createRelation(eventIdInt, ExecutionArrangementTypeEnum.RULE, 2L, 1)
        def relation3 = createRelation(eventIdInt, ExecutionArrangementTypeEnum.RULE_GROUP, 10000001L, 3)
        // MyBatis Plus will return them sorted by executionOrder ASC
        def relations = [relation2, relation1, relation3] // Sorted: 1, 2, 3

        // Mock rule status config
        def allowedStatuses = [RuleStatusEnum.ONLINE, RuleStatusEnum.GRAY] as Set
        ruleStatusConfig.getEvalAvailableRuleStatuses() >> allowedStatuses

        // Mock struct mapper - convert entity to model
        executionArrangementStructMapper.entityToModel(_ as ExecutionArrangementEntity) >> { ExecutionArrangementEntity e ->
            def model = new lab.zhang.rule.rule_engine.model.ExecutionArrangement()
            model.setEventId(e.eventId)
            model.setRuleId(e.ruleId)
            model.setGroupId(e.groupId)
            model.setExeOrder(e.exeOrder)
            model.setAbRatio(e.abRatio)
            return model
        }

        when: "get execution event relations"
        def result = eventService.getExecutionItems(eventId.intValue())

        then: "should return relations ordered by execution order"
        1 * ruleStatusConfig.getEvalAvailableRuleStatuses() >> allowedStatuses
        1 * executionArrangementMapper.selectByEventIdOnRuleStatus(eventIdInt, _) >> relations
        result != null
        result.size() == 3
        result[0].exeOrder == 1
        result[1].exeOrder == 2
        result[2].exeOrder == 3
    }

    def "test getExecutionItems - should return empty list when no relations exist"() {
        given: "event with no relations"
        def eventId = 10000001L
        def eventIdInt = eventId.intValue()
        
        // Mock rule status config
        def allowedStatuses = [RuleStatusEnum.ONLINE, RuleStatusEnum.GRAY] as Set
        ruleStatusConfig.getEvalAvailableRuleStatuses() >> allowedStatuses

        when: "get execution event relations"
        def result = eventService.getExecutionItems(eventIdInt)

        then: "should return empty list"
        1 * ruleStatusConfig.getEvalAvailableRuleStatuses() >> allowedStatuses
        1 * executionArrangementMapper.selectByEventIdOnRuleStatus(eventIdInt, _) >> []
        result != null
        result.isEmpty()
    }

    // Helper method
    private static ExecutionArrangementEntity createRelation(Integer eventId, ExecutionArrangementTypeEnum itemType, Long ruleId, Integer order) {
        def relation = new ExecutionArrangementEntity()
        relation.setEventId(eventId)
        relation.setRuleId(ruleId)
        relation.setGroupId(itemType == ExecutionArrangementTypeEnum.RULE_GROUP ? 1 : 0)
        relation.setExeOrder(order)
        return relation
    }
}

