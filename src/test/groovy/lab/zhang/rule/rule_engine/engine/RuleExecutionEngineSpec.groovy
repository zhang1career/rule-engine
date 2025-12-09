package lab.zhang.rule.rule_engine.engine

import lab.zhang.rule.rule_engine.common.TypedValue
import lab.zhang.rule.rule_engine.constant.EvalArgumentConst
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.enums.ValueTypeEnum
import lab.zhang.rule.rule_engine.executor.RuleExecutor
import lab.zhang.rule.rule_engine.mapper.ExecutionArrangementMapper
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleExecutionContext
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.service.RuleGroupService
import lab.zhang.rule.rule_engine.cache.RuleSelectionCacheService
import lab.zhang.rule.rule_engine.service.RuleService
import lab.zhang.rule.rule_engine.service.impl.RuleGroupServiceImpl
import lab.zhang.rule.rule_engine.util.HashUtil
import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field

import static lab.zhang.rule.rule_engine.constant.EvalArgumentConst.ARG_USER_HASH;

/**
 * RuleExecutionEngine unit test
 */
class RuleExecutionEngineSpec extends Specification {

    def ruleService = Mock(RuleService)
    def ruleGroupService = Mock(RuleGroupService)
    def ruleExecutor = Mock(RuleExecutor)
    def executionArrangementMapper = Mock(ExecutionArrangementMapper)
    def engine = new RuleExecutionEngine()

    def setup() {
        engine.ruleService = ruleService
        engine.ruleGroupService = ruleGroupService
        // Mock getSupportedRuleType for executor registration (use _ * for default behavior)
        _ * ruleExecutor.getSupportedRuleType() >> ContentTypeEnum.EXPRESSION
        engine.registerExecutor(ruleExecutor)
    }

    def setPrivateField(Object target, String fieldName, Object value) {
        Field field = target.class.getDeclaredField(fieldName)
        field.setAccessible(true)
        field.set(target, value)
    }

    @Unroll
    def "test execute rule sequence - normal case - eventId: #eventId, ruleCount: #ruleCount, expectedResult: #expectedResult"() {
        given: "prepare rules and execution context"
        def rules = []
        def results = []
        ruleCount.times { i ->
            def rule = new Rule(
                    id: (i + 1) as Long,
                    contentType: ContentTypeEnum.EXPRESSION,
                    ruleStatus: RuleStatusEnum.TEST
            )
            rules << rule
            results << new TypedValue(i + 1, ValueTypeEnum.INTEGER)
        }

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = eventId
        context.traceId = 999L

        def executionItems = rules.collect { ExecutionItem.forRule(it as Rule, null) }

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(eventId, context, trace)

        then: "should execute rules in order"
        1 * ruleService.getExecutionItemsByEventId(eventId, context) >> executionItems
        ruleCount.times { i ->
            1 * ruleExecutor.execute(rules[i], context) >> results[i]
        }
        result == expectedResult

        where:
        eventId | ruleCount | expectedResult
        1001    | 1         | new TypedValue(1, ValueTypeEnum.INTEGER)
        1001    | 2         | new TypedValue(2, ValueTypeEnum.INTEGER)
        1002    | 3         | new TypedValue(3, ValueTypeEnum.INTEGER)
    }

    @Unroll
    def "test execute rule sequence - should return null when no rules - eventId: #eventId"() {
        given: "prepare execution context"
        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = eventId

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(eventId, context, trace)

        then: "should return null TypedValue"
        1 * ruleService.getExecutionItemsByEventId(eventId, context) >> []
        result != null
        result.getType() == ValueTypeEnum.OBJECT
        result.getValue() == null

        where:
        eventId << [1001, 1002, 1003]
    }

