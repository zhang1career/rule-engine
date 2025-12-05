package lab.zhang.rule.rule_engine.service.impl


import lab.zhang.rule.rule_engine.entity.*
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.model.ExecutionArrangement
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst
import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.executor.impl.ApiQueryRuleExecutor
import lab.zhang.rule.rule_engine.executor.impl.ExpressionRuleExecutor
import lab.zhang.rule.rule_engine.executor.impl.ScriptRuleExecutor
import lab.zhang.rule.rule_engine.executor.impl.SqlQueryRuleExecutor
import lab.zhang.rule.rule_engine.mapper.*
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.service.RuleGroupService
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper
import spock.lang.Specification
import spock.lang.Unroll


/**
 * RuleService unit test
 *
 * @author Rongjin Zhang
 */
class RuleServiceImplSpec extends Specification {

    def ruleMapper = Mock(RuleMapper)
    def ruleContentMapper = Mock(RuleContentMapper)
    def executionEventRelationMapper = Mock(ExecutionArrangementMapper)
    def ruleStructMapper = Mock(RuleStructMapper)
    def ruleService = new RuleServiceImpl()
    def ruleGroupService = Mock(RuleGroupService)
    def ruleSelectionCacheService = Mock(lab.zhang.rule.rule_engine.service.RuleSelectionCacheService)
    def eventService = Mock(lab.zhang.rule.rule_engine.service.EventService)

    def setup() {
        ruleService.ruleMapper = ruleMapper
        ruleService.ruleContentMapper = ruleContentMapper
        ruleService.executionArrangementMapper = executionEventRelationMapper
        ruleService.ruleStructMapper = ruleStructMapper
        ruleService.ruleGroupService = ruleGroupService
        ruleService.ruleSelectionCacheService = ruleSelectionCacheService
        ruleService.eventService = eventService

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
                    .description(entity.description != null ? entity.description : "")
                    .content("")
                    .build()
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
        def ruleEntity2 = createRuleEntity(2L, RuleStatusEnum.ONLINE)
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
            if (fromStatus != RuleStatusEnum.ONLINE && toStatus == RuleStatusEnum.ONLINE) {
                // Transition to ONLINE - create rule group for each event
                // Mock executionEventRelationMapper to return event relations
                1 * executionEventRelationMapper.selectList(_) >> {
                    def relation = new ExecutionArrangementEntity()
                    relation.setEventId(1001)
                    relation.setRuleId(ruleId)
                    relation.setGroupId(0L)
                    relation.setExeOrder(0)
                    relation.setAbRatio(0)
                    return [relation]
                }
                // createRuleGroup now requires eventId parameter
                1 * ruleGroupService.createRuleGroup(_, 1001) >> {
                    def group = new RuleGroup(10000001L)
                    return group
                }
            } else if (fromStatus == RuleStatusEnum.ONLINE && toStatus == RuleStatusEnum.OFFLINE) {
                // Transition from ONLINE to OFFLINE
                // First, query table x to find all groups this rule belongs to
                1 * executionEventRelationMapper.selectList(_) >> {
                    // Return a relation entity to simulate the rule being in a group
                    def relation = new ExecutionArrangementEntity()
                    relation.setGroupId(10000001L)
                    relation.setRuleId(ruleId)
                    relation.setEventId(1001)
                    relation.setExeOrder(0)
                    relation.setAbRatio(50)
                    return [relation]
                }
                // Delete the rule group
                1 * ruleGroupService.deleteRuleGroup(10000001L)
                // Delete event-rule relations
                1 * executionEventRelationMapper.delete(_) >> 1
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
        3L     | RuleStatusEnum.GRAY    | RuleStatusEnum.ONLINE
        4L     | RuleStatusEnum.ONLINE  | RuleStatusEnum.OFFLINE
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
        RuleStatusEnum.OFFLINE | RuleStatusEnum.ONLINE
        RuleStatusEnum.TEST    | RuleStatusEnum.ONLINE
        RuleStatusEnum.GRAY    | RuleStatusEnum.TEST
        RuleStatusEnum.ONLINE  | RuleStatusEnum.TEST
        RuleStatusEnum.ONLINE  | RuleStatusEnum.GRAY
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
        status << [RuleStatusEnum.TEST, RuleStatusEnum.GRAY, RuleStatusEnum.ONLINE]
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


    // ========== getExecutionItemsByEventId() tests ==========

    def "test getExecutionItemsByEventId - should return empty list when no relations exist"() {
        given: "eventId with no relations"
        def eventId = 1001
        def context = createExecutionContext(12345L, eventId, 50)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId, context)

        then: "should return empty list"
        // Note: Now uses eventService.getExecutionArrangements instead of executionEventRelationMapper.selectList
        1 * eventService.getExecutionArrangements(eventId) >> []
        items != null
        items.isEmpty()
    }

    def "test getExecutionItemsByEventId - should return rules filtered by allowed statuses"() {
        given: "rules with different statuses (already filtered by eventService)"
        def eventId = 1001
        def rule1Id = 1L  // TEST - allowed (returned by eventService)
        def rule2Id = 2L  // OFFLINE - not allowed (filtered out by eventService)
        def rule3Id = 3L  // FULL - not allowed (filtered out by eventService)

        // eventService.getExecutionArrangements already filters by allowed statuses
        def arrangements = [
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(rule1Id)
                        .groupId(0L)
                        .exeOrder(0)
                        .abRatio(0)
                        .build()
        ]

        def rule1Entity = createRuleEntity(rule1Id, RuleStatusEnum.TEST)

        def context = createExecutionContext(12345L, eventId, 50)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId, context)

        then: "should return only allowed rules"
        1 * eventService.getExecutionArrangements(eventId) >> arrangements
        1 * ruleMapper.selectBatchIds([rule1Id]) >> [rule1Entity]
        1 * ruleContentMapper.selectBatchIds([rule1Id]) >> [createContentEntity(rule1Id)]

        items.size() == 1
        items[0].rule.id == rule1Id
        items[0].rule.ruleStatus == RuleStatusEnum.TEST
    }

