package lab.zhang.rule.rule_engine.service.impl

import com.baomidou.mybatisplus.core.conditions.Wrapper
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import lab.zhang.rule.rule_engine.entity.EventEntity
import lab.zhang.rule.rule_engine.entity.ExecutionEventRelationEntity
import lab.zhang.rule.rule_engine.entity.RuleEntity
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum
import lab.zhang.rule.rule_engine.mapper.EventMapper
import lab.zhang.rule.rule_engine.mapper.ExecutionEventRelationMapper
import lab.zhang.rule.rule_engine.mapper.RuleGroupMapper
import lab.zhang.rule.rule_engine.mapper.RuleMapper
import lab.zhang.rule.rule_engine.model.Event
import lab.zhang.rule.rule_engine.pojo.qo.ExecutionItemQO
import lab.zhang.rule.rule_engine.struct_mapper.EventStructMapper
import spock.lang.Specification
import spock.lang.Unroll

/**
 * EventService unit test
 */
class EventServiceImplSpec extends Specification {

    def eventMapper = Mock(EventMapper)
    def executionEventRelationMapper = Mock(ExecutionEventRelationMapper)
    def ruleMapper = Mock(RuleMapper)
    def ruleGroupMapper = Mock(RuleGroupMapper)
    def eventStructMapper = Mock(EventStructMapper)
    def eventService = new EventServiceImpl()

    def setup() {
        eventService.eventMapper = eventMapper
        eventService.executionEventRelationMapper = executionEventRelationMapper
        eventService.ruleMapper = ruleMapper
        eventService.ruleGroupMapper = ruleGroupMapper
        eventService.eventStructMapper = eventStructMapper

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
        executionEventRelationMapper.delete(_ as LambdaQueryWrapper) >> 1
        eventMapper.deleteById(eventId) >> 1

        when: "delete event"
        eventService.deleteEvent(eventId)

        then: "event should be deleted"
        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * executionEventRelationMapper.delete(_ as LambdaQueryWrapper) >> 1
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
            def existing1 = new ExecutionEventRelationEntity()
            existing1.setEventId(eventId)
            existing1.setItemType(ExecutionItemTypeEnum.RULE.getId())
            existing1.setItemId(999L) // This will be deleted if not in new list
            existingRelations.add(existing1)
        }
        executionEventRelationMapper.selectList(_ as LambdaQueryWrapper) >> existingRelations

        // Mock batch validation - rules
        def ruleEntities = []
        if (newItemCount >= 1) {
            def rule1 = new RuleEntity()
            rule1.setId(1L)
            ruleEntities.add(rule1)
        }
        if (newItemCount >= 3) {
            def rule2 = new RuleEntity()
            rule2.setId(2L)
            ruleEntities.add(rule2)
        }
        ruleMapper.selectBatchIds(_) >> ruleEntities

        // Mock batch validation - rule groups
        def groupEntities = []
        if (newItemCount >= 2) {
            def group1 = new RuleGroupEntity()
            group1.setId(10000001L)
            groupEntities.add(group1)
        }
        ruleGroupMapper.selectBatchIds(_) >> groupEntities

        // Mock insert, update, and delete
        executionEventRelationMapper.insert(_ as ExecutionEventRelationEntity) >> 1
        executionEventRelationMapper.updateById(_ as ExecutionEventRelationEntity) >> 1
        executionEventRelationMapper.delete(_ as LambdaQueryWrapper) >> 1

