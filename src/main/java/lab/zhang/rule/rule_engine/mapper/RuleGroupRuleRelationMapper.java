package lab.zhang.rule.rule_engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lab.zhang.rule.rule_engine.entity.RuleGroupRuleRelationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Rule group and rule association mapper interface
 * 
 * @author Rongjin Zhang
 */
@Mapper
public interface RuleGroupRuleRelationMapper extends BaseMapper<RuleGroupRuleRelationEntity> {
    
    /**
     * Batch update rule group rule relations
     * 
     * @param list list of rule group rule entities to update
     * @return number of affected rows
     */
    int updateBatchById(@Param("list") List<RuleGroupRuleRelationEntity> list);
}

