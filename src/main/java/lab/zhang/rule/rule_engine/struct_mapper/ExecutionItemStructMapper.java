package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.entity.ExecutionEventRelationEntity;
import lab.zhang.rule.rule_engine.pojo.dto.ExecutionItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting pojos
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface ExecutionItemStructMapper {
    /**
     * Convert ExecutionEventRelationEntity to ExecutionItemDTO
     */
    @Mapping(target = "itemType", expression = "java(relation.getItemTypeEnum() != null ? relation.getItemTypeEnum().getId() : null)")
    @Mapping(target = "itemId", source = "itemId")
    @Mapping(target = "executionOrder", source = "executionOrder")
    ExecutionItemDTO toExecutionItemDTO(ExecutionEventRelationEntity relation);
}
