package lab.zhang.rule.rule_engine.controller

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import lab.zhang.rule.rule_engine.model.Rule
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO
import lab.zhang.rule.rule_engine.pojo.qo.RuleQO
import lab.zhang.rule.rule_engine.service.RuleService
import lab.zhang.rule.rule_engine.struct_mapper.RuleStructMapper
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Unroll

/**
 * RuleController unit test
 *
 * @author Rongjin Zhang
 */
class RuleControllerSpec extends Specification {

    def ruleService = Mock(RuleService)
    def ruleStructMapper = Mock(RuleStructMapper)
    def controller = new RuleController()

    def setup() {
        controller.ruleService = ruleService
        controller.ruleStructMapper = ruleStructMapper
    }

    // ========== getAllRules() tests ==========

    def "test getAllRules - should return all rules"() {
        given: "prepare rules"
        def rule1 = Rule.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.TEST)
                .build()
        def rule2 = Rule.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.SCRIPT)
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()
        def rules = [rule1, rule2]

        def dto1 = RuleDTO.builder()
                .id(1L)
                .name("Rule 1")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .ruleStatus(RuleStatusEnum.TEST.getId())
                .build()
        def dto2 = RuleDTO.builder()
                .id(2L)
                .name("Rule 2")
                .contentType(ContentTypeEnum.SCRIPT.getId())
                .ruleStatus(RuleStatusEnum.ONLINE.getId())
                .build()

        when: "get all rules"
        def response = controller.getAllRules()

        then: "should return all rules"
        1 * ruleService.getAllRules() >> rules
        1 * ruleStructMapper.modelToDTO(rule1) >> dto1
        1 * ruleStructMapper.modelToDTO(rule2) >> dto2
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data != null
        response.body.data.size() == 2
        response.body.data[0].id == 1L
        response.body.data[1].id == 2L
    }

    def "test getAllRules - should return empty list when no rules exist"() {
        when: "get all rules"
        def response = controller.getAllRules()

        then: "should return empty list"
        1 * ruleService.getAllRules() >> []
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data != null
        response.body.data.isEmpty()
    }

    // ========== getRule() tests ==========

    @Unroll
    def "test getRule - should return rule by ID - ruleId: #ruleId"() {
        given: "prepare rule"
        def rule = Rule.builder()
                .id(ruleId)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.TEST)
                .content("1 + 1")
                .description("Test description")
                .build()

        def dto = RuleDTO.builder()
                .id(ruleId)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .ruleStatus(RuleStatusEnum.TEST.getId())
                .content("1 + 1")
                .description("Test description")
                .build()

        when: "get rule by ID"
        def response = controller.getRule(ruleId)

        then: "should return rule"
        1 * ruleService.getRuleById(ruleId) >> rule
        1 * ruleStructMapper.modelToDTO(rule) >> dto
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data != null
        response.body.data.id == ruleId
        response.body.data.name == "Test Rule"

        where:
        ruleId << [1L, 10000001L, 99999999L]
    }

    @Unroll
    def "test getRule - should throw exception when rule not found - ruleId: #ruleId"() {
        when: "get non-existent rule"
        controller.getRule(ruleId)

        then: "should throw IllegalArgumentException"
        1 * ruleService.getRuleById(ruleId) >> null
        thrown(IllegalArgumentException)

        where:
        ruleId << [999L, 888L]
    }

    // ========== createRule() tests ==========

    @Unroll
    def "test createRule - should create rule successfully - contentType: #contentType"() {
        given: "prepare rule QO"
        def ruleQO = RuleQO.builder()
                .name("New Rule")
                .contentType(contentType.getId())
                .content(validContent)
                .description("Rule description")
                .build()

        def rule = Rule.builder()
                .id(10000001L)
                .name("New Rule")
                .contentType(contentType)
                .content(validContent)
                .description("Rule description")
                .ruleStatus(RuleStatusEnum.OFFLINE)
                .build()

        when: "create rule"
        def response = controller.createRule(ruleQO)

        then: "should create rule successfully"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.createRule(rule)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data == null

        where:
        contentType              | validContent
        ContentTypeEnum.EXPRESSION | "1 + 1"
        ContentTypeEnum.SCRIPT      | "return 'test'"
        ContentTypeEnum.API_QUERY   | '{"url": "http://example.com"}'
        ContentTypeEnum.SQL_QUERY   | "SELECT 1"
    }

    def "test createRule - should ignore ruleStatus field"() {
        given: "prepare rule QO with ruleStatus (should be ignored)"
        def ruleQO = RuleQO.builder()
                .name("New Rule")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .content("1 + 1")
                .ruleStatus(RuleStatusEnum.ONLINE.getId()) // This should be ignored
                .build()

        def rule = Rule.builder()
                .id(10000001L)
                .name("New Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("1 + 1")
                .ruleStatus(RuleStatusEnum.OFFLINE) // Always OFFLINE for new rules
                .build()

        when: "create rule"
        def response = controller.createRule(ruleQO)

        then: "should create rule with OFFLINE status (ruleStatus in QO is ignored)"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.createRule({ Rule r ->
            r.ruleStatus == RuleStatusEnum.OFFLINE
        })
        response.statusCode == HttpStatus.OK
        response.body.code == 0
    }

    def "test createRule - should handle service exception"() {
        given: "prepare rule QO"
        def ruleQO = RuleQO.builder()
                .name("New Rule")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .content("1 + 1")
                .build()

        def rule = Rule.builder()
                .id(10000001L)
                .name("New Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("1 + 1")
                .build()

        when: "create rule"
        controller.createRule(ruleQO)

        then: "should propagate exception"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.createRule(rule) >> {
            throw new IllegalArgumentException("Rule name already exists")
        }
        thrown(IllegalArgumentException)
    }

    // ========== updateRule() tests ==========

    @Unroll
    def "test updateRule - should update rule successfully - ruleId: #ruleId"() {
        given: "prepare rule QO"
        def ruleQO = RuleQO.builder()
                .name("Updated Rule")
                .contentType(ContentTypeEnum.SCRIPT.getId())
                .content("return 'updated'")
                .ruleStatus(RuleStatusEnum.TEST.getId())
                .description("Updated description")
                .build()

        def rule = Rule.builder()
                .id(ruleId)
                .name("Updated Rule")
                .contentType(ContentTypeEnum.SCRIPT)
                .content("return 'updated'")
                .ruleStatus(RuleStatusEnum.TEST)
                .description("Updated description")
                .build()

        when: "update rule"
        def response = controller.updateRule(ruleId, ruleQO)

        then: "should update rule successfully"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.updateRule(ruleId, rule)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data == null

        where:
        ruleId << [1L, 10000001L]
    }

    def "test updateRule - should handle partial update (null fields)"() {
        given: "prepare rule QO with null fields (partial update)"
        def ruleQO = RuleQO.builder()
                .name(null)
                .contentType(null)
                .content(null)
                .ruleStatus(RuleStatusEnum.TEST.getId())
                .description(null)
                .build()

        def rule = Rule.builder()
                .id(1L)
                .name(null)
                .contentType(null)
                .content(null)
                .ruleStatus(RuleStatusEnum.TEST)
                .description(null)
                .build()

        when: "update rule with null fields"
        def response = controller.updateRule(1L, ruleQO)

        then: "should update rule (null fields will be preserved from existing rule)"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.updateRule(1L, rule)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
    }

    def "test updateRule - should handle service exception"() {
        given: "prepare rule QO"
        def ruleQO = RuleQO.builder()
                .name("Updated Rule")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .content("1 + 1")
                .ruleStatus(RuleStatusEnum.TEST.getId())
                .build()

        def rule = Rule.builder()
                .id(999L)
                .name("Updated Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("1 + 1")
                .ruleStatus(RuleStatusEnum.TEST)
                .build()

        when: "update non-existent rule"
        controller.updateRule(999L, ruleQO)

        then: "should throw exception"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.updateRule(999L, rule) >> {
            throw new IllegalArgumentException("Rule not found: 999")
        }
        thrown(IllegalArgumentException)
    }

    def "test updateRule - should handle invalid status transition"() {
        given: "prepare rule QO with invalid status transition"
        def ruleQO = RuleQO.builder()
                .name("Updated Rule")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .content("1 + 1")
                .ruleStatus(RuleStatusEnum.ONLINE.getId())
                .build()

        def rule = Rule.builder()
                .id(1L)
                .name("Updated Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .content("1 + 1")
                .ruleStatus(RuleStatusEnum.ONLINE)
                .build()

        when: "update rule with invalid status transition"
        controller.updateRule(1L, ruleQO)

        then: "should throw exception"
        1 * ruleStructMapper.qoToModel(ruleQO) >> rule
        1 * ruleService.updateRule(1L, rule) >> {
            throw new IllegalArgumentException("Invalid status transition from TEST to FULL")
        }
        thrown(IllegalArgumentException)
    }

    // ========== deleteRule() tests ==========

    @Unroll
    def "test deleteRule - should delete rule successfully - ruleId: #ruleId"() {
        when: "delete rule"
        def response = controller.deleteRule(ruleId)

        then: "should delete rule successfully"
        1 * ruleService.deleteRule(ruleId)
        response.statusCode == HttpStatus.OK
        response.body.code == 0
        response.body.msg == "success"
        response.body.data == null

        where:
        ruleId << [1L, 10000001L, 99999999L]
    }

    def "test deleteRule - should handle service exception when rule not found"() {
        when: "delete non-existent rule"
        controller.deleteRule(999L)

        then: "should throw exception"
        1 * ruleService.deleteRule(999L) >> {
            throw new IllegalArgumentException("Rule not found: 999")
        }
        thrown(IllegalArgumentException)
    }

    def "test deleteRule - should handle service exception when rule is not OFFLINE"() {
        when: "delete rule that is not OFFLINE"
        controller.deleteRule(1L)

        then: "should throw exception"
        1 * ruleService.deleteRule(1L) >> {
            throw new IllegalArgumentException("Rule can only be deleted when status is OFFLINE. Current status: TEST")
        }
        thrown(IllegalArgumentException)
    }

    // ========== Integration tests ==========

    def "test getAllRules - should map all rules to DTOs correctly"() {
        given: "prepare multiple rules"
        def rules = [
                Rule.builder().id(1L).name("Rule 1").contentType(ContentTypeEnum.EXPRESSION).ruleStatus(RuleStatusEnum.TEST).build(),
                Rule.builder().id(2L).name("Rule 2").contentType(ContentTypeEnum.SCRIPT).ruleStatus(RuleStatusEnum.ONLINE).build(),
                Rule.builder().id(3L).name("Rule 3").contentType(ContentTypeEnum.API_QUERY).ruleStatus(RuleStatusEnum.GRAY).build()
        ]

        when: "get all rules"
        def response = controller.getAllRules()

        then: "should map all rules to DTOs"
        1 * ruleService.getAllRules() >> rules
        3 * ruleStructMapper.modelToDTO(_) >> { Rule r ->
            RuleDTO.builder()
                    .id(r.id)
                    .name(r.name)
                    .contentType(r.contentType != null ? r.contentType.getId() : null)
                    .ruleStatus(r.ruleStatus != null ? r.ruleStatus.getId() : null)
                    .build()
        }
        response.statusCode == HttpStatus.OK
        response.body.data.size() == 3
    }

    def "test getRule - should return correct DTO structure"() {
        given: "prepare rule with all fields"
        def rule = Rule.builder()
                .id(1L)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION)
                .ruleStatus(RuleStatusEnum.TEST)
                .content("1 + 1")
                .description("Test description")
                .build()

        def dto = RuleDTO.builder()
                .id(1L)
                .name("Test Rule")
                .contentType(ContentTypeEnum.EXPRESSION.getId())
                .ruleStatus(RuleStatusEnum.TEST.getId())
                .content("1 + 1")
                .description("Test description")
                .build()

        when: "get rule"
        def response = controller.getRule(1L)

        then: "should return DTO with all fields"
        1 * ruleService.getRuleById(1L) >> rule
        1 * ruleStructMapper.modelToDTO(rule) >> dto
        response.body.data.id == 1L
        response.body.data.name == "Test Rule"
        response.body.data.contentType == ContentTypeEnum.EXPRESSION.getId()
        response.body.data.ruleStatus == RuleStatusEnum.TEST.getId()
        response.body.data.content == "1 + 1"
        response.body.data.description == "Test description"
    }
}

