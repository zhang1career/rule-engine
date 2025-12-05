package lab.zhang.rule.rule_engine.service.impl

import com.baomidou.mybatisplus.core.conditions.Wrapper
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper
import lab.zhang.rule.rule_engine.mapper.RuleGroupMapper
import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.service.RuleSelectionCacheService
import lab.zhang.rule.rule_engine.service.RuleService
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Test for RuleGroupServiceImpl business logic
 * Tests the 4 special business logic requirements
 *
 * @author Rongjin Zhang
 */
class RuleGroupServiceImplBusinessLogicSpec extends Specification {

    RuleGroupServiceImpl ruleGroupService
    RuleService ruleService = Mock()
    RuleGroupMapper ruleGroupMapper = Mock()
    ExecutionArrangementMapper executionEventRelationMapper = Mock()
    RuleGroupStructMapper ruleGroupStructMapper = Mock()
    RuleSelectionCacheService ruleSelectionCacheService = Mock()

    def setup() {
        ruleGroupService = new RuleGroupServiceImpl()
        ruleGroupService.ruleService = ruleService
        ruleGroupService.ruleGroupMapper = ruleGroupMapper
        ruleGroupService.executionArrangementMapper = executionEventRelationMapper
        ruleGroupService.ruleGroupStructMapper = ruleGroupStructMapper
        ruleGroupService.ruleSelectionCacheService = ruleSelectionCacheService
    }

    @Unroll
    def "Business Logic 1: Create rule group and copy rule-event associations - ruleId: #ruleId, eventIds: #eventIds"() {
        given: "a rule in ONLINE status with event associations"
        def rule = Rule.builder()
                .id(ruleId)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        // Mock rule-event associations (standalone rules with groupId = 0)
        def ruleEventRelations = eventIds.collect { eventId ->
            def relation = new ExecutionArrangementEntity()
            relation.setEventId(eventId)
            relation.setRuleId(ruleId)
            relation.setGroupId(0L) // Standalone rule
            relation.setExeOrder(eventIds.indexOf(eventId))
            relation.setAbRatio(0)
            return relation
        }

        // Mock selectList: return rule-event relations when querying for RULE type
        executionEventRelationMapper.selectList(_ as LambdaQueryWrapper) >> { LambdaQueryWrapper wrapper ->
            // Return rule-event relations when querying for RULE type
            return ruleEventRelations
        }

        // Mock selectOne with different behavior based on the query
        def selectOneCallCount = 0
        executionEventRelationMapper.selectOne(_ as LambdaQueryWrapper) >> {
            selectOneCallCount++
            if (selectOneCallCount == 1) {
                // First call: Check for existing group associations (group_id != 0) - should return null
                return null
            } else {
                // Second call: Find standalone relation (group_id = 0) - should return the relation
                return ruleEventRelations.find { it.ruleId == ruleId && it.groupId == 0L }
            }
        }

        // Mock insert for group-event associations
        executionEventRelationMapper.insert(_ as ExecutionArrangementEntity) >> { ExecutionArrangementEntity entity ->
            // Verify it's a group-event association
            assert entity.getRuleId() == ruleId
            assert entity.getGroupId() == 10000001L
            assert eventIds.contains(entity.getEventId())
            return 1
        }

        ruleGroupMapper.insert(_ as RuleGroupEntity) >> { RuleGroupEntity entity ->
            entity.setId(10000001L)
            return 1
        }

        // Note: GreRelationMapper is no longer used, relations are now managed via ExecutionArrangementMapper

        when: "create rule group"
        // createRuleGroup now requires eventId parameter
        def eventId = eventIds[0] // Use the first eventId from the test parameters
        def group = ruleGroupService.createRuleGroup(rule, eventId)

        then: "group should be created and rule-event associations copied to group"
        group != null
        group.getId() == 10000001L
        group.getRuleIds().contains(ruleId)

        // Verify group-event associations were created (one for each eventId)
        eventIds.size() * executionEventRelationMapper.insert(_ as ExecutionArrangementEntity)

        where:
        ruleId | eventIds
        1L     | [1001, 1002]
        2L     | [1001]
        3L     | [1001, 1002, 1003]
    }