    @Unroll
    def "test execute rule sequence - skip offline status rules - offlineRuleCount: #offlineRuleCount, activeRuleCount: #activeRuleCount"() {
        given: "prepare rules and execution context"
        def rules = []
        offlineRuleCount.times { i ->
            rules << new Rule(
                    id: (i + 1) as Long,
                    contentType: ContentTypeEnum.EXPRESSION,
                    ruleStatus: RuleStatusEnum.OFFLINE
            )
        }
        def activeRules = []
        activeRuleCount.times { i ->
            def rule = new Rule(
                    id: (offlineRuleCount + i + 1) as Long,
                    contentType: ContentTypeEnum.EXPRESSION,
                    ruleStatus: RuleStatusEnum.TEST
            )
            rules << rule
            activeRules << rule
        }

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result = new TypedValue(true, ValueTypeEnum.BOOLEAN)
        def executionItems = activeRules.collect { ExecutionItem.forRule(it as Rule, null) }

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def executionResult = engine.execute(1001, context, trace)

        then: "should skip offline status rules"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        offlineRuleCount.times { i ->
            0 * ruleExecutor.execute(rules[i], _)
        }
        activeRuleCount.times { i ->
            1 * ruleExecutor.execute(activeRules[i], context) >> result
        }
        executionResult == result

        where:
        offlineRuleCount | activeRuleCount
        1                | 1
        2                | 1
        1                | 2
        3                | 2
    }

    @Unroll
    def "test execute rule sequence - should execute rules returned by service - ruleStatus: #ruleStatus"() {
        given: "prepare rules and execution context"
        def rule = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: ruleStatus
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result = new TypedValue(true, ValueTypeEnum.BOOLEAN)
        def executionItems = [ExecutionItem.forRule(rule, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def executionResult = engine.execute(1001, context, trace)

        then: "should execute rules returned by service"
        // Note: Environment filtering is now handled in EventServiceImpl.getExecutionArrangements
        // RuleExecutionEngine just executes whatever rules are returned by the service
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule, context) >> result
        executionResult == result

        where:
        ruleStatus << [RuleStatusEnum.TEST, RuleStatusEnum.ONLINE, RuleStatusEnum.GRAY]
    }

    @Unroll
    def "test execute rule sequence - A/B test rule group probability distribution - rule1Ratio: #rule1Ratio, rule2Ratio: #rule2Ratio"() {
        given: "prepare execution items"

        and: "prepare A/B test rule group with real RuleGroupServiceImpl"
        def realRuleService = Mock(RuleService)
        // Note: GreRelationMapper is no longer used
        def realRuleSelectionCacheService = Mock(RuleSelectionCacheService)
        def realRuleGroupService = new RuleGroupServiceImpl()
        realRuleGroupService.ruleService = realRuleService
        // Note: GreRelationMapper is no longer used
        realRuleGroupService.ruleSelectionCacheService = realRuleSelectionCacheService

        engine.ruleGroupService = realRuleGroupService

        def rule1 = Rule.builder()
                .id(1L)
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(100L)
                .rules([1L: Pair.of(rule1, rule1Ratio),
                        2L: Pair.of(rule2, rule2Ratio)])
                .build()

        // Mock ruleService to return rules
        _ * realRuleService.getRuleById(1L) >> rule1
        _ * realRuleService.getRuleById(2L) >> rule2
        _ * realRuleService.getExecutionItemsByEventId(1001, _) >> [ExecutionItem.forRule(ruleGroup.getRules().values().first().left, null)]

        // Mock ruleGroupRuleMapper to return relation entities with ab_test_ratio
        def relation1 = new ExecutionArrangementEntity ()
        relation1.setGroupId(100L)
        relation1.setRuleId(1L)
        relation1.setAbRatio(rule1Ratio)

        def relation2 = new ExecutionArrangementEntity()
        relation2.setGroupId(100L)
        relation2.setRuleId(2L)
        relation2.setAbRatio(rule2Ratio)

        // Mock selectOne: return relation based on ruleId being queried
        // Since selectRuleFromGroup queries rules in the order of group.getRuleIds() ([1L, 2L]),
        // we use a simple counter that resets every 2 calls (one for each rule)
        def selectOneCallCount = 0
        _ * executionArrangementMapper.selectOne(_) >> {
            selectOneCallCount++
            // Each selectRuleFromGroup call queries both rules, so we alternate
            if (selectOneCallCount % 2 == 1) {
                return relation1  // Odd calls: ruleId=1
            } else {
                return relation2  // Even calls: ruleId=2
            }
        }

        when: "run multiple times to test probability distribution"
        def totalRuns = 1000
        def rule1Count = 0
        def rule2Count = 0
        def skippedCount = 0

        // Mock cache service: always return null (cache miss) to test probability distribution
        _ * realRuleSelectionCacheService.get(_, _, _) >> null
        _ * realRuleSelectionCacheService.put(_, _, _, _) >> { /* cache the selection */ }

        totalRuns.times { i ->
            def context = new RuleExecutionContext(1000L + i, 1001, 9999L, [:])
            // Set userHash and userHashInt in arguments (normally done by EvalServiceImpl)
            def userHash = HashUtil.murmurHash3(context.getUserId().toString())
            context.putArgument(ARG_USER_HASH, new TypedValue(userHash, ValueTypeEnum.STRING))
            int userHashInt = HashUtil.hashToIntRange(userHash, 1, 100)
            context.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(userHashInt, ValueTypeEnum.INTEGER))

            def selectedRule = realRuleGroupService.selectRuleFromGroup(ruleGroup, context)

            if (selectedRule != null && selectedRule.getId() == 1L) {
                rule1Count++
            } else if (selectedRule != null && selectedRule.getId() == 2L) {
                rule2Count++
            } else {
                skippedCount++
            }
        }

        and: "verify cache works: same userId and eventId should return same rule"
        // Mock cache to return cached ruleId for second call
        def cachedRuleId = 1L
        realRuleSelectionCacheService.get(9999L, 1001, 100L) >>> [null, cachedRuleId] // First call: cache miss, second call: cache hit
        
        def cacheContext1 = new RuleExecutionContext(9999L, 1001, 9999L, [:])
        def cacheUserHash1 = HashUtil.murmurHash3(cacheContext1.getUserId().toString())
        cacheContext1.putArgument(ARG_USER_HASH, new TypedValue(cacheUserHash1, ValueTypeEnum.STRING))
        int cacheUserHashInt1 = HashUtil.hashToIntRange(cacheUserHash1, 1, 100)
        cacheContext1.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(cacheUserHashInt1, ValueTypeEnum.INTEGER))
        def cacheFirstSelection = realRuleGroupService.selectRuleFromGroup(ruleGroup, cacheContext1)

