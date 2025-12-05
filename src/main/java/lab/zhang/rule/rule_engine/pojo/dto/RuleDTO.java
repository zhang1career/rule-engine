package lab.zhang.rule.rule_engine.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
public class RuleDTO {

    /**
     * Rule ID
     */
    private Long id;

    /**
     * Rule name
     */
    private String name;

    /**
     * Rule description
     */
    private String description;

    /**
     * Rule content type ID (enumeration ID: 0=EXPRESSION, 1=API_QUERY, 2=SQL_QUERY, 3=SCRIPT)
     * Note: For PUT /api/rules/{ruleId} (update), this field is optional. If provided, it will be updated.
     */
    private Integer contentType;

    /**
     * Rule content
     */
    private String content;

    /**
     * Rule status ID (enumeration ID: 0=OFFLINE, 1=TEST, 2=GRAY, 3=AB_TEST, 4=FULL)
     * Note: For POST /api/rules (create), this field is ignored and will be automatically set to OFFLINE (0).
     * This field is only used for PUT /api/rules/{ruleId} (update).
     */
    private Integer ruleStatus;
}