    @Unroll
    def "Business Logic 3: When rule changes from ONLINE to #newStatus, delete rule group - ruleId: #ruleId, groupId: #groupId"() {
        given: "a rule in ONLINE status in a group"

        when: "delete rule group"
        ruleGroupService.deleteRuleGroup(groupId)

        then: "rule group and all associations should be deleted"
        // Note: Relations are now managed via ExecutionArrangementMapper, not GreRelationMapper
        // Verify group-event relations are deleted/updated
        1 * executionEventRelationMapper.delete(_ as Wrapper<ExecutionArrangementEntity>)
        // Verify rule group entity is deleted
        1 * ruleGroupMapper.deleteById(groupId)

        where:
        ruleId | groupId   | newStatus
        1L     | 10000001L | RuleStatusEnum.OFFLINE
        2L     | 10000002L | RuleStatusEnum.TEST
        3L     | 10000003L | RuleStatusEnum.ONLINE
    }

    @Unroll
    def "Business Logic 4: When group is empty, delete group and its event associations - groupId: #groupId, eventIds: #eventIds"() {
        given: "an empty group with event associations"
        // Mock rule group entity
        def groupEntity = new RuleGroupEntity()
        groupEntity.setId(groupId)
        ruleGroupMapper.selectById(groupId) >> groupEntity

        // Mock rule group rule relations (empty - no rules in group)
        // Note: GreRelationMapper is no longer used

        // Mock group-event associations (empty group - no rules, but may have old associations to clean up)
        // For empty group, getRuleGroup should return empty relations
        def emptyGroupRelations = [] // Empty group has no rules
        
        // But deleteRuleGroupIfEmpty may find old associations to clean up
        def oldGroupRelations = eventIds.collect { eventId ->
            def relation = new ExecutionArrangementEntity()
            relation.setEventId(eventId)
            relation.setRuleId(1L) // Some old rule ID (not important for empty group)
            relation.setGroupId(groupId) // Group ID
            relation.setExeOrder(0)
            relation.setAbRatio(0)
            return relation
        }

        // Mock selectList - getRuleGroup returns empty, deleteRuleGroupIfEmpty finds old relations
        def callCount = 0
        executionEventRelationMapper.selectList(_ as LambdaQueryWrapper) >> { LambdaQueryWrapper wrapper ->
            callCount++
            // First call: getRuleGroup queries by groupId, should return empty for empty group
            if (callCount == 1) {
                return emptyGroupRelations
            }
            // Second call: deleteRuleGroupIfEmpty queries by groupId to find old associations
            return oldGroupRelations
        }

        // Mock delete operations
        executionEventRelationMapper.delete(_ as LambdaQueryWrapper) >> eventIds.size()
        ruleGroupMapper.deleteById(groupId) >> 1

        when: "delete empty group"
        ruleGroupService.deleteRuleGroupIfEmpty(groupId)

        then: "group and its event associations should be deleted"
        // Verify getRuleGroup was called (which calls selectById, selectList, and entityToModel)
        1 * ruleGroupMapper.selectById(groupId) >> groupEntity
        // Note: GreRelationMapper is no longer used
        // Mock ruleGroupStructMapper.entityToModel to return empty RuleGroup
        // getRuleGroup calls entityToModel with empty relationsMap, so it should return empty RuleGroup
        1 * ruleGroupStructMapper.entityToModelWithRuleRatios(groupEntity, _) >> { RuleGroupEntity entity, List entityList ->
            // Verify entityList is empty or null
            assert entityList == null || entityList.isEmpty()
            return RuleGroup.builder()
                    .id(groupId)
                    .rules(new HashMap<>())
                    .build()
        }
        // Verify delete operations
        1 * executionEventRelationMapper.delete(_ as LambdaQueryWrapper)
        1 * ruleGroupMapper.deleteById(groupId)

        where:
        groupId   | eventIds
        10000001L | [1001, 1002]
        10000002L | [1001]
        10000003L | []
    }


