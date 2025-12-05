package lab.zhang.rule.rule_engine.validation

import lab.zhang.rule.rule_engine.pojo.qo.RuleGroupQO
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

/**
 * ValidRuleRatiosValidator test
 *
 * @author Rongjin Zhang
 */
class ValidRuleRatiosValidatorSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    @Unroll
    def "test ValidRuleRatios validation - valid ratios: #ratios"() {
        given: "create RuleGroupQO with valid ratios"
        def qo = RuleGroupQO.builder()
                .ruleRatios(ratios)
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should pass"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.isEmpty()

        where:
        ratios << [
                [1L: 50, 2L: 50],                    // Sum = 100
                [1L: 100],                            // Single rule = 100
                [1L: 0, 2L: 0, 3L: 0],               // All zeros
                [1L: 30, 2L: 30, 3L: 40],            // Sum = 100
                [1L: 25, 2L: 25, 3L: 25, 4L: 25],    // Four rules, sum = 100
                [1L: 50, 2L: 50, 3L: 0],             // Sum = 100 with zero
        ]
    }

    @Unroll
    def "test ValidRuleRatios validation - invalid ratios (sum > 100): #ratios"() {
        given: "create RuleGroupQO with invalid ratios (sum > 100)"
        def qo = RuleGroupQO.builder()
                .ruleRatios(ratios)
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should fail with sum exceeds 100 error"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.size() > 0
        rulesViolations.any { it.message.contains("Sum of A/B test ratios cannot exceed 100") }

        where:
        ratios << [
                [1L: 50, 2L: 51],                    // Sum = 101
                [1L: 100, 2L: 1],                    // Sum = 101
                [1L: 30, 2L: 30, 3L: 41],           // Sum = 101
                [1L: 50, 2L: 50, 3L: 10],           // Sum = 110
        ]
    }

    @Unroll
    def "test ValidRuleRatios validation - invalid ratios (ratio < 0): #ratios"() {
        given: "create RuleGroupQO with invalid ratios (ratio < 0)"
        def qo = RuleGroupQO.builder()
                .ruleRatios(ratios)
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should fail with ratio < 0 error"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.size() > 0
        rulesViolations.any { it.message.contains("ratio must be between 0 and 100") }

        where:
        ratios << [
                [1L: -1],
                [1L: -10],
                [1L: 50, 2L: -1],
                [1L: -5, 2L: 50],
        ]
    }

    @Unroll
    def "test ValidRuleRatios validation - invalid ratios (ratio > 100): #ratios"() {
        given: "create RuleGroupQO with invalid ratios (ratio > 100)"
        def qo = RuleGroupQO.builder()
                .ruleRatios(ratios)
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should fail with ratio > 100 error"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.size() > 0
        rulesViolations.any { it.message.contains("ratio must be between 0 and 100") }

        where:
        ratios << [
                [1L: 101],
                [1L: 200],
                [1L: 50, 2L: 101],
                [1L: 150, 2L: 50],
        ]
    }

    @Unroll
    def "test ValidRuleRatios validation - null ratio: #ratios"() {
        given: "create RuleGroupQO with null ratio"
        def qo = RuleGroupQO.builder()
                .ruleRatios(ratios)
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should fail with null ratio error"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.size() > 0
        rulesViolations.any { it.message.contains("ratio cannot be null") }

        where:
        ratios << [
                [1L: null],
                [1L: 50, 2L: null],
                [1L: null, 2L: 50],
        ]
    }

    def "test ValidRuleRatios validation - null ruleId"() {
        given: "create RuleGroupQO with null ruleId"
        def ratios1 = new HashMap<Long, Integer>()
        ratios1.put(null, 50)
        
        def ratios2 = new HashMap<Long, Integer>()
        ratios2.put(null, 50)
        ratios2.put(2L, 50)
        
        def ratios3 = new HashMap<Long, Integer>()
        ratios3.put(1L, 50)
        ratios3.put(null, 50)

        when: "validate"
        def qo1 = RuleGroupQO.builder().ruleRatios(ratios1).build()
        def qo2 = RuleGroupQO.builder().ruleRatios(ratios2).build()
        def qo3 = RuleGroupQO.builder().ruleRatios(ratios3).build()
        
        Set<ConstraintViolation<RuleGroupQO>> violations1 = validator.validate(qo1)
        Set<ConstraintViolation<RuleGroupQO>> violations2 = validator.validate(qo2)
        Set<ConstraintViolation<RuleGroupQO>> violations3 = validator.validate(qo3)

        then: "validation should fail with null ruleId error"
        def rulesViolations1 = violations1.findAll { it.propertyPath.toString() == "ruleRatios" }
        def rulesViolations2 = violations2.findAll { it.propertyPath.toString() == "ruleRatios" }
        def rulesViolations3 = violations3.findAll { it.propertyPath.toString() == "ruleRatios" }
        
        rulesViolations1.size() > 0
        rulesViolations1.any { it.message.contains("Rule ID cannot be null") }
        
        rulesViolations2.size() > 0
        rulesViolations2.any { it.message.contains("Rule ID cannot be null") }
        
        rulesViolations3.size() > 0
        rulesViolations3.any { it.message.contains("Rule ID cannot be null") }
    }

    def "test ValidRuleRatios validation - null map"() {
        given: "create RuleGroupQO with null rules"
        def qo = RuleGroupQO.builder()
                .ruleRatios(null)
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should fail (null handled by @NotEmpty)"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.size() > 0
        rulesViolations.any { it.message.contains("cannot be empty") || it.message.contains("Rules cannot be empty") }
    }

    def "test ValidRuleRatios validation - empty map"() {
        given: "create RuleGroupQO with empty rules"
        def qo = RuleGroupQO.builder()
                .ruleRatios([:])
                .build()

        when: "validate"
        Set<ConstraintViolation<RuleGroupQO>> violations = validator.validate(qo)

        then: "validation should fail (empty handled by @NotEmpty)"
        def rulesViolations = violations.findAll { it.propertyPath.toString() == "ruleRatios" }
        rulesViolations.size() > 0
        rulesViolations.any { it.message.contains("cannot be empty") || it.message.contains("Rules cannot be empty") }
    }

}