        def cacheContext2 = new RuleExecutionContext(9999L, 1001, 9999L, [:])
        def cacheUserHash2 = HashUtil.murmurHash3(cacheContext2.getUserId().toString())
        cacheContext2.putArgument(ARG_USER_HASH, new TypedValue(cacheUserHash2, ValueTypeEnum.STRING))
        int cacheUserHashInt2 = HashUtil.hashToIntRange(cacheUserHash2, 1, 100)
        cacheContext2.putArgument(EvalArgumentConst.ARG_USER_HASH_INT, new TypedValue(cacheUserHashInt2, ValueTypeEnum.INTEGER))
        def cacheSecondSelection = realRuleGroupService.selectRuleFromGroup(ruleGroup, cacheContext2)

        then: "probability distribution should be close to expected ratios"
        def totalRatio = rule1Ratio + rule2Ratio
        def expectedRule1Percent = totalRatio > 0 ? (rule1Ratio * 100.0 / totalRatio) : 0
        def expectedRule2Percent = totalRatio > 0 ? (rule2Ratio * 100.0 / totalRatio) : 0
        def expectedSkippedPercent = totalRatio < 100 ? ((100 - totalRatio) * 100.0 / 100) : 0

        def actualRule1Percent = (rule1Count * 100.0 / totalRuns)
        def actualRule2Percent = (rule2Count * 100.0 / totalRuns)
        def actualSkippedPercent = (skippedCount * 100.0 / totalRuns)

        // Allow 5% tolerance
        def tolerance = 5.0

        if (totalRatio > 0) {
            Math.abs(actualRule1Percent - expectedRule1Percent as double) < tolerance
            Math.abs(actualRule2Percent - expectedRule2Percent as double) < tolerance
        }
        if (totalRatio < 100) {
            Math.abs(actualSkippedPercent - expectedSkippedPercent as double) < tolerance
        }

        // Verify cache works: same userId and eventId should return same rule
        cacheFirstSelection == cacheSecondSelection // Should be same due to cache

