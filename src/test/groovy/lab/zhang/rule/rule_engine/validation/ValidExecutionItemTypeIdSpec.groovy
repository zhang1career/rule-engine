package lab.zhang.rule.rule_engine.validation

import lab.zhang.rule.rule_engine.pojo.qo.ExecutionItemQO
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

/**
 * ValidItemTypeId validation annotation test
 */
class ValidExecutionItemTypeIdSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    @Unroll
    def "test ValidItemTypeId validation - itemType: #itemType, shouldPass: #shouldPass"() {
        given: "create ExecutionItemQO"
        def qo = ExecutionItemQO.builder()
                .itemType(itemType)
                .itemId(10000001L)
                .build()

        when: "validate"
        Set<ConstraintViolation<ExecutionItemQO>> violations = validator.validate(qo)

        then: "validation should pass or fail as expected"
        def itemTypeViolations = violations.findAll { it.propertyPath.toString() == "itemType" }
        (itemTypeViolations.isEmpty()) == shouldPass

        where:
        itemType | shouldPass
        0        | true   // RULE
        1        | true   // RULE_GROUP
        2        | false  // Invalid
        3        | false  // Invalid
        -1       | false  // Invalid
        100      | false  // Invalid
        null     | false  // null (handled by @NotNull)
    }

    @Unroll
    def "test ValidItemTypeId validation message - itemType: #itemType"() {
        given: "create ExecutionItemQO with invalid itemType"
        def qo = ExecutionItemQO.builder()
                .itemType(itemType)
                .itemId(10000001L)
                .build()

        when: "validate"
        Set<ConstraintViolation<ExecutionItemQO>> violations = validator.validate(qo)

        then: "should have validation error with correct message"
        def itemTypeViolations = violations.findAll { it.propertyPath.toString() == "itemType" }
        itemTypeViolations.size() > 0
        itemTypeViolations[0].message.contains("Invalid item type ID")

        where:
        itemType << [2, 3, -1, 100]
    }
}

