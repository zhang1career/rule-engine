package lab.zhang.rule.rule_engine.service.impl

import com.baomidou.mybatisplus.core.MybatisConfiguration
import com.baomidou.mybatisplus.core.conditions.Wrapper
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper
import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper
import lab.zhang.rule.rule_engine.mapper.RuleGroupMapper
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.cache.RuleSelectionCacheService
import lab.zhang.rule.rule_engine.service.RuleService
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper
import org.apache.commons.lang3.tuple.Pair
import org.apache.ibatis.builder.MapperBuilderAssistant
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
    ExecutionArrangementMapper executionArrangementMapper = Mock()
    RuleGroupStructMapper ruleGroupStructMapper = Mock()
    RuleSelectionCacheService ruleSelectionCacheService = Mock()

    def setup() {
        ruleGroupService = new RuleGroupServiceImpl()
        ruleGroupService.ruleService = ruleService
        ruleGroupService.ruleGroupMapper = ruleGroupMapper
        ruleGroupService.executionArrangementMapper = executionArrangementMapper
        ruleGroupService.ruleGroupStructMapper = ruleGroupStructMapper
        ruleGroupService.ruleSelectionCacheService = ruleSelectionCacheService
        // Setup MyBatis TableInfos
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ExecutionArrangementEntity.class);
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
        def defaultGroupId = 0L
        def standaloneArrangementEntityList = eventIds.collect { eventId ->
            def entity = new ExecutionArrangementEntity()
            entity.setEventId(eventId)
            entity.setRuleId(ruleId)
            entity.setGroupId(defaultGroupId)
            entity.setExeOrder(eventIds.indexOf(eventId))
            entity.setAbRatio(0)
            return entity
        }
        // Mock rule-event associations (existing group associations with groupId != 0)
        def groupId = 10000001L
        def groupedArrangementEntityList = eventIds.collect { eventId ->
            def entity = new ExecutionArrangementEntity()
            entity.setEventId(eventId)
            entity.setRuleId(ruleId)
            entity.setGroupId(groupId)
            entity.setExeOrder(eventIds.indexOf(eventId))
            entity.setAbRatio(0)
            return entity
        }

        // Mock selectList: return rule-event relations when querying for RULE type
        executionArrangementMapper.selectList(_ as LambdaQueryWrapper) >> { LambdaQueryWrapper wrapper ->
            // Return rule-event relations when querying for RULE type
            return arrangementEntityList
        }

        // Mock selectOne with different behavior based on the query
        def selectOneCallCount = 0
        executionArrangementMapper.selectOne(_ as LambdaQueryWrapper) >> {
            selectOneCallCount++
            if (selectOneCallCount <= 1) {
                // First call: Check for existing group associations (group_id != 0) - should return null
                return standaloneArrangementEntityList.find { it.groupId == defaultGroupId && it.ruleId == ruleId }
            } else {
                // Second call: Find standalone relation (group_id = 0) - should return the relation
                return groupedArrangementEntityList.find { it.groupId == groupId && it.ruleId == ruleId }
            }
        }

        // Mock insert for group-event associations
        1 * executionArrangementMapper.updateByPrimaryKey(_ as ExecutionArrangementEntity) >> { ExecutionArrangementEntity entity ->
            // Verify it's a group-event association
            assert entity.getRuleId() == ruleId
            assert entity.getGroupId() == groupId
            assert eventIds.contains(entity.getEventId())
            return 1
        }

        ruleGroupMapper.insert(_ as RuleGroupEntity) >> { RuleGroupEntity entity ->
            entity.setId(groupId)
            return 1
        }

        when: "create rule group"
        // createRuleGroup now requires eventId parameter
        def eventId = eventIds[0] // Use the first eventId from the test parameters
        def group = ruleGroupService.createRuleGroup(rule, eventId)

        then: "group should be created and rule-event associations copied to group"
        group != null
        group.getId() == groupId
        group.getRuleIds().contains(ruleId)

        where:
        ruleId | eventIds
        1L     | [1001, 1002]
        2L     | [1001]
        3L     | [1001, 1002, 1003]
    }


    @Unroll
    def "Business Logic 3a: When group has rules, delete rule group should throw exception - ruleId: #ruleId, groupId: #groupId"() {
        given: "a rule in ONLINE status in a group"
        // mock select rule group by groupId
        ruleGroupMapper.selectById(_ as Long) >> { Long id ->
            def entity = new RuleGroupEntity()
            entity.setId(id)
            return entity
        }

        // mock select rule-event relations for the rule in the group
        executionArrangementMapper.selectList(_ as Wrapper<ExecutionArrangementEntity>) >> { Wrapper<ExecutionArrangementEntity> wrapper ->
            def arrangementEntities = []
            arrangementEntities.add(new ExecutionArrangementEntity().tap {
                setEventId(1001)
                setRuleId(ruleId)
                setGroupId(groupId)
                setExeOrder(0)
                setAbRatio(0)
            })
            return arrangementEntities
        }

        // mock struct mapping
        ruleGroupStructMapper.entityToModelWithRuleRatios(_ as RuleGroupEntity, _ as List) >> { RuleGroupEntity entity, List<ExecutionArrangementEntity> entityList ->
            def rule = Rule.builder()
                    .id(ruleId)
                    .name("Test Rule")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(RuleStatusEnum.ONLINE)  // Rule should be in ONLINE status initially
                    .build()
            def rulesMap = new HashMap<Long, Pair<Rule, Integer>>()
            rulesMap.put(ruleId, Pair.of(rule, 100))
            return RuleGroup.builder()
                    .id(entity.getId())
                    .rules(rulesMap)
                    .build()
        }

        when: "delete rule group"
        ruleGroupService.deleteRuleGroup(groupId)

        then: "should throw IllegalStateException"
        thrown(IllegalStateException)

        where:
        ruleId | groupId   | abRatio | newStatus
        1L     | 10000001L | 0       | RuleStatusEnum.OFFLINE
        2L     | 10000002L | 50      | RuleStatusEnum.TEST
        3L     | 10000003L | 0       | RuleStatusEnum.GRAY
        4L     | 10000004L | 0       | RuleStatusEnum.ONLINE
    }

    def "Business Logic 3b: When group has no rules, delete rule group should succeed"() {
        given: "a rule in ONLINE status in a group"
        def ruleId = 4L
        def groupId = 10000004L

        // mock select rule group by groupId
        ruleGroupMapper.selectById(_ as Long) >> { Long id ->
            def entity = new RuleGroupEntity()
            entity.setId(id)
            return entity
        }

        // mock select rule-event relations for the rule in the group
        executionArrangementMapper.selectList(_ as Wrapper<ExecutionArrangementEntity>) >> { Wrapper<ExecutionArrangementEntity> wrapper ->
            return []
        }

        // mock struct mapping
        ruleGroupStructMapper.entityToModelWithRuleRatios(_ as RuleGroupEntity, _ as List) >> { RuleGroupEntity entity, List<ExecutionArrangementEntity> entityList ->
            return RuleGroup.builder()
                    .id(entity.getId())
                    .rules(new HashMap<>())
                    .build()
        }

        when: "delete rule group"
        ruleGroupService.deleteRuleGroup(groupId)

        then: "should throw IllegalStateException"
        1 * ruleGroupMapper.deleteById(groupId) >> 1
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
        executionArrangementMapper.selectList(_ as LambdaQueryWrapper) >> { LambdaQueryWrapper wrapper ->
            callCount++
            // First call: getRuleGroup queries by groupId, should return empty for empty group
            if (callCount == 1) {
                return emptyGroupRelations
            }
            // Second call: deleteRuleGroupIfEmpty queries by groupId to find old associations
            return oldGroupRelations
        }

        // Mock delete operations
        executionArrangementMapper.delete(_ as LambdaQueryWrapper) >> eventIds.size()
        ruleGroupMapper.deleteById(groupId) >> 1

        when: "delete empty group"
        ruleGroupService.deleteRuleGroup(groupId)

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
        1 * ruleGroupMapper.deleteById(groupId)

        where:
        groupId   | eventIds
        10000001L | [1001, 1002]
        10000002L | [1001]
        10000003L | []
    }

    @Unroll
    def "updateRuleGroupRatios a: should update rule ratios - groupId: #groupId, existingRuleIds: #existingRuleIds, nonExistentRuleIds: #nonExistentRuleIds"() {
        given: "mock existing group and rules"
        def ruleRatios = [:]
        existingRuleIds.each { Long ruleId ->
            ruleRatios[ruleId] = 50
        }
        nonExistentRuleIds.each { Object ruleId ->
            ruleRatios[(Long) ruleId] = 50
        }

        // mock existing group
        def groupEntity = new RuleGroupEntity()
        groupEntity.setId(groupId)
        ruleGroupMapper.selectById(_ as Long) >> { Long id ->
            def entity = new RuleGroupEntity()
            entity.setId(id)
            return entity
        }

        // mock select rule-event relations for the rule in the group
        executionArrangementMapper.selectList(_ as Wrapper<ExecutionArrangementEntity>) >> { Wrapper<ExecutionArrangementEntity> wrapper ->
            def arrangementEntities = []
            existingRuleIds.each { Long ruleId ->
                arrangementEntities.add(new ExecutionArrangementEntity().tap {
                    setEventId(eventId)
                    setRuleId(ruleId)
                    setGroupId(groupId)
                    setExeOrder(0)
                    setAbRatio(0)
                })
            }
            return arrangementEntities
        }

        // mock struct mapping
        ruleGroupStructMapper.entityToModelWithRuleRatios(_ as RuleGroupEntity, _ as List) >> { RuleGroupEntity entity, List<ExecutionArrangementEntity> entityList ->
            def rulesMap = new HashMap<Long, Pair<Rule, Integer>>()
            existingRuleIds.each { Long ruleId ->
                def rule = Rule.builder()
                        .id(ruleId)
                        .name("Rule $ruleId")
                        .contentType(ContentTypeEnum.EXPRESSION)
                        .ruleStatus(RuleStatusEnum.GRAY)
                        .build()
                rulesMap.put(ruleId, Pair.of(rule, 0))
            }
            return RuleGroup.builder()
                    .id(entity.getId())
                    .rules(rulesMap)
                    .build()
        }

        when: "update rule group ratios"
        ruleGroupService.updateRuleGroupRatios(groupId, ruleRatios as Map<Long, Integer>)

        then: "group ratios should be updated successfully"
        existingRuleIds.size() * executionArrangementMapper.update(_, _) >> 1

        where:
        eventId | groupId   | existingRuleIds | nonExistentRuleIds
        1001    | 10000001L | [1L, 2L, 3L]    | []
    }

    @Unroll
    def "updateRuleGroupRatios b: should validate rule existence in batch - groupId: #groupId, existingRuleIds: #existingRuleIds, nonExistentRuleIds: #nonExistentRuleIds"() {
        given: "mock existing group and rules"
        def ruleRatios = [:]
        existingRuleIds.each { Long ruleId ->
            ruleRatios[ruleId] = 50
        }
        nonExistentRuleIds.each { Object ruleId ->
            ruleRatios[(Long) ruleId] = 50
        }

        // mock existing group
        def groupEntity = new RuleGroupEntity()
        groupEntity.setId(groupId)
        ruleGroupMapper.selectById(_ as Long) >> { Long id ->
            def entity = new RuleGroupEntity()
            entity.setId(id)
            return entity
        }

        // mock select rule-event relations for the rule in the group
        executionArrangementMapper.selectList(_ as Wrapper<ExecutionArrangementEntity>) >> { Wrapper<ExecutionArrangementEntity> wrapper ->
            def arrangementEntities = []
            existingRuleIds.each { Long ruleId ->
                arrangementEntities.add(new ExecutionArrangementEntity().tap {
                    setEventId(eventId)
                    setRuleId(ruleId)
                    setGroupId(groupId)
                    setExeOrder(0)
                    setAbRatio(0)
                })
            }
            return arrangementEntities
        }

        // mock struct mapping
        ruleGroupStructMapper.entityToModelWithRuleRatios(_ as RuleGroupEntity, _ as List) >> { RuleGroupEntity entity, List<ExecutionArrangementEntity> entityList ->
            def rulesMap = new HashMap<Long, Pair<Rule, Integer>>()
            existingRuleIds.each { Long ruleId ->
                def rule = Rule.builder()
                        .id(ruleId)
                        .name("Rule $ruleId")
                        .contentType(ContentTypeEnum.EXPRESSION)
                        .ruleStatus(RuleStatusEnum.GRAY)
                        .build()
                rulesMap.put(ruleId, Pair.of(rule, 0))
            }
            return RuleGroup.builder()
                    .id(entity.getId())
                    .rules(rulesMap)
                    .build()
        }

        when: "update rule group ratios"
        def exception = null
        try {
            ruleGroupService.updateRuleGroupRatios(groupId, ruleRatios as Map<Long, Integer>)
        } catch (IllegalArgumentException e) {
            exception = e
        }

        then: "should throw exception with non-existent rule IDs"
        exception != null
        exception.message.contains("does not exist in group")
        // Verify that all non-existent rule IDs are mentioned in the exception message
        nonExistentRuleIds.each { ruleId ->
            assert exception.message.contains(ruleId.toString())
        }

        where:
        eventId | groupId   | existingRuleIds | nonExistentRuleIds
        1001    | 10000002L | [1L, 2L]        | [999L]
        1001    | 10000003L | [1L]            | [999L, 1000L]
        1001    | 10000004L | []              | [999L, 1000L]

    }
}
