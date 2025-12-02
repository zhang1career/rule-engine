package lab.zhang.rule.rule_engine.validation;

import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for @ValidExecutionItemTypeId annotation
 * Validates that the integer value is a valid ExecutionItemTypeEnum enumeration ID
 *
 * @author Rongjin Zhang
 */
public class ValidExecutionItemTypeIdValidator implements ConstraintValidator<ValidExecutionItemTypeId, Integer> {

    @Override
    public void initialize(ValidExecutionItemTypeId constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull annotation
        if (value == null) {
            return true;
        }
        
        // Check if the value is a valid ExecutionItemTypeEnum enumeration ID
        ExecutionItemTypeEnum itemType = ExecutionItemTypeEnum.fromId(value);
        return itemType != null;
    }
}

