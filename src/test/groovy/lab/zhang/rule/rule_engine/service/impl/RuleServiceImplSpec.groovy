package lab.zhang.rule.rule_engine.service.impl


import lab.zhang.rule.rule_engine.config.RuleStatusConfig
import lab.zhang.rule.rule_engine.entity.*
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.executor.impl.ApiQueryRuleExecutor
import lab.zhang.rule.rule_engine.executor.impl.ExpressionRuleExecutor
import lab.zhang.rule.rule_engine.executor.impl.ScriptRuleExecutor
import lab.zhang.rule.rule_engine.executor.impl.SqlQueryRuleExecutor
import lab.zhang.rule.rule_engine.mapper.*
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO
import lab.zhang.rule.rule_engine.service.RuleGroupService
import lab.zhang.rule.rule_engine.struct_mapper.RuleGroupStructMapper
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper
import spock.lang.Specification
import spock.lang.Unroll

import java.util.Collections
import java.util.HashSet

import static lab.zhang.rule.rule_engine.util.RatioUtil.buildRatioKey

/**
 * RuleService unit test
 *
 * @author Rongjin Zhang
 */
class RuleServiceImplSpec extends Specification {

    def ruleMapper = Mock(RuleMapper)
    def ruleContentMapper = Mock(RuleContentMapper)
    def executionEventRelationMapper = Mock(ExecutionEventRelationMapper)
    def ruleGroupMapper = Mock(RuleGroupMapper)
    def ruleGroupRuleRelationMapper = Mock(RuleGroupRuleRelationMapper)
    def ruleStructMapper = Mock(RuleStructMapper)
    def ruleGroupStructMapper = Mock(RuleGroupStructMapper)
    def ruleStatusConfig = Mock(RuleStatusConfig)
    def ruleService = new RuleServiceImpl()
    def ruleGroupService = Mock(RuleGroupService)

    def setup() {
        ruleService.ruleMapper = ruleMapper
        ruleService.ruleContentMapper = ruleContentMapper
        ruleService.executionEventRelationMapper = executionEventRelationMapper
        ruleService.ruleStructMapper = ruleStructMapper
        ruleService.ruleGroupService = ruleGroupService
        ruleService.ruleStatusConfig = ruleStatusConfig
        ruleService.ruleGroupMapper = ruleGroupMapper
        ruleService.ruleGroupRuleRelationMapper = ruleGroupRuleRelationMapper
        ruleService.ruleGroupStructMapper = ruleGroupStructMapper

        // Setup default mock for RuleStatusConfig (TEST environment)
        _ * ruleStatusConfig.getAllowedRuleStatuses() >> Collections.singleton(RuleStatusEnum.TEST)

        // Setup RuleExecutor instances for content validation
        def expressionExecutor = new ExpressionRuleExecutor()
        def scriptExecutor = new ScriptRuleExecutor()
        def apiQueryExecutor = new ApiQueryRuleExecutor(new org.springframework.web.client.RestTemplate())
        def sqlQueryExecutor = new SqlQueryRuleExecutor(Mock(org.springframework.jdbc.core.JdbcTemplate))
        ruleService.ruleExecutors = [expressionExecutor, scriptExecutor, apiQueryExecutor, sqlQueryExecutor]

        // Setup default mock for ruleStructMapper.entityToModel
        _ * ruleStructMapper.entityToModel(_ as RuleEntity) >> { RuleEntity entity ->
            if (entity == null) {
                return null
            }
            Rule.builder()
                    .id(entity.id)
                    .name(entity.name != null ? entity.name : "")
                    .contentType(ContentTypeEnum.fromId(entity.contentType))
                    .ruleStatus(RuleStatusEnum.fromId(entity.ruleStatus))
                    .ruleGroupId(entity.ruleGroupId != null && entity.ruleGroupId != 0L ? entity.ruleGroupId : null)
                    .description(entity.description != null ? entity.description : "")
                    .content("")
                    .build()
        }

        // Setup default mock for ruleGroupStructMapper.entityToModel
        _ * ruleGroupStructMapper.entityToModel(_ as RuleGroupEntity, _) >> { RuleGroupEntity entity, Map relationsMap ->
            if (entity == null) {
                return null
            }
            def ruleGroup = new RuleGroup(entity.id != null ? entity.id : 0L)
            ruleGroup.setRules([:])
            return ruleGroup
        }

        // Setup default mock for ruleGroupStructMapper.modelToDTOWithRules to handle null ratiosMap
        _ * ruleGroupStructMapper.modelToDTOWithRules(_ as RuleGroup, _) >> { RuleGroup group, Map ratiosMap ->
            def dto = new RuleGroupDTO()
            dto.id = group != null ? group.id : null
            dto.rules = [:]
            if (group != null && group.getRuleIds() != null && !group.getRuleIds().isEmpty() && ratiosMap != null) {
                Map<Long, Integer> rules = [:]
                Long groupId = group.getId()
                for (Long ruleId : group.getRuleIds()) {
                    String key = buildRatioKey(groupId, ruleId)
                    Integer ratio = (ratiosMap != null && ratiosMap.containsKey(key) && ratiosMap.get(key) != null) ? ratiosMap.get(key) : 0;
                    rules.put(ruleId, ratio);
                }
                dto.rules = rules
            }
            return dto
        }
    }

