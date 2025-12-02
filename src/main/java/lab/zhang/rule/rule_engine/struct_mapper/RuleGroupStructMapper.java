package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.entity.RuleGroupEntity;
import lab.zhang.rule.rule_engine.entity.RuleGroupRuleRelationEntity;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.model.RuleGroup;
import lab.zhang.rule.rule_engine.pojo.dto.RuleGroupDTO;
import org.apache.commons.lang3.tuple.Pair;
import org.mapstruct.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lab.zhang.rule.rule_engine.util.RatioUtil.buildRatioKey;

/**
 * MapStruct mapper for converting pojos
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface RuleGroupStructMapper {

    /**
     * Convert RuleGroup to RuleGroupDTO without rules field
     * Used for getAllRuleGroups to avoid loading rules data
     */
    @Mapping(target = "rules", expression = "java(java.util.Collections.emptyMap())")
    RuleGroupDTO modelToDTO(RuleGroup group);

    /**
     * Convert RuleGroup to RuleGroupDTO
     * Note: rules map will be set in @AfterMapping from ratios map
     */
    @Mapping(target = "rules", ignore = true)
    RuleGroupDTO modelToDTOWithRulesInternal(RuleGroup group, @Context Map<String, Integer> ratiosMap);

    /**
     * Convert RuleGroup to RuleGroupDTO with rules
     * This is a wrapper method that calls modelToDTOWithRulesInternal and then setRuleGroupRules
     */
    default RuleGroupDTO modelToDTOWithRules(RuleGroup group, Map<String, Integer> ratiosMap) {
        if (group == null) {
            return null;
        }
        RuleGroupDTO dto = modelToDTOWithRulesInternal(group, ratiosMap);
        if (dto != null) {
            setRuleGroupRules(dto, group, ratiosMap);
        }
        return dto;
    }

    /**
     * After mapping RuleGroup to RuleGroupDTO, set rules map from ratios map
     * Note: ratiosMap key format is "groupId:ruleId", value is the A/B test ratio
     */
    @AfterMapping
    default void setRuleGroupRules(@MappingTarget RuleGroupDTO dto, RuleGroup group, @Context Map<String, Integer> ratiosMap) {
        if (group == null || group.getId() == null || group.getRuleIds() == null || group.getRuleIds().isEmpty()) {
            dto.setRules(Collections.emptyMap());
            return;
        }

        Map<Long, Integer> rules = new HashMap<>();
        Long groupId = group.getId();
        for (Long ruleId : group.getRuleIds()) {
            // Get ratio from pre-loaded ratios map (key format: "groupId:ruleId")
            String key = buildRatioKey(groupId, ruleId);
            Integer ratio = (ratiosMap != null && ratiosMap.containsKey(key) && ratiosMap.get(key) != null) ? ratiosMap.get(key) : 0;
            rules.put(ruleId, ratio);
        }
        dto.setRules(rules);
    }

    /**
     * Convert RuleGroupEntity to RuleGroup model (internal method)
     * Note: rules map will be set empty initially, should be loaded separately when needed
     *
     * @param entity       RuleGroupEntity
     * @param relationsMap Map of groupId to List of RuleGroupRuleRelationEntity (unused, kept for compatibility)
     * @return RuleGroup model
     */
    @Mapping(target = "rules", ignore = true)
    RuleGroup entityToModelInternal(RuleGroupEntity entity, @Context Map<Long, List<RuleGroupRuleRelationEntity>> relationsMap);

    /**
     * Convert RuleGroupEntity to RuleGroup model
     * This is a wrapper method that calls entityToModelInternal and then initializeRulesMap
     */
    default RuleGroup entityToModel(RuleGroupEntity entity, Map<Long, List<RuleGroupRuleRelationEntity>> relationsMap) {
        if (entity == null) {
            return null;
        }
        RuleGroup group = entityToModelInternal(entity, relationsMap);
        if (group != null) {
            initializeRulesMap(group, entity, relationsMap);
        }
        return group;
    }

    /**
     * After mapping RuleGroupEntity to RuleGroup, initialize rules map from relations
     * Note: Rule objects will be loaded separately when needed (e.g., in getRuleGroupsWithRules)
     * For now, we populate the map with null Rule objects and ratios from relations
     */
    @AfterMapping
    default void initializeRulesMap(@MappingTarget RuleGroup group, RuleGroupEntity entity, @Context Map<Long, List<RuleGroupRuleRelationEntity>> relationsMap) {
        if (group == null) {
            return;
        }
        Map<Long, Pair<Rule, Integer>> rulesMap = new HashMap<>();
        if (relationsMap != null && entity != null && entity.getId() != null) {
            List<RuleGroupRuleRelationEntity> relations = relationsMap.get(entity.getId());
            if (relations != null) {
                for (RuleGroupRuleRelationEntity relation : relations) {
                    if (relation != null && relation.getRuleId() != null) {
                        Long ruleId = relation.getRuleId();
                        Integer ratio = relation.getAbTestRatio() != null ? relation.getAbTestRatio() : 0;
                        // Rule object will be loaded separately when needed
                        rulesMap.put(ruleId, Pair.of(null, ratio));
                    }
                }
            }
        }
        group.setRules(rulesMap);
    }
}

