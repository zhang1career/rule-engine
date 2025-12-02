package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.entity.EventEntity;
import lab.zhang.rule.rule_engine.pojo.dto.EventDTO;
import lab.zhang.rule.rule_engine.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting Event related objects
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface EventStructMapper {

    /**
     * Convert Event model to EventDTO
     *
     * @param event Event model
     * @return EventDTO
     */
    EventDTO entityToDTO(Event event);

    /**
     * Convert EventEntity to Event model
     *
     * @param entity EventEntity
     * @return Event model
     */
    @Mapping(target = "name", expression = "java(entity.getName() != null ? entity.getName() : \"\")")
    @Mapping(target = "description", expression = "java(entity.getDescription() != null ? entity.getDescription() : \"\")")
    Event entityToModel(EventEntity entity);

    /**
     * Convert Event model to EventEntity
     * Note: Time fields (ct, ut) are not mapped here and should be set by the caller
     *
     * @param event Event model
     * @return EventEntity
     */
    @Mapping(target = "name", expression = "java(event.getName() != null ? event.getName() : \"\")")
    @Mapping(target = "description", expression = "java(event.getDescription() != null ? event.getDescription() : \"\")")
    @Mapping(target = "ct", ignore = true)
    @Mapping(target = "ut", ignore = true)
    EventEntity modelToEntity(Event event);
}