    // ========== getAllRules() tests ==========

    def "test getAllRules - should return empty list when no rules exist"() {
        when: "get all rules"
        def rules = ruleService.getAllRules()

        then: "should return empty list"
        1 * ruleMapper.selectList(null) >> []
        rules != null
        rules.isEmpty()
    }

    def "test getAllRules - should return all rules"() {
        given: "multiple rules exist"
        def ruleEntity1 = createRuleEntity(1L, RuleStatusEnum.TEST)
        def ruleEntity2 = createRuleEntity(2L, RuleStatusEnum.FULL)
        def ruleEntity3 = createRuleEntity(3L, RuleStatusEnum.OFFLINE)

        when: "get all rules"
        def rules = ruleService.getAllRules()

        then: "should return all rules"
        1 * ruleMapper.selectList(null) >> [ruleEntity1, ruleEntity2, ruleEntity3]
        rules.size() == 3
        rules[0].id == 1L
        rules[1].id == 2L
        rules[2].id == 3L
    }

    def "test getAllRules - should return empty list when selectList returns null"() {
        when: "get all rules"
        def rules = ruleService.getAllRules()

        then: "should return empty list"
        1 * ruleMapper.selectList(null) >> null
        rules != null
        rules.isEmpty()
    }

    // ========== getRuleById() tests ==========

    @Unroll
    def "test getRuleById - should return rule when exists - ruleId: #ruleId"() {
        given: "a rule exists"
        def ruleEntity = createRuleEntity(ruleId, RuleStatusEnum.TEST)
        def contentEntity = createContentEntity(ruleId)
        contentEntity.content = "test content"

        when: "get rule by id"
        def rule = ruleService.getRuleById(ruleId)

        then: "should return rule with content"
        1 * ruleMapper.selectById(ruleId) >> ruleEntity
        1 * ruleContentMapper.selectById(ruleId) >> contentEntity
        rule != null
        rule.id == ruleId
        rule.content == "test content"

        where:
        ruleId << [1L, 2L, 10000001L]
    }

    def "test getRuleById - should return null when rule does not exist"() {
        given: "rule does not exist"
        def ruleId = 999L

        when: "get rule by id"
        def rule = ruleService.getRuleById(ruleId)

        then: "should return null"
        1 * ruleMapper.selectById(ruleId) >> null
        rule == null
    }

    def "test getRuleById - should return empty content when content does not exist"() {
        given: "rule exists but content does not"
        def ruleId = 1L
        def ruleEntity = createRuleEntity(ruleId, RuleStatusEnum.TEST)

        when: "get rule by id"
        def rule = ruleService.getRuleById(ruleId)

        then: "should return rule with empty content"
        1 * ruleMapper.selectById(ruleId) >> ruleEntity
        1 * ruleContentMapper.selectById(ruleId) >> null
        rule != null
        rule.content == ""
    }

