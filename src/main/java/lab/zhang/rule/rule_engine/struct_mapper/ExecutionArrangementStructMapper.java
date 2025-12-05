package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity;
import lab.zhang.rule.rule_engine.model.ExecutionArrangement;
import lab.zhang.rule.rule_engine.pojo.dto.ExecutionArrangementDTO;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for execution arrangement
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface ExecutionArrangementStructMapper {
    /**
     * Convert ExecutionArrangementEntity to ExecutionArrangement
     *
     * @param entity the entity to convert
     * @return the DTO
     */
    ExecutionArrangement entityToModel(ExecutionArrangementEntity entity);

    /**
     * Convert ExecutionArrangement to ExecutionArrangementDTO
     * @param model the model to convert
     * @return the DTO
     */
    ExecutionArrangementDTO modelToDTO(ExecutionArrangement model);
}
