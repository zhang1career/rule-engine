package lab.zhang.rule.rule_engine.validation;

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ValidContentTypeIdValidator implements ConstraintValidator<ValidContentTypeId, Integer> {

    @Override
    public void initialize(ValidContentTypeId constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull annotation
        if (value == null) {
            return true;
        }

        // Check if the value is a valid ContentTypeEnum enumeration ID
        ContentTypeEnum itemType = ContentTypeEnum.fromId(value);
        return itemType != null;
    }
}

