package lab.zhang.rule.rule_engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lab.zhang.rule.rule_engine.entity.EventEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Event mapper interface
 * 
 * @author Rongjin Zhang
 */
@Mapper
public interface EventMapper extends BaseMapper<EventEntity> {
}

