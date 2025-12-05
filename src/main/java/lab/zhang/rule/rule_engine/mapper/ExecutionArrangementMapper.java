package lab.zhang.rule.rule_engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Execution event relation mapper interface
 *
 * @author Rongjin Zhang
 */
@Mapper
public interface ExecutionArrangementMapper extends BaseMapper<ExecutionArrangementEntity> {

    /**
     * Query execution arrangements by event ID with rule status filter
     * Joins x table with rule table and filters by allowed rule statuses
     *
     * @param eventId event ID
     * @param allowedStatusIds list of allowed rule status IDs
     * @return list of execution arrangement entities
     */
    List<ExecutionArrangementEntity> selectByEventIdOnRuleStatus(
            @Param("eventId") Integer eventId,
            @Param("ruleStatus") List<Integer> allowedStatusIds);

    /**
     * Update entity by primary key, updating non-null fields except primary key
     *
     * @param entity entity with primary key fields and fields to update
     * @return number of affected rows
     */
    int updateByPrimaryKey(@Param("entity") ExecutionArrangementEntity entity);
}
