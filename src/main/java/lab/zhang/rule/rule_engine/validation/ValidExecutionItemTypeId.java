package lab.zhang.rule.rule_engine.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validation annotation for ExecutionItemTypeEnum enumeration ID
 * Validates that the integer value is a valid ExecutionItemTypeEnum enumeration ID
 *
 * @author Rongjin Zhang
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidExecutionItemTypeIdValidator.class)
@Documented
public @interface ValidExecutionItemTypeId {
    
    /**
     * Error message
     */
    String message() default "Invalid item type ID. Valid values are: 0=RULE, 1=RULE_GROUP";
    
    /**
     * Validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload
     */
    Class<? extends Payload>[] payload() default {};
}

