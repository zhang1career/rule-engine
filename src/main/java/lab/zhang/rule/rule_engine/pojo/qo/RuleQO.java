package lab.zhang.rule.rule_engine.pojo.qo;

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.validation.ValidContentTypeId;
import lab.zhang.rule.rule_engine.validation.ValidRuleStatusId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Rule DTO for REST API
 * According to project conventions, enumeration fields use enumeration IDs (integers) instead of enumeration values.
 * 
 * @author Rongjin Zhang
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleQO {
    
    /**
     * Rule name
     */
    @NotBlank(message = "Rule name cannot be blank", groups = {RuleQO.Create.class})
    @Size(max = 100, message = "Rule name must not exceed 100 characters", groups = {RuleQO.Create.class, RuleQO.Update.class})
    private String name;

    /**
     * Rule description
     */
    @Size(max = 250, message = "Rule description must not exceed 250 characters", groups = {RuleQO.Create.class, RuleQO.Update.class})
    private String description;

    /**
     * Rule content type ID (enumeration ID: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT)
     * Note: For PUT /api/rules/{ruleId} (update), this field is optional. If provided, it will be updated.
     */
    @NotNull(message = "Content type cannot be null", groups = {RuleQO.Create.class})
    @ValidContentTypeId(message = "Invalid rule content type ID. Valid values are: 0=Expression, 1=API Query, 2=SQL Query, 3=Script", groups = {RuleQO.Create.class, RuleQO.Update.class})
    private Integer contentType;

    /**
     * Rule content
     */
    @NotBlank(message = "Rule content cannot be blank", groups = {RuleQO.Create.class})
    private String content;

    /**
     * Rule status ID (enumeration ID: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL)
     * Note: For POST /api/rules (create), this field is ignored and will be automatically set to OFFLINE (0).
     * This field is only used for PUT /api/rules/{ruleId} (update).
     */
    @ValidRuleStatusId(message = "Invalid rule status ID. Valid values are: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL", groups = {RuleQO.Update.class})
    private Integer ruleStatus;


    /**
     * Validation group for create operation
     */
    public interface Create {
    }

    /**
     * Validation group for update operation
     */
    public interface Update {
    }


    /**
     * Convert to ContentTypeEnum enum
     */
    public ContentTypeEnum getContentTypeEnum() {
        return ContentTypeEnum.fromId(contentType);
    }

    /**
     * Convert to RuleStatusEnum enum
     */
    public RuleStatusEnum getRuleStatusEnum() {
        return RuleStatusEnum.fromId(ruleStatus);
    }
}

