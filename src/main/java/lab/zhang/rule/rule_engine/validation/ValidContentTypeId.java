package lab.zhang.rule.rule_engine.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;


/**
 * Validation annotation for ContentTypeEnum enumeration ID
 * Validates that the integer value is a valid ContentTypeEnum enumeration ID
 *
 * @author Rongjin Zhang
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidContentTypeIdValidator.class)
@Documented
public @interface ValidContentTypeId {

    /**
     * Error message
     */
    String message() default "Invalid rule content type ID. Valid values are: 0=Expression, 1=API Query, 2=SQL Query, 3=Script";

    /**
     * Validation groups
     */
    Class<?>[] groups() default {};

    /**
     * Payload
     */
    Class<? extends Payload>[] payload() default {};
}