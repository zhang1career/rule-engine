package lab.zhang.rule.rule_engine.validation

import lab.zhang.rule.rule_engine.pojo.qo.RuleQO
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

/**
 * ValidContentTypeIdValidator test
 *
 * @author Rongjin Zhang
 */
class ValidContentTypeIdValidatorSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    @Unroll
    def "test ValidContentTypeId validation - contentType: #contentType, shouldPass: #shouldPass"() {
        given: "create RuleQO"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(contentType)
                .content("test content")
                .build()

        when: "validate with Create group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Create.class)

        then: "validation should pass or fail as expected"
        def contentTypeViolations = violations.findAll { it.propertyPath.toString() == "contentType" }
        (contentTypeViolations.isEmpty()) == shouldPass

        where:
        contentType | shouldPass
        0           | true   // EXPRESSION
        1           | true   // API_QUERY
        2           | true   // SQL_QUERY
        3           | true   // SCRIPT
        4           | false  // Invalid
        5           | false  // Invalid
        -1          | false  // Invalid
        100         | false  // Invalid
    }

    @Unroll
    def "test ValidContentTypeId validation with null value - should pass (handled by @NotNull)"() {
        given: "create RuleQO with null contentType"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(null)
                .content("test content")
                .build()

        when: "validate with Create group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Create.class)

        then: "should have @NotNull violation, not ValidContentTypeId violation"
        def contentTypeViolations = violations.findAll { it.propertyPath.toString() == "contentType" }
        contentTypeViolations.size() > 0
        contentTypeViolations.any { it.message.contains("cannot be null") }
        !contentTypeViolations.any { it.message.contains("Invalid rule content type ID") }
    }

    @Unroll
    def "test ValidContentTypeId validation message - contentType: #contentType"() {
        given: "create RuleQO with invalid contentType"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(contentType)
                .content("test content")
                .build()

        when: "validate with Create group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Create.class)

        then: "should have validation error with correct message"
        def contentTypeViolations = violations.findAll { it.propertyPath.toString() == "contentType" }
        contentTypeViolations.size() > 0
        contentTypeViolations.any { it.message.contains("Invalid rule content type ID") }

        where:
        contentType << [4, 5, -1, 100]
    }

    @Unroll
    def "test ValidContentTypeId validation in Update group - contentType: #contentType, shouldPass: #shouldPass"() {
        given: "create RuleQO"
        def qo = RuleQO.builder()
                .name("Test Rule")
                .contentType(contentType)
                .content("test content")
                .build()

        when: "validate with Update group"
        Set<ConstraintViolation<RuleQO>> violations = validator.validate(qo, RuleQO.Update.class)

        then: "validation should pass or fail as expected"
        def contentTypeViolations = violations.findAll { it.propertyPath.toString() == "contentType" }
        (contentTypeViolations.isEmpty()) == shouldPass

        where:
        contentType | shouldPass
        0           | true   // EXPRESSION
        1           | true   // API_QUERY
        2           | true   // SQL_QUERY
        3           | true   // SCRIPT
        4           | false  // Invalid
        -1          | false  // Invalid
        null        | true   // null is allowed in Update group (handled by @NotNull in Create group)
    }

    def "test ValidContentTypeId validator directly - valid values"() {
        given: "create validator instance"
        def validatorInstance = new ValidContentTypeIdValidator()
        validatorInstance.initialize(Mock(ValidContentTypeId))

        expect: "should return true for valid ContentTypeEnum IDs"
        validatorInstance.isValid(0, Mock(javax.validation.ConstraintValidatorContext)) == true  // EXPRESSION
        validatorInstance.isValid(1, Mock(javax.validation.ConstraintValidatorContext)) == true  // API_QUERY
        validatorInstance.isValid(2, Mock(javax.validation.ConstraintValidatorContext)) == true  // SQL_QUERY
        validatorInstance.isValid(3, Mock(javax.validation.ConstraintValidatorContext)) == true  // SCRIPT
    }

    def "test ValidContentTypeId validator directly - invalid values"() {
        given: "create validator instance"
        def validatorInstance = new ValidContentTypeIdValidator()
        validatorInstance.initialize(Mock(ValidContentTypeId))

        expect: "should return false for invalid ContentTypeEnum IDs"
        validatorInstance.isValid(4, Mock(javax.validation.ConstraintValidatorContext)) == false
        validatorInstance.isValid(-1, Mock(javax.validation.ConstraintValidatorContext)) == false
        validatorInstance.isValid(100, Mock(javax.validation.ConstraintValidatorContext)) == false
    }

    def "test ValidContentTypeId validator directly - null value"() {
        given: "create validator instance"
        def validatorInstance = new ValidContentTypeIdValidator()
        validatorInstance.initialize(Mock(ValidContentTypeId))

        expect: "should return true for null (handled by @NotNull)"
        validatorInstance.isValid(null, Mock(javax.validation.ConstraintValidatorContext)) == true
    }
}