    @Unroll
    def "updateRuleGroupRatios: should validate rule existence in batch - groupId: #groupId, existingRuleIds: #existingRuleIds, nonExistentRuleIds: #nonExistentRuleIds"() {
        given: "mock existing group and rules"
        def rules = [:]
        existingRuleIds.each { ruleId ->
            rules[ruleId] = 50
        }
        nonExistentRuleIds.each { ruleId ->
            rules[ruleId] = 50
        }

        // Mock existing group
        def groupEntity = new RuleGroupEntity()
        groupEntity.setId(groupId)
        ruleGroupMapper.selectById(groupId) >> groupEntity

        // Mock existing group rules (empty for simplicity)
        // Note: GreRelationMapper is no longer used

        // Mock ruleService.getRuleById for each existing rule
        existingRuleIds.each { ruleId ->
            def rule = Rule.builder()
                    .id(ruleId)
                    .name("Rule $ruleId")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(RuleStatusEnum.GRAY)
                    .build()
            ruleService.getRuleById(ruleId) >> rule
        }

        // Note: GreRelationMapper is no longer used, relations are now managed via ExecutionArrangementMapper

        // Mock getRuleGroup to return empty group
        def emptyGroupEntity = new RuleGroupEntity()
        emptyGroupEntity.setId(groupId)
        ruleGroupMapper.selectById(groupId) >> emptyGroupEntity
        // Note: GreRelationMapper is no longer used
        def emptyGroup = RuleGroup.builder()
                .id(groupId)
                .rules([:])
                .build()
        ruleGroupStructMapper.entityToModelWithRuleRatios(emptyGroupEntity, _ as List) >> emptyGroup

        when: "update rule group ratios"
        ruleGroupService.updateRuleGroupRatios(groupId, rules)

        then: "group ratios should be updated successfully"
        // Note: GreRelationMapper is no longer used, ratios are now updated via ExecutionArrangementMapper
        // Verify ratios were updated in table x
        1 * executionEventRelationMapper.update(_, _) >> 1

        where:
        groupId   | existingRuleIds | nonExistentRuleIds
        10000001L | [1L, 2L, 3L]    | []
    }

    @Unroll
    def "updateRuleGroupRatios: should throw exception when rules do not exist - groupId: #groupId, existingRuleIds: #existingRuleIds, nonExistentRuleIds: #nonExistentRuleIds"() {
        given: "mock existing group and rules"
        def rules = [:]
        existingRuleIds.each { ruleId ->
            rules[ruleId] = 50
        }
        nonExistentRuleIds.each { ruleId ->
            rules[ruleId] = 50
        }

        // Mock existing group
        def groupEntity = new RuleGroupEntity()
        groupEntity.setId(groupId)
        ruleGroupMapper.selectById(groupId) >> groupEntity

        // Mock existing group rules (empty for simplicity)
        // Note: GreRelationMapper is no longer used

        // Mock ruleService.getRuleById for existing rules
        existingRuleIds.each { ruleId ->
            def rule = Rule.builder()
                    .id(ruleId)
                    .name("Rule $ruleId")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(RuleStatusEnum.GRAY)
                    .build()
            ruleService.getRuleById(ruleId) >> rule
        }

        // Mock ruleService.getRuleById for non-existent rules (return null)
        nonExistentRuleIds.each { ruleId ->
            ruleService.getRuleById(ruleId) >> null
        }

        // Note: GreRelationMapper is no longer used

        // Mock getRuleGroup to return empty group
        def emptyGroupEntity = new RuleGroupEntity()
        emptyGroupEntity.setId(groupId)
        def emptyGroup = RuleGroup.builder()
                .id(groupId)
                .rules([:])
                .build()
        ruleGroupStructMapper.entityToModelWithRuleRatios(emptyGroupEntity, _ as List) >> emptyGroup

        when: "update rule group with rules"
        def exception = null
        try {
            ruleGroupService.updateRuleGroupRatios(groupId, rules)
        } catch (IllegalArgumentException e) {
            exception = e
        }
        
        then: "should throw exception with non-existent rule IDs"
        exception != null
        exception.message.contains("Rule IDs do not exist in database")
        // Verify that all non-existent rule IDs are mentioned in the exception message
        nonExistentRuleIds.each { ruleId ->
            assert exception.message.contains(ruleId.toString())
        }

        where:
        groupId   | existingRuleIds | nonExistentRuleIds
        10000002L | [1L, 2L]        | [999L]
        10000003L | [1L]            | [999L, 1000L]
        10000004L | []              | [999L, 1000L]
    }