        // Build execution items DTO
        def executionItems = []
        if (newItemCount >= 1) {
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(1L)
                    .build())
        }
        if (newItemCount >= 2) {
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(10000001L)
                    .build())
        }
        if (newItemCount >= 3) {
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(2L)
                    .build())
        }

        when: "batch set execution items"
        eventService.batchSetExecutionItems(eventId, executionItems)

        then: "execution items should be set correctly"
        1 * eventMapper.selectById(eventId) >> eventEntity

        // Batch validation calls
        if (newItemCount > 0) {
            // Count rules and groups separately
            def ruleCount = newItemCount >= 1 ? 1 : 0
            ruleCount += newItemCount >= 3 ? 1 : 0
            def groupCount = newItemCount >= 2 ? 1 : 0

            if (ruleCount > 0) {
                1 * ruleMapper.selectBatchIds(_) >> ruleEntities
            }
            if (groupCount > 0) {
                1 * ruleGroupMapper.selectBatchIds(_) >> groupEntities
            }
        }

        1 * executionEventRelationMapper.selectList(_ as LambdaQueryWrapper) >> existingRelations

        // All new items will be inserted (since they don't exist in existingRelations)
        if (newItemCount > 0) {
            newItemCount * executionEventRelationMapper.insert(_ as ExecutionEventRelationEntity) >> 1
        }

        // If there are existing relations not in new list, delete will be called
        if (hasExisting && newItemCount == 0) {
            // All existing relations will be deleted
            1 * executionEventRelationMapper.delete(_ as LambdaQueryWrapper) >> 1
        } else if (hasExisting && newItemCount > 0) {
            // The existing relation (999L) is not in new list, so it will be deleted
            1 * executionEventRelationMapper.delete(_ as LambdaQueryWrapper) >> 1
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
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }

        // Mock existing rules for batch query
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        ruleMapper.selectBatchIds(_) >> existingRuleEntities

        // Mock existing relations (empty)
        executionEventRelationMapper.selectList(_) >> []

        when: "batch set execution items"
        eventService.batchSetExecutionItems(eventId, executionItems)

        then: "should succeed"
        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleMapper.selectBatchIds(_) >> existingRuleEntities
        1 * executionEventRelationMapper.selectList(_) >> []
        executionItems.size() * executionEventRelationMapper.insert(_) >> 1

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
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
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
            eventService.batchSetExecutionItems(eventId, executionItems)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with non-existent rule IDs"
        exception != null
        exception.message.contains("Execution items not found in database")
        exception.message.contains("Rule IDs")
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

        // Build execution items with rule groups
        def executionItems = []
        existingGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }

        // Mock existing rule groups for batch query
        def existingGroupEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleGroupEntity()
            entity.setId(groupId)
            return entity
        }
        ruleGroupMapper.selectBatchIds(_) >> existingGroupEntities

        // Mock existing relations (empty)
        executionEventRelationMapper.selectList(_) >> []

        when: "batch set execution items"
        eventService.batchSetExecutionItems(eventId, executionItems)

        then: "should succeed"
        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleGroupMapper.selectBatchIds(_) >> existingGroupEntities
        1 * executionEventRelationMapper.selectList(_) >> []
        executionItems.size() * executionEventRelationMapper.insert(_) >> 1

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
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }

        // Mock existing rule groups for batch query
        def existingGroupEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleGroupEntity()
            entity.setId(groupId)
            return entity
        }
        ruleGroupMapper.selectBatchIds(_) >> existingGroupEntities

        when: "batch set execution items"
        def exception = null
        try {
            eventService.batchSetExecutionItems(eventId, executionItems)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with non-existent group IDs"
        exception != null
        exception.message.contains("Execution items not found in database")
        exception.message.contains("Rule group IDs")
        nonExistentGroupIds.each { groupId ->
            assert exception.message.contains(groupId.toString())
        }

        1 * eventMapper.selectById(eventId) >> eventEntity
        1 * ruleGroupMapper.selectBatchIds(_)

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
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }
        existingGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }

        // Mock existing rules
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing rules
            return existingRuleEntities.findAll { it.id in ids }
        }

        // Mock existing rule groups
        def existingGroupEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleGroupEntity()
            entity.setId(groupId)
            return entity
        }
        ruleGroupMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing groups
            return existingGroupEntities.findAll { it.id in ids }
        }

        // Mock existing relations (empty)
        executionEventRelationMapper.selectList(_) >> []

        when: "batch set execution items"
        eventService.batchSetExecutionItems(eventId, executionItems)

        then: "should succeed"
        1 * eventMapper.selectById(eventId) >> eventEntity
        if (!existingRuleIds.isEmpty()) {
            1 * ruleMapper.selectBatchIds(_) >> existingRuleEntities
        }
        if (!existingGroupIds.isEmpty()) {
            1 * ruleGroupMapper.selectBatchIds(_) >> existingGroupEntities
        }
        1 * executionEventRelationMapper.selectList(_) >> []
        executionItems.size() * executionEventRelationMapper.insert(_) >> 1

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
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }
        existingGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }
        nonExistentRuleIds.each { ruleId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE.getId())
                    .itemId(ruleId)
                    .build())
        }
        nonExistentGroupIds.each { groupId ->
            executionItems.add(ExecutionItemQO.builder()
                    .itemType(ExecutionItemTypeEnum.RULE_GROUP.getId())
                    .itemId(groupId)
                    .build())
        }

        // Mock existing rules
        def existingRuleEntities = existingRuleIds.collect { ruleId ->
            def entity = new RuleEntity()
            entity.setId(ruleId)
            return entity
        }
        ruleMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing rules
            return existingRuleEntities.findAll { it.id in ids }
        }

        // Mock existing rule groups
        def existingGroupEntities = existingGroupIds.collect { groupId ->
            def entity = new RuleGroupEntity()
            entity.setId(groupId)
            return entity
        }
        ruleGroupMapper.selectBatchIds(_) >> { List<Long> ids ->
            // Return only existing groups
            return existingGroupEntities.findAll { it.id in ids }
        }

        when: "batch set execution items"
        def exception = null
        try {
            eventService.batchSetExecutionItems(eventId, executionItems)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with all non-existent IDs"
        exception != null
        exception.message.contains("Execution items not found in database")
        if (!nonExistentRuleIds.isEmpty()) {
            exception.message.contains("Rule IDs")
            nonExistentRuleIds.each { ruleId ->
                assert exception.message.contains(ruleId.toString())
            }
        }
        if (!nonExistentGroupIds.isEmpty()) {
            exception.message.contains("Rule group IDs")
            nonExistentGroupIds.each { groupId ->
                assert exception.message.contains(groupId.toString())
            }
        }

        1 * eventMapper.selectById(eventId) >> eventEntity

        where:
        existingRuleIds | existingGroupIds | nonExistentRuleIds | nonExistentGroupIds
        [1L]            | []               | [999L]             | []
        []              | [10000001L]      | []                 | [999999L]
        [1L, 2L]        | [10000001L]      | [999L]             | [999999L]
    }

    def "test getExecutionEventRelations - should return relations ordered by execution order"() {
        given: "event with execution relations"
        def eventId = 10000001L
        // Create relations in unsorted order
        def relation1 = createRelation(eventId, ExecutionItemTypeEnum.RULE, 1L, 2)
        def relation2 = createRelation(eventId, ExecutionItemTypeEnum.RULE, 2L, 1)
        def relation3 = createRelation(eventId, ExecutionItemTypeEnum.RULE_GROUP, 10000001L, 3)
        // MyBatis Plus will return them sorted by executionOrder ASC
        def relations = [relation2, relation1, relation3] // Sorted: 1, 2, 3

        when: "get execution event relations"
        def result = eventService.getExecutionEventRelations(eventId)

        then: "should return relations ordered by execution order"
        1 * executionEventRelationMapper.selectList(_ as LambdaQueryWrapper) >> relations
        result != null
        result.size() == 3
        result[0].executionOrder == 1
        result[1].executionOrder == 2
        result[2].executionOrder == 3
    }

    def "test getExecutionEventRelations - should return empty list when no relations exist"() {
        given: "event with no relations"
        def eventId = 10000001L
        executionEventRelationMapper.selectList(_ as Wrapper<ExecutionEventRelationEntity>) >> null

        when: "get execution event relations"
        def result = eventService.getExecutionEventRelations(eventId)

        then: "should return empty list"
        1 * executionEventRelationMapper.selectList(_)
        result != null
        result.isEmpty()
    }

    // Helper method
    private static ExecutionEventRelationEntity createRelation(Long eventId, ExecutionItemTypeEnum itemType, Long itemId, Integer order) {
        def relation = new ExecutionEventRelationEntity()
        relation.setEventId(eventId)
        relation.setItemTypeEnum(itemType)
        relation.setItemId(itemId)
        relation.setExecutionOrder(order)
        return relation
    }
}

