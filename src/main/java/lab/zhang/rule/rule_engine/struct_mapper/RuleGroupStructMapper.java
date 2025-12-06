package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.entity.ExecutionArrangementEntity;
import lab.zhang.rule.rule_engine.entity.RuleGroupEntity;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleGroupQO;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MapStruct mapper for converting pojos
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface RuleGroupStructMapper {

    /**
     * Convert RuleGroup to RuleGroupDTO
     * This is a wrapper method that calls modelToDTOInternal and then setRuleRatiosFromRules
     */
    default RuleGroupDTO modelToDTO(RuleGroup group) {
        if (group == null) {
            return null;
        }
        RuleGroupDTO dto = modelToDTOInternal(group);
        if (dto == null) {
            return null;
        }

        if (group.getRules() == null || group.getRules().isEmpty()) {
            dto.setRuleRatios(Collections.emptyMap());
            return dto;
        }

        Map<Long, Integer> ruleRatioMap = new HashMap<>();
        for (Map.Entry<Long, Pair<Rule, Integer>> entry : group.getRules().entrySet()) {
            Long ruleId = entry.getKey();
            Pair<Rule, Integer> pair = entry.getValue();
            // Extract ratio (right value) from Pair
            Integer ratio = (pair != null && pair.getRight() != null) ? pair.getRight() : 0;
            ruleRatioMap.put(ruleId, ratio);
        }
        dto.setRuleRatios(ruleRatioMap);

        return dto;
    }

    /**
     * Convert RuleGroup to RuleGroupDTO
     * Extracts rule ratios from RuleGroup.rules map (key: ruleId, value: Pair<Rule, Integer>)
     * and builds a Map<Long, Integer> for ruleRatios field
     */
    @Mapping(target = "ruleRatios", ignore = true)
    RuleGroupDTO modelToDTOInternal(RuleGroup group);

    /**
     * Convert RuleGroupQO to RuleGroup model
     * Converts ruleRatios map to rules map, with Rule objects set to null (to be loaded separately)
     *
     * @param qo RuleGroupQO containing rule ratios
     * @return RuleGroup model
     */
    default RuleGroup qoToModel(RuleGroupQO qo) {
        if (qo == null) {
            return null;
        }
        RuleGroup group = new RuleGroup();
        if (qo.getRuleRatios() == null || qo.getRuleRatios().isEmpty()) {
            group.setRules(Collections.emptyMap());
            return group;
        }
        Map<Long, Pair<Rule, Integer>> rulesMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : qo.getRuleRatios().entrySet()) {
            Long ruleId = entry.getKey();
            Integer ratio = entry.getValue() != null ? entry.getValue() : 0;
            // Rule object will be loaded separately when needed
            rulesMap.put(ruleId, Pair.of(null, ratio));
        }
        group.setRules(rulesMap);
        return group;
    }

    /**
     * Convert RuleGroupEntity to RuleGroup model
     * This is a wrapper method that calls entityToModelInternal and then setRuleMap
     */
    default RuleGroup entityToModelWithRuleRatios(RuleGroupEntity entity, List<ExecutionArrangementEntity> entityList) {
        if (entity == null) {
            return null;
        }
        RuleGroup group = entityToModel(entity);
        if (group == null) {
            return null;
        }
        setRuleMap(group, entityList);
        return group;
    }

    default void setRuleMap(RuleGroup group,
                            List<ExecutionArrangementEntity> arrangementEntityList) {
        if (arrangementEntityList == null) {
            group.setRules(Collections.emptyMap());
            return;
        }
        Map<Long, Pair<Rule, Integer>> rulesMap = new HashMap<>();
        for (ExecutionArrangementEntity arrangementEntity : arrangementEntityList) {
            if (arrangementEntity == null || arrangementEntity.getRuleId() == null) {
                continue;
            }
            Long ruleId = arrangementEntity.getRuleId();
            Integer ratio = arrangementEntity.getAbRatio() != null ? arrangementEntity.getAbRatio() : 0;
            // Rule object will be loaded separately when needed
            rulesMap.put(ruleId, Pair.of(null, ratio));
        }

        group.setRules(rulesMap);
    }

    /**
     * Convert RuleGroupEntity to RuleGroup model (internal method)
     * Note: rules map will be set empty initially, should be loaded separately when needed
     *
     * @param entity RuleGroupEntity
     * @return RuleGroup model
     */
    @Mapping(target = "rules", ignore = true)
    RuleGroup entityToModel(RuleGroupEntity entity);

    /**
     * Convert RuleGroup model to RuleGroupEntity
     * Note: Time fields (ct, ut) are not mapped here and should be set by the caller
     *
     * @param group RuleGroup model
     * @return RuleGroupEntity
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", expression = "java(group.getName() != null ? group.getName() : \"\")")
    @Mapping(target = "description", expression = "java(group.getDescription() != null ? group.getDescription() : \"\")")
    @Mapping(target = "ct", ignore = true)
    @Mapping(target = "ut", ignore = true)
    RuleGroupEntity modelToEntity(RuleGroup group);
}

