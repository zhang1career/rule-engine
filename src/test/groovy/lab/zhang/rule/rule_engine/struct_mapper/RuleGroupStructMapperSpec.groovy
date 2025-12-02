package lab.zhang.rule.rule_engine.struct_mapper

import lab.zhang.rule.rule_engine.entity.RuleGroupEntity
import lab.zhang.rule.rule_engine.entity.RuleGroupRuleRelationEntity
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO
import org.apache.commons.lang3.tuple.Pair
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.lang.Unroll

/**
 * RuleGroupStructMapper unit test
 *
 * @author Rongjin Zhang
 */
@SpringBootTest
@ActiveProfiles("test")
class RuleGroupStructMapperSpec extends Specification {

    @Autowired
    RuleGroupStructMapper ruleGroupStructMapper

    // ========== modelToDTO() tests ==========

    @Unroll
    def "test modelToDTO - should convert RuleGroup to RuleGroupDTO - groupId: #groupId"() {
        given: "a RuleGroup"
        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([:])
                .build()

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTO(ruleGroup)

        then: "should convert correctly with empty rules"
        dto != null
        dto.id == groupId
        dto.rules != null
        dto.rules.isEmpty()

        where:
        groupId << [10000001L, 10000002L, 10000003L]
    }

    def "test modelToDTO - should return null when RuleGroup is null"() {
        when: "convert null RuleGroup to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTO(null)

        then: "should return null"
        dto == null
    }

    def "test modelToDTO - should set empty rules map regardless of input rules"() {
        given: "a RuleGroup with rules"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([1L: Pair.of(rule1, 50), 2L: Pair.of(rule2, 50)])
                .build()

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTO(ruleGroup)

        then: "should have empty rules map"
        dto != null
        dto.id == 10000001L
        dto.rules != null
        dto.rules.isEmpty()
    }

    // ========== modelToDTOWithRules() tests ==========

    @Unroll
    def "test modelToDTOWithRules - should convert RuleGroup with rules - groupId: #groupId, rule1Id: #rule1Id, rule1Ratio: #rule1Ratio, rule2Id: #rule2Id, rule2Ratio: #rule2Ratio"() {
        given: "a RuleGroup with rules"
        def rule1 = Rule.builder()
                .id(rule1Id)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()
        def rule2 = Rule.builder()
                .id(rule2Id)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(groupId)
                .rules([(rule1Id): Pair.of(rule1, rule1Ratio), (rule2Id): Pair.of(rule2, rule2Ratio)])
                .build()

        and: "ratios map"
        Map<String, Integer> ratiosMap = new HashMap<>()
        ratiosMap.put(String.valueOf(groupId) + ":" + String.valueOf(rule1Id), rule1Ratio)
        ratiosMap.put(String.valueOf(groupId) + ":" + String.valueOf(rule2Id), rule2Ratio)

        when: "convert to RuleGroupDTO with rules"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, ratiosMap)

        then: "should convert correctly with rules"
        dto != null
        dto.id == groupId
        dto.rules != null
        dto.rules.size() == 2
        dto.rules[rule1Id] == rule1Ratio
        dto.rules[rule2Id] == rule2Ratio