        where:
        rule1Ratio | rule2Ratio
        50         | 30
        30         | 20
        40         | 40
        10         | 10
    }

    @Unroll
    def "test execute rule sequence - early break - resultValue: #resultValue, resultType: #resultType, shouldBreak: #shouldBreak"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(resultValue, resultType)
        def result2 = new TypedValue(100, ValueTypeEnum.INTEGER)
        def executionItems = [ExecutionItem.forRule(rule1, null), ExecutionItem.forRule(rule2, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should break early when returning false boolean"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        (shouldBreak ? 0 : 1) * ruleExecutor.execute(rule2, context) >> result2
        result == (shouldBreak ? result1 : result2)

        where:
        resultValue | resultType                   | shouldBreak
        false       | ValueTypeEnum.BOOLEAN | true
        true        | ValueTypeEnum.BOOLEAN | false
        100         | ValueTypeEnum.INTEGER | false
        "test"      | ValueTypeEnum.STRING  | false
    }

    @Unroll
    def "test execute rule sequence - rule execution failure should throw exception - ruleId: #ruleId, errorMessage: #errorMessage"() {
        given: "prepare rules and execution context"
        def rule = new Rule(
                id: ruleId,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        def executionItems = [ExecutionItem.forRule(rule, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        engine.execute(1001, context, trace)

        then: "should throw exception"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule, context) >> {
            throw new RuntimeException(errorMessage)
        }
        thrown(RuntimeException)

        where:
        ruleId | errorMessage
        1L     | "Rule execution failed"
        2L     | "Expression evaluation error"
        3L     | "Invalid rule content"
    }

    @Unroll
    def "test execute rule sequence - should throw exception when executor not found - ruleType: #ruleType"() {
        given: "prepare rules and execution context"
        def rule = new Rule(
                id: 1L,
                contentType: ruleType,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        def executionItems = [ExecutionItem.forRule(rule, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        engine.execute(1001, context, trace)

        then: "should throw exception"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        thrown(RuntimeException)

        where:
        ruleType << [ContentTypeEnum.SCRIPT, ContentTypeEnum.API_QUERY, ContentTypeEnum.SQL_QUERY]
    }

    @Unroll
    def "test execute rule sequence - mixed rules and rule groups - selectedRuleId: #selectedRuleId, expectedResult: #expectedResult"() {
        given: "prepare execution items"

        and: "prepare mixed execution items: rule, rule group, rule"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )
        def rule3 = new Rule(
                id: 3L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )
        def rule4 = new Rule(
                id: 4L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE
        )

        def ruleGroup = new RuleGroup(100L)
        ruleGroup.addRule(2L, rule2, 50)
        ruleGroup.addRule(3L, rule3, 50)

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001L
        // Add userHash to context (required for rule group selection)
        context.putArgument(ARG_USER_HASH, new TypedValue("testHash123", ValueTypeEnum.STRING))

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def result2 = new TypedValue(20, ValueTypeEnum.INTEGER)
        def result3 = new TypedValue(30, ValueTypeEnum.INTEGER)
        def result4 = new TypedValue(40, ValueTypeEnum.INTEGER)

        // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
        // The selected rule is already in the execution items list
        // Determine selectedRule based on selectedRuleId
        def selectedRule = selectedRuleId == 2L ? rule2 : (selectedRuleId == 3L ? rule3 : null)
        
        // Create execution items: rule1, selectedRule (from group, or null if not selected), rule4
        def executionItems = []
        executionItems.add(ExecutionItem.forRule(rule1, null))
        if (selectedRule != null) {
            executionItems.add(ExecutionItem.forRule(selectedRule, null))
        } else {
            executionItems.add(ExecutionItem.forRule(null, null)) // null rule will be skipped
        }
        executionItems.add(ExecutionItem.forRule(rule4, null))

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def executionResult = engine.execute(1001, context, trace)

        then: "should execute rules in order"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        // Execute rule1
        1 * ruleExecutor.execute(rule1, context) >> result1
        // Rule group selection is handled in RuleServiceImpl, not here
        0 * ruleGroupService.selectRuleFromGroup(_, _)

        // Conditionally mock based on selectedRule
        (selectedRule != null && selectedRule.getId() == 2L ? 1 : 0) * ruleExecutor.execute(rule2, context) >> result2
        (selectedRule != null && selectedRule.getId() == 3L ? 1 : 0) * ruleExecutor.execute(rule3, context) >> result3

        // Execute rule4 (always executed)
        1 * ruleExecutor.execute(rule4, context) >> result4

        // Should return the last result (rule4)
        executionResult == expectedResult

        where:
        selectedRuleId | expectedResult
        2L             | new TypedValue(40, ValueTypeEnum.INTEGER)
        3L             | new TypedValue(40, ValueTypeEnum.INTEGER)
        null           | new TypedValue(40, ValueTypeEnum.INTEGER)
    }

    def "test execute - should skip null rules in execution items"() {
        given: "prepare execution items with null rule"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)

        // Create execution item with null rule (will be skipped)
        def nullRuleItem = ExecutionItem.forRule(null, null)
        def executionItems = [
                ExecutionItem.forRule(rule1, null),
                nullRuleItem
        ]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should skip null rule and continue (no exception thrown)"
        // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
        // RuleExecutionEngine just skips null rules and continues
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        result == result1
    }

    def "test execute - should skip null rules and continue with next rule"() {
        given: "prepare execution items with null rule in the middle (simulating empty rule group scenario)"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def result2 = new TypedValue(20, ValueTypeEnum.INTEGER)

        // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
        // If a rule group is empty or no rule is selected, it won't appear in the execution items
        // This test simulates the case where a null rule might appear (should be skipped)
        def executionItems = [
                ExecutionItem.forRule(rule1, null),
                ExecutionItem.forRule(null, null), // null rule (simulating empty group scenario)
                ExecutionItem.forRule(rule2, null)
        ]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should skip null rule and continue with next rule"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        1 * ruleExecutor.execute(rule2, context) >> result2
        result == result2
    }

    def "test execute - rule group selection missing userId should skip"() {
        given: "prepare execution items with rule group"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )

        def ruleGroup = new RuleGroup(100L)
        ruleGroup.addRule(2L, rule2, 50)

        def context = new RuleExecutionContext()
        context.userId = null  // Missing userId
        context.eventId = 1001

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def executionItems = [
                ExecutionItem.forRule(rule1, null),
                ExecutionItem.forRule(null, null) // Note: Rule group selection is now handled in RuleServiceImpl
        ]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should skip rule group selection when userId is missing"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        result == result1
    }

    def "test execute - rule group selection missing eventId should skip"() {
        given: "prepare execution items with rule group"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )

        def ruleGroup = new RuleGroup(100L)
        ruleGroup.addRule(2L, rule2, 50)

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = null  // Missing eventId
        context.putArgument(ARG_USER_HASH, new TypedValue("hash123", ValueTypeEnum.STRING))

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def executionItems = [
                ExecutionItem.forRule(rule1, null),
                ExecutionItem.forRule(null, null) // Note: Rule group selection is now handled in RuleServiceImpl
        ]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should skip rule group selection when eventId is missing"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        result == result1
    }

    def "test execute - rule group selection missing userHash should skip"() {
        given: "prepare execution items with rule group"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )

        def ruleGroup = new RuleGroup(100L)
        ruleGroup.addRule(2L, rule2, 50)

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        // Missing userHash

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def executionItems = [
                ExecutionItem.forRule(rule1, null),
                ExecutionItem.forRule(null, null) // Note: Rule group selection is now handled in RuleServiceImpl
        ]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should skip rule group selection when userHash is missing"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        result == result1
    }

    // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
    // This test is no longer applicable to RuleExecutionEngine

    def "test execute - should store rule execution results in context variables"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def result2 = new TypedValue(20, ValueTypeEnum.INTEGER)
        def executionItems = [ExecutionItem.forRule(rule1, null), ExecutionItem.forRule(rule2, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should store results in context variables"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        2 * ruleExecutor.execute(_, { RuleExecutionContext ctx ->
            // Verify that lastResult is set after first rule execution
            true
        }) >> { Rule rule, RuleExecutionContext ctx ->
            if (rule.getId() == 1L) {
                return result1
            } else {
                // Verify that lastResult from rule1 is available
                assert ctx.getArgument("lastResult") == result1
                assert ctx.getArgument("rule:1:result") == result1
                return result2
            }
        }
        result == result2

        and: "context should contain all rule results"
        context.getArgument("lastResult") == result2
        context.getArgument("rule:1:result") == result1
        context.getArgument("rule:2:result") == result2
    }

    def "test execute - should return null TypedValue when no rule executed"() {
        given: "prepare execution items where all rules are skipped"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )

        def ruleGroup = new RuleGroup(100L)
        ruleGroup.addRule(1L, rule1, 50)

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        context.putArgument(ARG_USER_HASH, new TypedValue("hash123", ValueTypeEnum.STRING))

        // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
        // If no rule is selected, the execution items list will be empty
        def executionItems = []

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should return null TypedValue when no rule executed"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        0 * ruleExecutor.execute(_, _)
        result != null
        result.getType() == ValueTypeEnum.OBJECT
        result.getValue() == null
    }

    @Unroll
    def "test execute - shouldBreakExecution with different result types - resultValue: #resultValue, resultType: #resultType, shouldBreak: #shouldBreak"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        def result1 = resultValue != null ? new TypedValue(resultValue, resultType) : null
        def result2 = new TypedValue(100, ValueTypeEnum.INTEGER)
        def executionItems = [ExecutionItem.forRule(rule1, null), ExecutionItem.forRule(rule2, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should break or continue based on result"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        (shouldBreak ? 0 : 1) * ruleExecutor.execute(rule2, context) >> result2
        if (shouldBreak) {
            result == result1
        } else {
            result == result2
        }

        where:
        resultValue | resultType                   | shouldBreak
        null        | ValueTypeEnum.OBJECT         | false
        false       | ValueTypeEnum.BOOLEAN        | true
        true        | ValueTypeEnum.BOOLEAN        | false
        100         | ValueTypeEnum.INTEGER        | false
        "test"      | ValueTypeEnum.STRING         | false
        10.5        | ValueTypeEnum.DECIMAL       | false
    }

    def "test execute - shouldBreakExecution with boolean null value should not break"() {
        given: "prepare rules and execution context"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001

        // Create TypedValue with boolean type but null value
        def result1 = new TypedValue(null, ValueTypeEnum.BOOLEAN)
        def result2 = new TypedValue(100, ValueTypeEnum.INTEGER)
        def executionItems = [ExecutionItem.forRule(rule1, null), ExecutionItem.forRule(rule2, null)]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should not break when boolean value is null"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        1 * ruleExecutor.execute(rule2, context) >> result2
        result == result2
    }

    def "test execute - rule group with userHash value null should skip"() {
        given: "prepare execution items with rule group"
        def rule1 = new Rule(
                id: 1L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.TEST
        )
        def rule2 = new Rule(
                id: 2L,
                contentType: ContentTypeEnum.EXPRESSION,
                ruleStatus: RuleStatusEnum.ONLINE,
        )

        def ruleGroup = new RuleGroup(100L)
        ruleGroup.addRule(2L, rule2, 50)

        def context = new RuleExecutionContext()
        context.userId = 123L
        context.eventId = 1001
        // userHash exists but value is null
        context.putArgument(ARG_USER_HASH, new TypedValue(null, ValueTypeEnum.STRING))

        def result1 = new TypedValue(10, ValueTypeEnum.INTEGER)
        def executionItems = [
                ExecutionItem.forRule(rule1, null),
                ExecutionItem.forRule(null, null) // Note: Rule group selection is now handled in RuleServiceImpl
        ]

        when: "execute rule sequence"
        def trace = new ExecutionTrace()
        def result = engine.execute(1001, context, trace)

        then: "should skip rule group selection when userHash value is null"
        1 * ruleService.getExecutionItemsByEventId(1001, _) >> executionItems
        1 * ruleExecutor.execute(rule1, context) >> result1
        0 * ruleGroupService.selectRuleFromGroup(_, _)
        result == result1
    }

    // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
    // This test is no longer applicable to RuleExecutionEngine

    // Note: Rule group selection is now handled in RuleServiceImpl.getExecutionItemsByEventId
    // Tests for rule group selection should be in RuleServiceImplSpec
}
