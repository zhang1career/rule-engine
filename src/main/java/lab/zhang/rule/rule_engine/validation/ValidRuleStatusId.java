package lab.zhang.rule.rule_engine.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;


/**
 * Validation annotation for RuleStatusEnum enumeration ID
 * Validates that the integer value is a valid RuleStatusEnum enumeration ID
 *
 * @author Rongjin Zhang
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidRuleStatusIdValidator.class)
@Documented
public @interface ValidRuleStatusId {

    /**
     * Error message
     */
    String message() default "Invalid rule status ID. Valid values are: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL";

    /**
     * Validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload
     */
    Class<? extends Payload>[] payload() default {};
}