        where:
        groupId    | rule1Id | rule1Ratio | rule2Id | rule2Ratio
        10000001L  | 1L      | 50         | 2L      | 50
        10000002L  | 3L      | 30         | 4L      | 70
        10000003L  | 5L      | 20         | 6L      | 40
    }

    def "test modelToDTOWithRules - should return null when RuleGroup is null"() {
        given: "null RuleGroup and ratios map"
        def ratiosMap = ["10000001:1": 50]

        when: "convert null RuleGroup to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(null, ratiosMap)

        then: "should return null"
        dto == null
    }

    def "test modelToDTOWithRules - should handle empty rules map when RuleGroup has no rules"() {
        given: "a RuleGroup without rules"
        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([:])
                .build()

        and: "empty ratios map"
        def ratiosMap = [:]

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, ratiosMap)

        then: "should have empty rules map"
        dto != null
        dto.id == 10000001L
        dto.rules != null
        dto.rules.isEmpty()
    }

    def "test modelToDTOWithRules - should handle null ratios map"() {
        given: "a RuleGroup with rules"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([1L: Pair.of(rule1, 50)])
                .build()

        when: "convert to RuleGroupDTO with null ratios map"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, null)

        then: "should set default ratio 0 for all rules when ratios map is null"
        dto != null
        dto.id == 10000001L
        dto.rules != null
        dto.rules.size() == 1
        dto.rules[1L] == 0
    }

    def "test modelToDTOWithRules - should set default ratio 0 when ratio not found in ratios map"() {
        given: "a RuleGroup with rules"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([1L: Pair.of(rule1, 50), 2L: Pair.of(rule2, 50)])
                .build()

        and: "ratios map with only one rule"
        def ratiosMap = [
                "10000001:1": 50
                // rule2 ratio is missing
        ]

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, ratiosMap)

        then: "should set default ratio 0 for missing rule"
        dto != null
        dto.id == 10000001L
        dto.rules != null
        dto.rules.size() == 2
        dto.rules[1L] == 50
        dto.rules[2L] == 0
    }

    def "test modelToDTOWithRules - should handle null Number values in ratios map"() {
        given: "a RuleGroup with one rule"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([1L: Pair.of(rule1, 50)])
                .build()

        and: "ratios map with null Integer value"
        def ratiosMap = [
                "10000001:1": null
        ]

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, ratiosMap)

        then: "should set default ratio 0 for null value"
        dto != null
        dto.rules != null
        dto.rules[1L] == 0
    }

    def "test modelToDTOWithRules - should handle multiple rules with same ratio"() {
        given: "a RuleGroup with multiple rules"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()
        def rule3 = Rule.builder()
                .id(3L)
                .name("Rule 3")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([
                        1L: Pair.of(rule1, 33),
                        2L: Pair.of(rule2, 33),
                        3L: Pair.of(rule3, 34)
                ])
                .build()

        and: "ratios map"
        def ratiosMap = [
                "10000001:1": 33,
                "10000001:2": 33,
                "10000001:3": 34
        ]

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, ratiosMap)

        then: "should convert all rules correctly"
        dto != null
        dto.id == 10000001L
        dto.rules != null
        dto.rules.size() == 3
        dto.rules[1L] == 33
        dto.rules[2L] == 33
        dto.rules[3L] == 34
    }

    // ========== entityToModel() tests ==========

    @Unroll
    def "test entityToModel - should convert RuleGroupEntity to RuleGroup - entityId: #entityId"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = entityId

        and: "empty relations map"
        def relationsMap = [:]

        when: "convert to RuleGroup"
        def ruleGroup = ruleGroupStructMapper.entityToModel(entity, relationsMap)

        then: "should convert correctly with empty rules"
        ruleGroup != null
        ruleGroup.id == entityId
        ruleGroup.rules != null
        ruleGroup.rules.isEmpty()

        where:
        entityId << [10000001L, 10000002L, 10000003L]
    }

    def "test entityToModel - should return null when entity is null"() {
        given: "null entity and relations map"
        def relationsMap = [:]

        when: "convert null entity to RuleGroup"
        def ruleGroup = ruleGroupStructMapper.entityToModel(null, relationsMap)

        then: "should return null"
        ruleGroup == null
    }

    def "test entityToModel - should populate rules map from relations map with null Rule objects"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = 10000001L

        and: "relations map with data"
        def relation1 = new RuleGroupRuleRelationEntity()
        relation1.groupId = 10000001L
        relation1.ruleId = 1L
        relation1.abTestRatio = 50

        def relationsMap = [
                10000001L: [relation1]
        ]

        when: "convert to RuleGroup"
        def ruleGroup = ruleGroupStructMapper.entityToModel(entity, relationsMap)

        then: "should populate rules map with rule IDs and ratios (Rule objects are null, loaded separately)"
        ruleGroup != null
        ruleGroup.id == 10000001L
        ruleGroup.rules != null
        ruleGroup.rules.size() == 1
        ruleGroup.rules.containsKey(1L)
        ruleGroup.rules.get(1L).left == null  // Rule object is null, loaded separately
        ruleGroup.rules.get(1L).right == 50   // Ratio is populated
        ruleGroup.getRuleIds().contains(1L)    // getRuleIds() works because rules map is populated
    }

    def "test entityToModel - should handle null relations map"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = 10000001L

        when: "convert to RuleGroup with null relations map"
        def ruleGroup = ruleGroupStructMapper.entityToModel(entity, null)

        then: "should convert correctly with empty rules"
        ruleGroup != null
        ruleGroup.id == 10000001L
        ruleGroup.rules != null
        ruleGroup.rules.isEmpty()
    }


    // ========== Integration tests ==========

    def "test modelToDTO then modelToDTOWithRules - should work correctly"() {
        given: "a RuleGroup"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.AB_TEST)
                .build()

        def ruleGroup = RuleGroup.builder()
                .id(10000001L)
                .rules([1L: Pair.of(rule1, 50)])
                .build()

        when: "convert to DTO without rules"
        def dto1 = ruleGroupStructMapper.modelToDTO(ruleGroup)

        and: "convert to DTO with rules"
        def ratiosMap = ["10000001:1": 50]
        def dto2 = ruleGroupStructMapper.modelToDTOWithRules(ruleGroup, ratiosMap)

        then: "should work correctly"
        dto1 != null
        dto1.id == 10000001L
        dto1.rules.isEmpty()

        dto2 != null
        dto2.id == 10000001L
        dto2.rules.size() == 1
        dto2.rules[1L] == 50
    }

    def "test entityToModel then modelToDTO - should work correctly"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = 10000001L

        when: "convert entity to model"
        def ruleGroup = ruleGroupStructMapper.entityToModel(entity, [:])

        and: "convert model to DTO"
        def dto = ruleGroupStructMapper.modelToDTO(ruleGroup)

        then: "should work correctly"
        ruleGroup != null
        ruleGroup.id == 10000001L
        ruleGroup.rules.isEmpty()

        dto != null
        dto.id == 10000001L
        dto.rules.isEmpty()
    }
}

