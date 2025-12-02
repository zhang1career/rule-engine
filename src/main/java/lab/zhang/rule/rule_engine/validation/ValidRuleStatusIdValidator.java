package lab.zhang.rule.rule_engine.validation;

import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class ValidRuleStatusIdValidator implements ConstraintValidator<ValidRuleStatusId, Integer> {

    @Override
    public void initialize(ValidRuleStatusId constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        // Null values are handled by @NotNull annotation
        if (value == null) {
            return true;
        }

        // Check if the value is a valid RuleStatusEnum enumeration ID
        RuleStatusEnum itemType = RuleStatusEnum.fromId(value);
        return itemType != null;
    }
}

