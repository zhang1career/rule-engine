package lab.zhang.rule.rule_engine.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for checking rule ratios in Map values
 * Validates that:
 * 1. Each ratio value is between 0 and 100
 * 2. Sum of all ratios does not exceed 100
 *
 * @author Rongjin Zhang
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRuleRatiosValidator.class)
@Documented
public @interface ValidRuleRatios {

    /**
     * Error message
     */
    String message() default "Invalid rule ratios";

    /**
     * Validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload
     */
    Class<? extends Payload>[] payload() default {};
}