    def "test getRuleById - should return empty content when content is null"() {
        given: "rule exists but content is null"
        def ruleId = 1L
        def ruleEntity = createRuleEntity(ruleId, RuleStatusEnum.TEST)
        def contentEntity = createContentEntity(ruleId)
        contentEntity.content = null

        when: "get rule by id"
        def rule = ruleService.getRuleById(ruleId)

        then: "should return rule with empty content"
        1 * ruleMapper.selectById(ruleId) >> ruleEntity
        1 * ruleContentMapper.selectById(ruleId) >> contentEntity
        rule != null
        rule.content == ""
    }

    // ========== createRule() tests ==========

    @Unroll
    def "test createRule - should create rule with OFFLINE status - contentType: #contentType"() {
        given: "a new rule"
        def rule = Rule.builder()
                .id(null)
                .name("Test Rule")
                .contentType(contentType)
                .content("test content")
                .description("test description")
                .build()

        when: "create rule"
        ruleService.createRule(rule)

        then: "should create rule with OFFLINE status"
        1 * ruleStructMapper.modelToEntity(_) >> { Rule r ->
            def entity = new RuleEntity()
            entity.name = r.name
            entity.contentType = r.contentType != null ? r.contentType.getId() : null
            entity.description = r.description
            entity.ruleStatus = RuleStatusEnum.OFFLINE.getId()
            return entity
        }
        1 * ruleMapper.insert(_) >> { RuleEntity entity ->
            assert entity.ruleStatus == RuleStatusEnum.OFFLINE.getId()
            entity.id = 10000001L
            rule.id = 10000001L
            return 1
        }
        1 * ruleStructMapper.modelToContentEntity(_) >> { Rule r ->
            def content = new RuleContentEntity()
            content.id = 10000001L
            content.content = r.content
            return content
        }
        1 * ruleContentMapper.insert(_) >> 1
        rule.id == 10000001L
        rule.ruleStatus == RuleStatusEnum.OFFLINE

        where:
        contentType << [ContentTypeEnum.EXPRESSION, ContentTypeEnum.SCRIPT, ContentTypeEnum.API_QUERY, ContentTypeEnum.SQL_QUERY]
    }

    def "test createRule - should skip content insert when content is null"() {
        given: "a new rule without content"
        def rule = Rule.builder()
                .id(null)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content(null)
                .build()

        when: "create rule"
        ruleService.createRule(rule)

        then: "should create rule without content"
        1 * ruleStructMapper.modelToEntity(_) >> { Rule r ->
            def entity = new RuleEntity()
            entity.name = r.name
            entity.contentType = r.contentType != null ? r.contentType.getId() : null
            entity.ruleStatus = RuleStatusEnum.OFFLINE.getId()
            return entity
        }
        1 * ruleMapper.insert(_) >> { RuleEntity entity ->
            entity.id = 10000001L
            rule.id = 10000001L
            return 1
        }
        0 * ruleContentMapper.insert(_)
    }

    def "test createRule - should throw exception when rule is null"() {
        when: "create null rule"
        ruleService.createRule(null)

        then: "should throw exception"
        thrown(Exception)
    }

    // ========== updateRule() tests ==========

