package lab.zhang.rule.rule_engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Rule group entity for database
 *
 * @author Rongjin Zhang
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("rule_group")
public class RuleGroupEntity extends BaseEntity {

    /**
     * Group ID (primary key)
     * Auto-increment starting from 10,000,000
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * Group name
     */
    private String name;

    /**
     * Group description
     */
    private String description;
}

