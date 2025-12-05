package lab.zhang.rule.rule_engine.validation

import lab.zhang.rule.rule_engine.pojo.qo.RuleQO
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

/**
 * ValidRuleStatusIdValidator test
 *
 * @author Rongjin Zhang
 */
class ValidRuleStatusIdValidatorSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    @Unroll
    def "test ValidRuleStatusId validation - ruleStatus: #ruleStatus, shouldPass: #shouldPass"() {
        given: "create RuleQO"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(0)
                .content("test content")
                .ruleStatus(ruleStatus)
                .build()

        when: "validate with Update group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Update.class)

        then: "validation should pass or fail as expected"
        def ruleStatusViolations = violations.findAll { it.propertyPath.toString() == "ruleStatus" }
        (ruleStatusViolations.isEmpty()) == shouldPass

        where:
        ruleStatus | shouldPass
        0          | true   // OFFLINE
        1          | true   // TEST
        2          | true   // GRAY
        3          | true   // ONLINE
        4          | false  // Invalid (no such status)
        5          | false  // Invalid
        6          | false  // Invalid
        -1         | false  // Invalid
        100        | false  // Invalid
    }

    @Unroll
    def "test ValidRuleStatusId validation with null value - should pass (handled by @NotNull)"() {
        given: "create RuleQO with null ruleStatus"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(0)
                .content("test content")
                .ruleStatus(null)
                .build()

        when: "validate with Update group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Update.class)

        then: "null is allowed (no @NotNull on ruleStatus in Update group)"
        def ruleStatusViolations = violations.findAll { it.propertyPath.toString() == "ruleStatus" }
        ruleStatusViolations.isEmpty()
    }

    @Unroll
    def "test ValidRuleStatusId validation message - ruleStatus: #ruleStatus"() {
        given: "create RuleQO with invalid ruleStatus"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(0)
                .content("test content")
                .ruleStatus(ruleStatus)
                .build()

        when: "validate with Update group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Update.class)

        then: "should have validation error with correct message"
        def ruleStatusViolations = violations.findAll { it.propertyPath.toString() == "ruleStatus" }
        ruleStatusViolations.size() > 0
        ruleStatusViolations.any { it.message.contains("Invalid rule status ID") }

        where:
        ruleStatus << [5, 6, -1, 100]
    }

    def "test ValidRuleStatusId validator directly - valid values"() {
        given: "create validator instance"
        def validatorInstance = new ValidRuleStatusIdValidator()
        validatorInstance.initialize(Mock(ValidRuleStatusId))

        expect: "should return true for valid RuleStatusEnum IDs"
        validatorInstance.isValid(0, Mock(javax.validation.ConstraintValidatorContext)) == true  // OFFLINE
        validatorInstance.isValid(1, Mock(javax.validation.ConstraintValidatorContext)) == true  // TEST
        validatorInstance.isValid(2, Mock(javax.validation.ConstraintValidatorContext)) == true  // GRAY
        validatorInstance.isValid(3, Mock(javax.validation.ConstraintValidatorContext)) == true  // ONLINE
    }

    def "test ValidRuleStatusId validator directly - invalid values"() {
        given: "create validator instance"
        def validatorInstance = new ValidRuleStatusIdValidator()
        validatorInstance.initialize(Mock(ValidRuleStatusId))

        expect: "should return false for invalid RuleStatusEnum IDs"
        validatorInstance.isValid(5, Mock(javax.validation.ConstraintValidatorContext)) == false
        validatorInstance.isValid(-1, Mock(javax.validation.ConstraintValidatorContext)) == false
        validatorInstance.isValid(100, Mock(javax.validation.ConstraintValidatorContext)) == false
    }

    def "test ValidRuleStatusId validator directly - null value"() {
        given: "create validator instance"
        def validatorInstance = new ValidRuleStatusIdValidator()
        validatorInstance.initialize(Mock(ValidRuleStatusId))

        expect: "should return true for null (handled by @NotNull)"
        validatorInstance.isValid(null, Mock(javax.validation.ConstraintValidatorContext)) == true
    }
}