    @Unroll
    def "test updateRule - should update rule successfully - ruleId: #ruleId, fromStatus: #fromStatus, toStatus: #toStatus"() {
        given: "an existing rule"
        def existingEntity = createRuleEntity(ruleId, fromStatus)
        existingEntity.name = "Old Name"
        existingEntity.contentType = ContentTypeEnum.EXPRESSION.getId()
        existingEntity.ct = 1000

        def existingContent = createContentEntity(ruleId)
        existingContent.content = "old content"

        def newRule = Rule.builder()
                .id(ruleId)
                .name("New Name")
                .contentType(ContentTypeEnum.SCRIPT)
                .content("return 'new content'")
                .ruleStatus(toStatus)
                .build()

        when: "update rule"
        ruleService.updateRule(ruleId, newRule)

        then: "should update rule successfully"
        1 * ruleMapper.selectById(ruleId) >> existingEntity
        1 * ruleStructMapper.entityToModel(existingEntity) >> {
            Rule.builder()
                    .id(ruleId)
                    .name("Old Name")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(fromStatus)
                    .content("old content")
                    .build()
        }
        if (fromStatus != toStatus) {
            // Status change handling
            if (fromStatus != RuleStatusEnum.AB_TEST && toStatus == RuleStatusEnum.AB_TEST) {
                // Transition to AB_TEST
                1 * ruleGroupService.createRuleGroup(_) >> {
                    def group = new RuleGroup(10000001L)
                    return group
                }
            } else if (fromStatus == RuleStatusEnum.AB_TEST && toStatus != RuleStatusEnum.AB_TEST) {
                // Transition from AB_TEST
                1 * ruleGroupService.setOtherRulesOffline(_)
                1 * ruleGroupService.doDeleteRuleGroup(_)
            }
        }
        1 * ruleStructMapper.modelToEntity(_) >> { Rule r ->
            def entity = new RuleEntity()
            entity.id = r.id
            entity.name = r.name
            entity.contentType = r.contentType != null ? r.contentType.getId() : null
            entity.ruleStatus = r.ruleStatus != null ? r.ruleStatus.getId() : null
            entity.ct = existingEntity.ct
            return entity
        }
        1 * ruleMapper.updateById(_) >> 1
        1 * ruleStructMapper.modelToContentEntity(_) >> { Rule r ->
            def content = new RuleContentEntity()
            content.id = r.id
            content.content = r.content
            return content
        }
        1 * ruleContentMapper.updateById(_) >> 1

        where:
        ruleId | fromStatus             | toStatus
        1L     | RuleStatusEnum.TEST    | RuleStatusEnum.TEST
        2L     | RuleStatusEnum.TEST    | RuleStatusEnum.GRAY
        3L     | RuleStatusEnum.GRAY    | RuleStatusEnum.AB_TEST
        4L     | RuleStatusEnum.AB_TEST | RuleStatusEnum.FULL
        5L     | RuleStatusEnum.FULL    | RuleStatusEnum.OFFLINE
    }

