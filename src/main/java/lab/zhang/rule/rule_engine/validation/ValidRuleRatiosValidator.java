package lab.zhang.rule.rule_engine.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.*;

/**
 * Validator for @ValidRuleRatios annotation
 * Validates that:
 * 1. Each ratio value is between 0 and 100
 * 2. Sum of all ratios does not exceed 100
 *
 * @author Rongjin Zhang
 */
public class ValidRuleRatiosValidator implements ConstraintValidator<ValidRuleRatios, Map<Long, Integer>> {

    @Override
    public void initialize(ValidRuleRatios constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Map<Long, Integer> value, ConstraintValidatorContext context) {
        // Null values are handled by @NotEmpty annotation
        if (value == null || value.isEmpty()) {
            return true;
        }

        int totalRatio = 0;
        List<String> invalidRatios = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : value.entrySet()) {
            Long ruleId = entry.getKey();
            Integer ratio = entry.getValue();

            // Check if ruleId is null
            if (ruleId == null) {
                invalidRatios.add("Rule ID cannot be null");
                continue;
            }

            // Check if ratio is null or out of range
            if (ratio == null) {
                invalidRatios.add("Rule " + ruleId + ": ratio cannot be null");
                continue;
            }

            if (ratio < 0 || ratio > 100) {
                invalidRatios.add("Rule " + ruleId + ": ratio must be between 0 and 100, got " + ratio);
                continue;
            }

            totalRatio += ratio;
        }

        // Check if there are invalid individual ratios
        if (!invalidRatios.isEmpty()) {
            String errorMessage = "Invalid A/B test ratios: " + String.join("; ", invalidRatios);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addConstraintViolation();
            return false;
        }

        // Check if sum exceeds 100
        if (totalRatio > 100) {
            String errorMessage = "Sum of A/B test ratios cannot exceed 100. Current sum: " + totalRatio;
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(errorMessage)
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

