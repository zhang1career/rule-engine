package lab.zhang.rule.rule_engine.struct_mapper;

import lab.zhang.rule.rule_engine.constant.CommonConst;
import lab.zhang.rule.rule_engine.entity.RuleContentEntity;
import lab.zhang.rule.rule_engine.entity.RuleEntity;
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum;
import lab.zhang.rule.rule_engine.pojo.dto.RuleDTO;
import lab.zhang.rule.rule_engine.pojo.qo.RuleQO;
import lab.zhang.rule.rule_engine.model.Rule;
import lab.zhang.rule.rule_engine.util.StrUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for converting pojos
 *
 * @author Rongjin Zhang
 */
@Mapper(componentModel = "spring")
public interface RuleStructMapper {

    /**
     * Convert Rule to RuleDTO
     */
    @Mapping(target = "contentType", expression = "java(rule.getContentType() != null ? rule.getContentType().getId() : null)")
    @Mapping(target = "ruleStatus", expression = "java(rule.getRuleStatus() != null ? rule.getRuleStatus().getId() : null)")
    RuleDTO modelToDTO(Rule rule);

    /**
     * Convert RuleEntity to Rule model
     * Note: content field is not mapped here, it should be loaded separately
     * Note: createTime and updateTime fields are converted from ct and ut (UNIX timestamp in seconds to Date)
     */
    default Rule entityToModel(RuleEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Rule rule = Rule.builder()
                .id(entity.getId())
                .name(entity.getName() != null ? entity.getName() : CommonConst.EMPTY_STRING)
                .description(entity.getDescription() != null ? entity.getDescription() : CommonConst.EMPTY_STRING)
                .contentType(entity.getContentTypeEnum())
                .content(CommonConst.EMPTY_STRING)
                .ruleStatus(entity.getRuleStatusEnum())
                .build();

        // Convert contentArgs to contentArgList
        if (entity.getContentArgs() != null && !entity.getContentArgs().trim().isEmpty()) {
            String[] args = entity.getContentArgs().split(",");
            rule.setContentArgList(java.util.Arrays.asList(args));
        } else {
            rule.setContentArgList(java.util.Collections.emptyList());
        }
        
        // Convert ct (UNIX timestamp in seconds) to createTime (Date)
        if (entity.getCt() != null) {
            rule.setCreateTimeByTimestamp(entity.getCt());
        }
        // Convert ut (UNIX timestamp in seconds) to updateTime (Date)
        if (entity.getUt() != null) {
            rule.setUpdateTimeByTimestamp(entity.getUt());
        }
        
        return rule;
    }

    /**
     * Convert RuleQO to Rule model
     *
     * @param qo RuleQO containing update fields
     * @return updated Rule model
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "contentType", expression = "java(qo.getContentTypeEnum())")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "ruleStatus", expression = "java(qo.getRuleStatusEnum())")
    Rule qoToModel(RuleQO qo);

    /**
     * Convert RuleQO to RuleDTO
     * @param qo RuleQO
     * @return RuleDTO
     */
    @Mapping(target = "id", ignore = true)
    RuleDTO qoToDto(RuleQO qo);

    /**
     * Convert Rule model to RuleEntity
     * Note: ct and ut fields will be set in @AfterMapping
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name")
    @Mapping(target = "description", source = "description")
    @Mapping(target = "ruleStatusEnum", ignore = true)
    @Mapping(target = "contentTypeEnum", ignore = true)
    @Mapping(target = "contentArgs", ignore = true)
    @Mapping(target = "contentType", expression = "java(rule.getContentType().getId())")
    @Mapping(target = "ruleStatus", expression = "java(rule.getRuleStatus() != null ? rule.getRuleStatus().getId() : null)")
    @Mapping(target = "ct", expression = "java(rule.getCreateTime() != null ? rule.getCreateTimeInTimestamp() : 0)")
    @Mapping(target = "ut", expression = "java(rule.getUpdateTime() != null ? rule.getUpdateTimeInTimestamp() : 0)")
    RuleEntity modelToEntity(Rule rule);

    /**
     * After mapping handler for modelToEntity: set enum fields, name, description, and time fields
     *
     * Note: MapStruct matches this method to modelToEntity based on:
     * - @MappingTarget type: RuleEntity (matches return type)
     * - Source parameter: Rule (matches modelToEntity parameter)
     */
    @AfterMapping
    default void applyModelToEntity(@MappingTarget RuleEntity entity, Rule rule) {
        if (rule == null) {
            return;
        }

        // Set default status to OFFLINE if not provided
        if (rule.getRuleStatus() == null) {
            entity.setRuleStatusEnum(RuleStatusEnum.OFFLINE);
        } else {
            entity.setRuleStatusEnum(rule.getRuleStatus());
        }

        // Set enum fields using entity's setter methods
        entity.setContentTypeEnum(rule.getContentType());

        // Convert contentArgList to contentArgs (comma-separated string)
        if (rule.getContentArgList() != null && !rule.getContentArgList().isEmpty()) {
            entity.setContentArgs(StrUtil.implode(rule.getContentArgList()));
        } else {
            entity.setContentArgs(CommonConst.EMPTY_STRING);
        }
    }

    /**
     * Convert Rule model to RuleContentEntity
     */
    @Mapping(target = "id", source = "id")
    @Mapping(target = "content", source = "content")
    @Mapping(target = "ct", expression = "java(rule.getCreateTime() != null ? rule.getCreateTimeInTimestamp(): 0)")
    @Mapping(target = "ut", expression = "java(rule.getUpdateTime() != null ? rule.getUpdateTimeInTimestamp(): 0)")
    RuleContentEntity modelToContentEntity(Rule rule);
}