    def "test updateRule - should throw exception when rule not found"() {
        given: "rule does not exist"
        def ruleId = 999L
        def newRule = Rule.builder()
                .id(ruleId)
                .name("New Name")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        when: "update non-existent rule"
        ruleService.updateRule(ruleId, newRule)

        then: "should throw IllegalArgumentException"
        1 * ruleMapper.selectById(ruleId) >> null
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "test updateRule - should throw exception for invalid status transition - fromStatus: #fromStatus, toStatus: #toStatus"() {
        given: "an existing rule"
        def ruleId = 1L
        def existingEntity = createRuleEntity(ruleId, fromStatus)
        def existingContent = createContentEntity(ruleId)

        def newRule = Rule.builder()
                .id(ruleId)
                .name("New Name")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(toStatus)
                .build()

        when: "update rule with invalid status transition"
        ruleService.updateRule(ruleId, newRule)

        then: "should throw IllegalArgumentException"
        1 * ruleMapper.selectById(ruleId) >> existingEntity
        1 * ruleStructMapper.entityToModel(existingEntity) >> {
            Rule.builder()
                    .id(ruleId)
                    .name("Old Name")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(fromStatus)
                    .build()
        }
        IllegalArgumentException e = thrown()
        e.message.contains("Invalid status transition")

        where:
        fromStatus             | toStatus
        RuleStatusEnum.OFFLINE | RuleStatusEnum.GRAY
        RuleStatusEnum.OFFLINE | RuleStatusEnum.AB_TEST
        RuleStatusEnum.TEST    | RuleStatusEnum.AB_TEST
        RuleStatusEnum.TEST    | RuleStatusEnum.FULL
        RuleStatusEnum.GRAY    | RuleStatusEnum.TEST
        RuleStatusEnum.GRAY    | RuleStatusEnum.FULL
        RuleStatusEnum.AB_TEST | RuleStatusEnum.TEST
        RuleStatusEnum.AB_TEST | RuleStatusEnum.GRAY
        RuleStatusEnum.FULL    | RuleStatusEnum.TEST
        RuleStatusEnum.FULL    | RuleStatusEnum.GRAY
    }

    def "test updateRule - should preserve existing values when new values are null"() {
        given: "an existing rule"
        def ruleId = 1L
        def existingEntity = createRuleEntity(ruleId, RuleStatusEnum.TEST)
        existingEntity.name = "Old Name"
        existingEntity.contentType = ContentTypeEnum.EXPRESSION.getId()
        existingEntity.description = "Old Description"
        existingEntity.ct = 1000

        def existingContent = createContentEntity(ruleId)
        existingContent.content = "old content"

        def newRule = Rule.builder()
                .id(ruleId)
                .name(null)
                .contentType(null)
                .content(null)
                .description(null)
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        when: "update rule with null values"
        ruleService.updateRule(ruleId, newRule)

        then: "should preserve existing values"
        1 * ruleMapper.selectById(ruleId) >> existingEntity
        1 * ruleStructMapper.entityToModel(existingEntity) >> {
            Rule.builder()
                    .id(ruleId)
                    .name("Old Name")
                    .contentType(ContentTypeEnum.EXPRESSION)
                    .ruleStatus(RuleStatusEnum.TEST)
                    .description("Old Description")
                    .content("old content")
                    .build()
        }
        1 * ruleStructMapper.modelToEntity(_) >> { Rule r ->
            def entity = new RuleEntity()
            entity.id = r.id
            entity.name = r.name != null && !r.name.trim().isEmpty() ? r.name : "Old Name"
            entity.contentType = r.contentType != null ? r.contentType.getId() : ContentTypeEnum.EXPRESSION.getId()
            entity.ruleStatus = r.ruleStatus != null ? r.ruleStatus.getId() : RuleStatusEnum.TEST.getId()
            entity.description = r.description != null ? r.description : "Old Description"
            entity.ct = existingEntity.ct
            return entity
        }
        1 * ruleMapper.updateById(_) >> 1
        0 * ruleContentMapper.updateById(_)
    }

    // ========== doUpdateRule() tests ==========

    @Unroll
    def "test doUpdateRule - should update rule content when both contentType and content provided - contentType: #contentType"() {
        given: "existing and new rules"
        def existingRule = Rule.builder()
                .id(1L)
                .name("Old Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("old content")
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        def newRule = Rule.builder()
                .id(1L)
                .name("New Rule")
                .contentType(contentType)
                .content(validContent)
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        when: "do update rule"
        ruleService.doUpdateRule(existingRule, newRule)

        then: "should update both entity and content"
        1 * ruleStructMapper.modelToEntity(_) >> { Rule r ->
            def entity = new RuleEntity()
            entity.id = r.id
            entity.name = r.name
            entity.contentType = r.contentType != null ? r.contentType.getId() : null
            entity.ruleStatus = r.ruleStatus != null ? r.ruleStatus.getId() : null
            return entity
        }
        1 * ruleMapper.updateById(_) >> 1
        1 * ruleStructMapper.modelToContentEntity(_) >> { Rule r ->
            def content = new RuleContentEntity()
            content.id = r.id
            content.content = r.content
            return content
        }
        1 * ruleContentMapper.updateById(_) >> 1

        where:
        contentType              | validContent
        ContentTypeEnum.EXPRESSION | "1 + 1"
        ContentTypeEnum.SCRIPT     | "return 'new content'"
        ContentTypeEnum.API_QUERY  | '{"url": "http://example.com"}'
        ContentTypeEnum.SQL_QUERY  | "SELECT 1"
    }

    def "test doUpdateRule - should skip content update when content is null"() {
        given: "existing and new rules"
        def existingRule = Rule.builder()
                .id(1L)
                .name("Old Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("old content")
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        def newRule = Rule.builder()
                .id(1L)
                .name("New Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content(null)
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        when: "do update rule without content"
        ruleService.doUpdateRule(existingRule, newRule)

        then: "should update entity but not content"
        1 * ruleStructMapper.modelToEntity(_) >> { Rule r ->
            def entity = new RuleEntity()
            entity.id = r.id
            entity.name = r.name
            entity.contentType = r.contentType != null ? r.contentType.getId() : null
            entity.ruleStatus = r.ruleStatus != null ? r.ruleStatus.getId() : null
            return entity
        }
        1 * ruleMapper.updateById(_) >> 1
        0 * ruleContentMapper.updateById(_)
    }

    def "test doUpdateRule - should throw exception when executor not found"() {
        given: "existing and new rules with unsupported contentType"
        def existingRule = Rule.builder()
                .id(1L)
                .name("Old Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("old content")
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        def newRule = Rule.builder()
                .id(1L)
                .name("New Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("invalid content")
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        and: "no executor available"
        ruleService.ruleExecutors = []

        when: "do update rule"
        ruleService.doUpdateRule(existingRule, newRule)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    // ========== deleteRule() tests ==========

    @Unroll
    def "test deleteRule - should delete rule successfully - ruleId: #ruleId"() {
        given: "a rule with OFFLINE status"
        def ruleEntity = createRuleEntity(ruleId, RuleStatusEnum.OFFLINE)
        def contentEntity = createContentEntity(ruleId)

        when: "delete rule"
        ruleService.deleteRule(ruleId)

        then: "should delete rule and content"
        1 * ruleMapper.selectById(ruleId) >> ruleEntity
        1 * ruleContentMapper.selectById(ruleId) >> contentEntity
        1 * ruleContentMapper.deleteById(ruleId) >> 1
        1 * ruleMapper.deleteById(ruleId) >> 1
        1 * executionEventRelationMapper.delete(_) >> 1

        where:
        ruleId << [1L, 2L, 10000001L]
    }

    @Unroll
    def "test deleteRule - should throw exception when rule status is not OFFLINE - status: #status"() {
        given: "a rule with non-OFFLINE status"
        def ruleId = 1L
        def ruleEntity = createRuleEntity(ruleId, status)
        def contentEntity = createContentEntity(ruleId)

        when: "delete rule"
        ruleService.deleteRule(ruleId)

        then: "should throw IllegalArgumentException"
        1 * ruleMapper.selectById(ruleId) >> ruleEntity
        1 * ruleContentMapper.selectById(ruleId) >> contentEntity
        IllegalArgumentException e = thrown()
        e.message.contains("can only be deleted when status is OFFLINE")

        where:
        status << [RuleStatusEnum.TEST, RuleStatusEnum.GRAY, RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL]
    }

    def "test deleteRule - should throw exception when rule does not exist"() {
        given: "rule does not exist"
        def ruleId = 999L

        when: "delete non-existent rule"
        ruleService.deleteRule(ruleId)

        then: "should throw IllegalArgumentException"
        1 * ruleMapper.selectById(ruleId) >> null
        thrown(IllegalArgumentException)
    }

    // ========== saveExecutionSequence() tests ==========

    @Unroll
    def "test saveExecutionSequence - should save execution sequence successfully - eventId: #eventId, ruleIds: #ruleIds"() {
        given: "rules exist"
        ruleIds.each { ruleId ->
            def ruleEntity = createRuleEntity(ruleId, RuleStatusEnum.TEST)
            1 * ruleMapper.selectById(ruleId) >> ruleEntity
        }

        when: "save execution sequence"
        ruleService.saveExecutionSequence(eventId, ruleIds)

        then: "should save execution sequence"
        1 * executionEventRelationMapper.selectList(_) >> []
        1 * executionEventRelationMapper.delete(_) >> 1
        ruleIds.size() * executionEventRelationMapper.insert(_) >> 1

        where:
        eventId | ruleIds
        1001L   | [1L, 2L]
        1002L   | [1L, 2L, 3L]
        1003L   | [1L]
    }

    def "test saveExecutionSequence - should throw exception when eventId is null"() {
        when: "save execution sequence with null eventId"
        ruleService.saveExecutionSequence(null, [1L])

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test saveExecutionSequence - should handle empty ruleIds list"() {
        given: "eventId"
        def eventId = 1001L

        when: "save execution sequence with empty ruleIds"
        ruleService.saveExecutionSequence(eventId, [])

        then: "should delete old relations but not insert new ones"
        1 * executionEventRelationMapper.selectList(_) >> []
        1 * executionEventRelationMapper.delete(_) >> 1
        0 * executionEventRelationMapper.insert(_)
    }

    def "test saveExecutionSequence - should remove old relations and add new ones"() {
        given: "old and new ruleIds"
        def eventId = 1001L
        def oldRuleIds = [1L, 2L]
        def newRuleIds = [2L, 3L]

        def oldRelations = []
        oldRuleIds.eachWithIndex { ruleId, index ->
            def entity = new ExecutionEventRelationEntity()
            entity.eventId = eventId
            entity.itemId = ruleId
            entity.itemType = ExecutionItemTypeEnum.RULE.getId()
            entity.executionOrder = index + 1
            oldRelations.add(entity)
        }

        // Only ruleId=1L is removed (in oldRuleIds but not in newRuleIds), so getRuleById(1L) will be called
        def rule1Entity = createRuleEntity(1L, RuleStatusEnum.TEST)
        def rule1ContentEntity = createContentEntity(1L)

        when: "save execution sequence"
        ruleService.saveExecutionSequence(eventId, newRuleIds)

        then: "should remove old relations and add new ones"
        1 * executionEventRelationMapper.selectList(_) >> oldRelations
        1 * ruleMapper.selectById(1L) >> rule1Entity
        1 * ruleContentMapper.selectById(1L) >> rule1ContentEntity
        1 * executionEventRelationMapper.delete(_) >> 1
        2 * executionEventRelationMapper.insert(_) >> 1
    }

    // ========== getExecutionItemsByEventId() tests ==========

    def "test getExecutionItemsByEventId - should return empty list when no relations exist"() {
        given: "eventId with no relations"
        def eventId = 1001L

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId)

        then: "should return empty list"
        1 * executionEventRelationMapper.selectList(_) >> []
        items != null
        items.isEmpty()
    }

    def "test getExecutionItemsByEventId - should return rules filtered by allowed statuses"() {
        given: "rules with different statuses"
        def eventId = 1001L
        def rule1Id = 1L  // TEST - allowed
        def rule2Id = 2L  // OFFLINE - not allowed
        def rule3Id = 3L  // FULL - not allowed in TEST environment

        def relationEntities = [
                createRelationEntity(eventId, rule1Id, 1),
                createRelationEntity(eventId, rule2Id, 2),
                createRelationEntity(eventId, rule3Id, 3)
        ]

        def rule1Entity = createRuleEntity(rule1Id, RuleStatusEnum.TEST)
        def rule2Entity = createRuleEntity(rule2Id, RuleStatusEnum.OFFLINE)
        def rule3Entity = createRuleEntity(rule3Id, RuleStatusEnum.FULL)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId)

        then: "should filter by allowed statuses"
        1 * executionEventRelationMapper.selectList(_) >> relationEntities
        1 * ruleStatusConfig.getAllowedRuleStatuses() >> Collections.singleton(RuleStatusEnum.TEST)
        1 * ruleMapper.selectBatchIds([rule1Id, rule2Id, rule3Id]) >> [rule1Entity, rule2Entity, rule3Entity]
        1 * ruleContentMapper.selectBatchIds([rule1Id]) >> [createContentEntity(rule1Id)]

        items.size() == 1
        items[0].rule.id == rule1Id
        items[0].rule.ruleStatus == RuleStatusEnum.TEST
    }

    def "test getExecutionItemsByEventId - should return rule groups"() {
        given: "execution sequence with rule group"
        def eventId = 1001L
        def groupId = 100L
        def rule1Id = 1L
        def rule2Id = 2L

        def relationEntities = [
                createRelationEntityForGroup(eventId, groupId, 1)
        ]

        def ruleGroupEntity = new RuleGroupEntity()
        ruleGroupEntity.id = groupId

        def ruleGroupRule1 = new RuleGroupRuleRelationEntity()
        ruleGroupRule1.groupId = groupId
        ruleGroupRule1.ruleId = rule1Id
        ruleGroupRule1.abTestRatio = 50

        def ruleGroupRule2 = new RuleGroupRuleRelationEntity()
        ruleGroupRule2.groupId = groupId
        ruleGroupRule2.ruleId = rule2Id
        ruleGroupRule2.abTestRatio = 50

        def rule1Entity = createRuleEntity(rule1Id, RuleStatusEnum.AB_TEST)
        def rule2Entity = createRuleEntity(rule2Id, RuleStatusEnum.AB_TEST)

        def ruleGroup = new RuleGroup(groupId)
        ruleGroup.addRule(rule1Id, Rule.builder().id(rule1Id).ruleStatus(RuleStatusEnum.AB_TEST).build(), 50)
        ruleGroup.addRule(rule2Id, Rule.builder().id(rule2Id).ruleStatus(RuleStatusEnum.AB_TEST).build(), 50)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId)

        then: "should return rule group"
        1 * executionEventRelationMapper.selectList(_) >> relationEntities
        1 * ruleStatusConfig.getAllowedRuleStatuses() >> new HashSet<>([RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL])
        1 * ruleGroupMapper.selectBatchIds([groupId]) >> [ruleGroupEntity]
        1 * ruleGroupRuleRelationMapper.selectList(_) >> [ruleGroupRule1, ruleGroupRule2]
        1 * ruleGroupStructMapper.entityToModel(ruleGroupEntity, _) >> ruleGroup
        1 * ruleMapper.selectBatchIds([rule1Id, rule2Id]) >> [rule1Entity, rule2Entity]
        1 * ruleContentMapper.selectBatchIds([rule1Id, rule2Id]) >> [
                createContentEntity(rule1Id),
                createContentEntity(rule2Id)
        ]

        items.size() == 1
        items[0].type == ExecutionItemTypeEnum.RULE_GROUP
        items[0].ruleGroup.id == groupId
    }

    def "test getExecutionItemsByEventId - should maintain execution order"() {
        given: "execution sequence with specific order"
        def eventId = 1001L
        def rule1Id = 1L
        def rule2Id = 2L
        def rule3Id = 3L

        def relationEntities = [
                createRelationEntity(eventId, rule1Id, 1),
                createRelationEntity(eventId, rule2Id, 2),
                createRelationEntity(eventId, rule3Id, 3)
        ]

        def rule1Entity = createRuleEntity(rule1Id, RuleStatusEnum.TEST)
        def rule2Entity = createRuleEntity(rule2Id, RuleStatusEnum.TEST)
        def rule3Entity = createRuleEntity(rule3Id, RuleStatusEnum.TEST)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId)

        then: "should maintain execution order"
        1 * executionEventRelationMapper.selectList(_) >> relationEntities
        1 * ruleMapper.selectBatchIds([rule1Id, rule2Id, rule3Id]) >> [rule1Entity, rule2Entity, rule3Entity]
        1 * ruleContentMapper.selectBatchIds([rule1Id, rule2Id, rule3Id]) >> [
                createContentEntity(rule1Id),
                createContentEntity(rule2Id),
                createContentEntity(rule3Id)
        ]

        items.size() == 3
        items[0].rule.id == rule1Id
        items[1].rule.id == rule2Id
        items[2].rule.id == rule3Id
    }

    def "test getExecutionItemsByEventId - should skip null rule groups"() {
        given: "execution sequence with non-existent rule group"
        def eventId = 1001L
        def groupId = 999L

        def relationEntities = [
                createRelationEntityForGroup(eventId, groupId, 1)
        ]

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId)

        then: "should skip null rule group"
        1 * executionEventRelationMapper.selectList(_) >> relationEntities
        1 * ruleGroupMapper.selectBatchIds([groupId]) >> []

        items.isEmpty()
    }

    // ========== Helper methods ==========

    private static ExecutionEventRelationEntity createRelationEntity(Long eventId, Long itemId, Integer order) {
        def entity = new ExecutionEventRelationEntity()
        entity.eventId = eventId
        entity.itemId = itemId
        entity.itemType = ExecutionItemTypeEnum.RULE.getId()
        entity.executionOrder = order
        return entity
    }

    private static ExecutionEventRelationEntity createRelationEntityForGroup(Long eventId, Long groupId, Integer order) {
        def entity = new ExecutionEventRelationEntity()
        entity.eventId = eventId
        entity.itemId = groupId
        entity.itemType = ExecutionItemTypeEnum.RULE_GROUP.getId()
        entity.executionOrder = order
        return entity
    }

    private static RuleEntity createRuleEntity(Long ruleId, RuleStatusEnum status) {
        def entity = new RuleEntity()
        entity.id = ruleId
        entity.name = "Rule ${ruleId}"
        entity.contentType = ContentTypeEnum.EXPRESSION.getId()
        entity.ruleStatus = status.getId()
        entity.description = "Test rule ${ruleId}"
        return entity
    }

    private static RuleContentEntity createContentEntity(Long ruleId) {
        def entity = new RuleContentEntity()
        entity.id = ruleId
        entity.content = "test content for rule ${ruleId}"
        return entity
    }
}
