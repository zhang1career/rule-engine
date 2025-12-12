package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.model.EvalRequest;
import lab.zhang.rule.rule_engine.pojo.qo.EvalRequestQO;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper for converting EvalQO to EvalDTO
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface EvalStructMapper {

    /**
     * Convert EvalQO to EvalDTO
     *
     * @param qo evaluation query object
     * @return evaluation DTO
     */
    EvalRequest qoToModel(EvalRequestQO qo);
}

