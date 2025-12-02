package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Rule group and rule association entity for database
 *
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("rule_group_rule_rel")
public class RuleGroupRuleRelationEntity extends BaseEntity {

    /**
     * Rule group ID (part of composite primary key)
     * Note: Using IdType.INPUT to indicate this is an input value (not auto-generated).
     * MyBatis-Plus will still insert all fields including ruleId.
     */
    @TableId(type = IdType.INPUT)
    private Long groupId;

    /**
     * Rule ID (part of composite primary key)
     */
    private Long ruleId;

    /**
     * A/B test ratio (0-100)
     * Only valid when rule status is AB_TEST
     */
    private Integer abTestRatio;
}

