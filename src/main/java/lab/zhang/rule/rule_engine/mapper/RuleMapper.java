package lab.zhang.rule.rule_engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Rule mapper interface
 *
 * @author Rongjin Zhang
 */
@Mapper
public interface RuleMapper extends BaseMapper<RuleEntity> {
}