    def "test getExecutionItemsByEventId - should return rules from groups"() {
        given: "execution sequence with rule group"
        def eventId = 1001
        def groupId = 10000001L
        def rule1Id = 1L
        def rule2Id = 2L

        // Create arrangements: rule1 and rule2 in the same group
        // Note: exeOrder must be consecutive starting from 0 (0, 1, 2, ...)
        def arrangements = [
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(rule1Id)
                        .groupId(groupId)
                        .exeOrder(0)
                        .abRatio(50)
                        .build(),
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(rule2Id)
                        .groupId(groupId)
                        .exeOrder(1)
                        .abRatio(50)
                        .build()
        ]

        def rule1Entity = createRuleEntity(rule1Id, RuleStatusEnum.ONLINE)
        def rule2Entity = createRuleEntity(rule2Id, RuleStatusEnum.ONLINE)

        def context = createExecutionContext(12345L, eventId, 30) // userHashInt = 30, should select rule1 (0-50)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId, context)

        then: "should return selected rule from group"
        1 * eventService.getExecutionArrangements(eventId) >> arrangements
        // Mock cache service (no cached value, will select based on userHashInt)
        1 * ruleSelectionCacheService.get(12345L, eventId, groupId) >> null
        1 * ruleSelectionCacheService.put(12345L, eventId, groupId, rule1Id)
        // Mock ruleMapper - should be called with [rule1Id] (only selected rule)
        // Note: finalRuleIds only contains rule1Id because rule2Id is not selected
        1 * ruleMapper.selectBatchIds(_) >> [rule1Entity]
        // Mock ruleContentMapper - should be called with [rule1Id] (foundRuleIds from ruleMapper result)
        1 * ruleContentMapper.selectBatchIds(_) >> [createContentEntity(rule1Id)]
        // Mock ruleStructMapper - ensure it converts rule1Entity correctly
        // The setup() method already has a default mock using _ * ruleStructMapper.entityToModel(_ as RuleEntity)
        // The setup() mock should handle the conversion

        items.size() == 1
        items[0].rule.id == rule1Id // userHashInt=30 falls in rule1's range (0-50)
    }

    def "test getExecutionItemsByEventId - should maintain execution order"() {
        given: "execution sequence with specific order"
        def eventId = 1001
        def rule1Id = 1L
        def rule2Id = 2L
        def rule3Id = 3L

        def arrangements = [
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(rule1Id)
                        .groupId(0L)
                        .exeOrder(0)
                        .abRatio(0)
                        .build(),
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(rule2Id)
                        .groupId(0L)
                        .exeOrder(1)
                        .abRatio(0)
                        .build(),
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(rule3Id)
                        .groupId(0L)
                        .exeOrder(2)
                        .abRatio(0)
                        .build()
        ]

        def rule1Entity = createRuleEntity(rule1Id, RuleStatusEnum.TEST)
        def rule2Entity = createRuleEntity(rule2Id, RuleStatusEnum.TEST)
        def rule3Entity = createRuleEntity(rule3Id, RuleStatusEnum.TEST)

        def context = createExecutionContext(12345L, eventId, 50)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId, context)

        then: "should maintain execution order"
        1 * eventService.getExecutionArrangements(eventId) >> arrangements
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

    def "test getExecutionItemsByEventId - should handle empty groups"() {
        given: "execution sequence with rule in group where userHashInt exceeds total ratio"
        def eventId = 1001
        def groupId = 999L
        def ruleId = 1L

        def arrangements = [
                ExecutionArrangement.builder()
                        .eventId(eventId)
                        .ruleId(ruleId)
                        .groupId(groupId)
                        .exeOrder(0)
                        .abRatio(50)
                        .build()
        ]

        // userHashInt = 100, which exceeds totalRatio (50), so no rule will be selected
        def context = createExecutionContext(12345L, eventId, 100)

        when: "get execution items"
        def items = ruleService.getExecutionItemsByEventId(eventId, context)

        then: "should throw exception when rule selection fails (userHashInt > totalRatio)"
        1 * eventService.getExecutionArrangements(eventId) >> arrangements
        1 * ruleSelectionCacheService.get(12345L, eventId, groupId) >> null
        // selectRuleFromGroupByRatio will return null because userHashInt (100) > totalRatio (50)
        // This will cause IllegalStateException: "Failed to select rule from group"
        thrown(IllegalStateException)
    }

    // ========== Helper methods ==========

    private static ExecutionArrangementEntity createRelationEntity(Long eventId, Long ruleId, Long groupId, Integer exeOrder, Integer abRatio) {
        def entity = new ExecutionArrangementEntity()
        entity.eventId = eventId != null ? eventId.intValue() : null
        entity.ruleId = ruleId
        entity.groupId = groupId != null ? groupId : 0L
        entity.exeOrder = exeOrder != null ? exeOrder : 0
        entity.abRatio = abRatio != null ? abRatio : 0
        return entity
    }

    private static RuleExecutionContext createExecutionContext(Long userId, Integer eventId, Integer userHashInt) {
        def context = new RuleExecutionContext(userId, eventId, null, [:])
        if (userHashInt != null) {
            context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(userHashInt, ValueTypeEnum.INTEGER))
        }
        return context
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
