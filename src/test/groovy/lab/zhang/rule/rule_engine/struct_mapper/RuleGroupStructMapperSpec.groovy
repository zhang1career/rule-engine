package lab.zhang.rule.rule_engine.struct_mapper

import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.model.RuleGroup
import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
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
        dto.ruleRatios != null
        dto.ruleRatios.isEmpty()

        where:
        groupId << [10000001L, 10000002L, 10000003L]
    }

    def "test modelToDTO - should return null when RuleGroup is null"() {
        when: "convert null RuleGroup to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTO(null)

        then: "should return null"
        dto == null
    }

    def "test modelToDTO - should extract rule ratios from rules map"() {
        given: "a RuleGroup with rules"
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
                .id(10000001L)
                .rules([1L: Pair.of(rule1, 50), 2L: Pair.of(rule2, 30)])
                .build()

        when: "convert to RuleGroupDTO"
        def dto = ruleGroupStructMapper.modelToDTO(ruleGroup)

        then: "should extract rule ratios from rules map"
        dto != null
        dto.id == 10000001L
        dto.ruleRatios != null
        dto.ruleRatios.size() == 2
        dto.ruleRatios[1L] == 50
        dto.ruleRatios[2L] == 30
    }


    // ========== entityToModel() tests ==========

    @Unroll
    def "test entityToModel - should convert RuleGroupEntity to RuleGroup - entityId: #entityId"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = entityId

        and: "empty entity list"
        def entityList = []

        when: "convert to RuleGroup"
        def ruleGroup = ruleGroupStructMapper.entityToModelWithRuleRatios(entity, entityList)

        then: "should convert correctly with empty rules"
        ruleGroup != null
        ruleGroup.id == entityId
        ruleGroup.rules != null
        ruleGroup.rules.isEmpty()

        where:
        entityId << [10000001L, 10000002L, 10000003L]
    }

    def "test entityToModel - should return null when entity is null"() {
        given: "null entity and empty entity list"
        def entityList = []

        when: "convert null entity to RuleGroup"
        def ruleGroup = ruleGroupStructMapper.entityToModelWithRuleRatios(null, entityList)

        then: "should return null"
        ruleGroup == null
    }

    def "test entityToModel - should populate rules map from entity list with null Rule objects"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = groupId

        and: "entity list with data"
        def arrangementEntity = new ExecutionArrangementEntity()
        arrangementEntity.groupId = groupId
        arrangementEntity.ruleId = ruleId
        arrangementEntity.abRatio = abRatio

        def arrangementEntities = [arrangementEntity]

        when: "convert to RuleGroup"
        def ruleGroup = ruleGroupStructMapper.entityToModelWithRuleRatios(entity, arrangementEntities)

        then: "should populate rules map with rule IDs and ratios (Rule objects are null, loaded separately)"
        ruleGroup != null
        ruleGroup.id == groupId
        ruleGroup.rules != null
        ruleGroup.rules.size() == 1
        ruleGroup.rules.containsKey(ruleId)
        ruleGroup.rules.get(ruleId).left == null      // Rule object is null, loaded separately
        ruleGroup.rules.get(ruleId).right == abRatio  // Ratio is populated
        ruleGroup.getRuleIds().contains(ruleId)       // getRuleIds() works because rules map is populated

        where:
        ruleId | groupId   | abRatio
        1L     | 10000001L | 0
        2L     | 10000002L | 50
        3L     | 10000003L | 100
    }

    def "test entityToModel - should handle null entity list"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = 10000001L

        when: "convert to RuleGroup with null entity list"
        def ruleGroup = ruleGroupStructMapper.entityToModelWithRuleRatios(entity, null)

        then: "should convert correctly with empty rules"
        ruleGroup != null
        ruleGroup.id == 10000001L
        ruleGroup.rules != null
        ruleGroup.rules.isEmpty()
    }


    // ========== Integration tests ==========

    def "test entityToModel then modelToDTO - should work correctly"() {
        given: "a RuleGroupEntity"
        def entity = new RuleGroupEntity()
        entity.id = 10000001L

        when: "convert entity to model"
        def ruleGroup = ruleGroupStructMapper.entityToModelWithRuleRatios(entity, [])

        and: "convert model to DTO"
        def dto = ruleGroupStructMapper.modelToDTO(ruleGroup)

        then: "should work correctly"
        ruleGroup != null
        ruleGroup.id == 10000001L
        ruleGroup.rules.isEmpty()

        dto != null
        dto.id == 10000001L
        dto.ruleRatios.isEmpty()
    }
}