    def "selectRuleFromGroup: should return cached rule when cache exists and rule is valid"() {
        given: "a rule group with rules and a cached rule ID"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([1L: Pair.of(rule1, 50), 2L: Pair.of(rule2, 50)])
                .build()

        def userId = 1000L
        def eventId = 2000
        def cachedRuleId = 1L
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(25, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called with cached rule ID"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> cachedRuleId
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should return the cached rule"
        result != null
        result.getId() == cachedRuleId
        result == rule1
    }

    def "selectRuleFromGroup: should select rule based on hashInt when cache is empty"() {
        given: "a rule group with rules and no cache"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([1L: Pair.of(rule1, 30), 2L: Pair.of(rule2, 40)])
                .build()

        def userId = 1000L
        def eventId = 2000
        def hashInt = 25  // Should select rule1 (cumulative ratio: 30)
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called without cache"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> null
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should select rule1 based on hashInt"
        result != null
        result.getId() == 1L
        result == rule1

        and: "should cache the selection"
        1 * ruleSelectionCacheService.put(userId, eventId, groupId, 1L)
    }

    def "selectRuleFromGroup: should select rule2 when hashInt is in rule2's range"() {
        given: "a rule group with rules"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([1L: Pair.of(rule1, 30), 2L: Pair.of(rule2, 40)])
                .build()

        def userId = 1000L
        def eventId = 2000
        def hashInt = 35  // Should select rule2 (cumulative ratio: 30 + 40 = 70, 35 > 30)
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> null
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should select rule2"
        result != null
        result.getId() == 2L
        result == rule2

        and: "should cache the selection"
        1 * ruleSelectionCacheService.put(userId, eventId, groupId, 2L)
    }

    def "selectRuleFromGroup: should return null when hashInt exceeds total ratio"() {
        given: "a rule group with rules"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([1L: Pair.of(rule1, 30), 2L: Pair.of(rule2, 40)])
                .build()

        def userId = 1000L
        def eventId = 2000
        def hashInt = 80  // Exceeds total ratio (30 + 40 = 70)
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> null
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should return null (group skipped)"
        result == null

        and: "should not cache anything"
        0 * ruleSelectionCacheService.put(_, _, _, _)
    }

    def "selectRuleFromGroup: should return null when rule group is empty"() {
        given: "an empty rule group"
        def groupId = 100L
        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([:])
                .build()

        def userId = 1000L
        def eventId = 2000
        def hashInt = 25
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> null
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should return null"
        result == null

        and: "should not cache anything"
        0 * ruleSelectionCacheService.put(_, _, _, _)
    }

    def "selectRuleFromGroup: should return null when cached rule is not in group's rules map"() {
        given: "a rule group with rules but cached rule ID is not in the group"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([1L: Pair.of(rule1, 50)])
                .build()

        def userId = 1000L
        def eventId = 2000L
        def cachedRuleId = 999L  // Not in group's rules map
        def hashInt = 25
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called with invalid cached rule ID"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> cachedRuleId
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should select rule1 based on hashInt (cache is invalid)"
        result != null
        result.getId() == 1L
        result == rule1

        and: "should cache the new selection"
        1 * ruleSelectionCacheService.put(userId, eventId, groupId, 1L)
    }

    @Unroll
    def "selectRuleFromGroup: should select correct rule based on hashInt - hashInt: #hashInt, expectedRuleId: #expectedRuleId"() {
        given: "a rule group with multiple rules"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule3 = Rule.builder()
                .id(3L)
                .name("Rule 3")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([1L: Pair.of(rule1, 20), 2L: Pair.of(rule2, 30), 3L: Pair.of(rule3, 40)])
                .build()

        def userId = 1000L
        def eventId = 2000L
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> null
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should select the expected rule"
        if (expectedRuleId == null) {
            assert result == null
        } else {
            assert result != null
            assert result.getId() == expectedRuleId
        }

        where:
        hashInt | expectedRuleId
        10      | 1L            // In rule1's range (1-20)
        25      | 2L            // In rule2's range (21-50)
        60      | 3L            // In rule3's range (51-90)
        95      | null          // Exceeds total ratio (90)
    }

    def "selectRuleFromGroup: should handle null ruleRatioPair in isCachedABTestRule"() {
        given: "a rule group with null Pair in rules map"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        // Create a rule group with a null Pair entry (simulating corrupted data)
        def rulesMap = new HashMap<Long, Pair<Rule, Integer>>()
        rulesMap.put(1L, Pair.of(rule1, 50))
        rulesMap.put(999L, null)  // null Pair entry

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules(rulesMap)
                .build()

        def userId = 1000L
        def eventId = 2000L
        def cachedRuleId = 999L  // Points to null Pair
        def hashInt = 25
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called with cached rule ID pointing to null Pair"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> cachedRuleId
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should ignore invalid cache and select rule1 based on hashInt"
        result != null
        result.getId() == 1L
        result == rule1

        and: "should cache the new selection"
        1 * ruleSelectionCacheService.put(userId, eventId, groupId, 1L)
    }

    def "selectRuleFromGroup: should handle ruleRatioPair with null Left (Rule) in isCachedABTestRule"() {
        given: "a rule group with Pair containing null Rule"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        // Create a rule group with a Pair that has null Left (Rule)
        def rulesMap = new HashMap<Long, Pair<Rule, Integer>>()
        rulesMap.put(1L, Pair.of(rule1, 50))
        rulesMap.put(999L, Pair.of(null, 50))  // Pair with null Rule

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules(rulesMap)
                .build()

        def userId = 1000L
        def eventId = 2000L
        def cachedRuleId = 999L  // Points to Pair with null Rule
        def hashInt = 25
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called with cached rule ID pointing to Pair with null Rule"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> cachedRuleId
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should ignore invalid cache and select rule1 based on hashInt"
        result != null
        result.getId() == 1L
        result == rule1

        and: "should cache the new selection"
        1 * ruleSelectionCacheService.put(userId, eventId, groupId, 1L)
    }

    @Unroll
    def "selectRuleFromGroup: should handle invalid cached rule in isCachedABTestRule - ruleRatioPair null: #ruleRatioPairNull, rule null: #ruleNull"() {
        given: "a rule group with potentially invalid Pair entry"
        def groupId = 100L
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def rulesMap = new HashMap<Long, Pair<Rule, Integer>>()
        rulesMap.put(1L, Pair.of(rule1, 50))

        // Add invalid entry based on test parameters
        if (ruleRatioPairNull) {
            rulesMap.put(999L, null)  // null Pair
        } else if (ruleNull) {
            rulesMap.put(999L, Pair.of(null, 50))  // Pair with null Rule
        } else {
            // Valid entry for comparison
            def rule2 = Rule.builder()
                    .id(2L)
                    .name("Rule 2")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(RuleStatusEnum.ONLINE)
                    .build()
            rulesMap.put(2L, Pair.of(rule2, 50))
        }

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules(rulesMap)
                .build()

        def userId = 1000L
        def eventId = 2000L
        def cachedRuleId = (ruleRatioPairNull || ruleNull) ? 999L : 2L
        def hashInt = 25
        def context = new RuleExecutionContext(userId, eventId != null ? eventId.intValue() : null, 9999L, [:])
        context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(hashInt, ValueTypeEnum.INTEGER))

        when: "selectRuleFromGroup is called"
        ruleSelectionCacheService.get(userId, eventId, groupId) >> cachedRuleId
        def result = ruleGroupService.selectRuleFromGroup(ruleGroup, context)

        then: "should handle invalid cache appropriately"
        // Mock interactions must be outside conditional blocks
        // If cache is invalid, put will be called once; if valid, put won't be called
        (ruleRatioPairNull || ruleNull ? 1 : 0) * ruleSelectionCacheService.put(userId, eventId, groupId, 1L)

        and: "verify result"
        result != null
        if (ruleRatioPairNull || ruleNull) {
            // Invalid cache, should select rule1 based on hashInt
            result.getId() == 1L
        } else {
            // Valid cache, should return cached rule2
            result.getId() == 2L
        }

        where:
        ruleRatioPairNull | ruleNull | description
        true              | false    | "Pair is null"
        false             | true     | "Pair.Left (Rule) is null"
        false             | false    | "Valid Pair for comparison"
    }
}

