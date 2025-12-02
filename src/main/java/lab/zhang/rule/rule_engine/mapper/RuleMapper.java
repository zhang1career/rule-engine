package lab.zhang.rule.rule_engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Rule mapper interface
 * 
 * @author Rongjin Zhang
 */
@Mapper
public interface RuleMapper extends BaseMapper<RuleEntity> {
    
    /**
     * Batch update rules
     *
     * @param list list of rule entities to update
     * @return number of affected rows
     */
    int updateBatchById(@Param("list") List<RuleEntity> list);
}

