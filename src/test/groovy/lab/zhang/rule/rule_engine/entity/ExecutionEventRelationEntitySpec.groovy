package lab.zhang.rule.rule_engine.entity

import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum
import spock.lang.Specification
import spock.lang.Unroll

/**
 * ExecutionEventRelationEntity unit test
 */
class ExecutionEventRelationEntitySpec extends Specification {

    @Unroll
    def "test ExecutionEventRelationEntity getter and setter - eventId: #eventId, itemType: #itemType, itemId: #itemId, executionOrder: #executionOrder"() {
        given: "create ExecutionEventRelationEntity"
        def entity = new ExecutionEventRelationEntity()

        when: "set values"
        entity.setEventId(eventId)
        entity.setItemType(itemType)
        entity.setItemId(itemId)
        entity.setExecutionOrder(executionOrder)
        entity.setCt(1234567890)
        entity.setUt(1234567891)

        then: "should get correct values"
        entity.getEventId() == eventId
        entity.getItemType() == itemType
        entity.getItemId() == itemId
        entity.getExecutionOrder() == executionOrder
        entity.getCt() == 1234567890
        entity.getUt() == 1234567891

        where:
        eventId      | itemType                          | itemId      | executionOrder
        10000001L    | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | 1
        10000002L    | ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000002L | 2
        10000003L    | ExecutionItemTypeEnum.RULE.getId() | 10000003L   | 3
    }

    @Unroll
    def "test ExecutionEventRelationEntity getItemTypeEnum and setItemTypeEnum - itemType: #itemType, expectedEnum: #expectedEnum"() {
        given: "create ExecutionEventRelationEntity"
        def entity = new ExecutionEventRelationEntity()

        when: "set itemType using enum"
        entity.setItemTypeEnum(expectedEnum)

        then: "should get correct itemType ID"
        entity.getItemType() == itemType
        entity.getItemTypeEnum() == expectedEnum

        where:
        itemType                          | expectedEnum
        ExecutionItemTypeEnum.RULE.getId() | ExecutionItemTypeEnum.RULE
        ExecutionItemTypeEnum.RULE_GROUP.getId() | ExecutionItemTypeEnum.RULE_GROUP
    }

    @Unroll
    def "test ExecutionEventRelationEntity getItemTypeEnum - null itemType"() {
        given: "create ExecutionEventRelationEntity with null itemType"
        def entity = new ExecutionEventRelationEntity()
        entity.setItemType(null)

        when: "get itemType enum"
        def result = entity.getItemTypeEnum()

        then: "should return null"
        result == null
    }

    @Unroll
    def "test ExecutionEventRelationEntity setItemTypeEnum - null enum"() {
        given: "create ExecutionEventRelationEntity"
        def entity = new ExecutionEventRelationEntity()
        entity.setItemType(ExecutionItemTypeEnum.RULE.getId())

        when: "set null enum"
        entity.setItemTypeEnum(null)

        then: "should set itemType to null"
        entity.getItemType() == null
    }

    @Unroll
    def "test ExecutionEventRelationEntity toString - eventId: #eventId, itemId: #itemId"() {
        given: "create ExecutionEventRelationEntity"
        def entity = new ExecutionEventRelationEntity()
        entity.setEventId(eventId)
        entity.setItemType(ExecutionItemTypeEnum.RULE.getId())
        entity.setItemId(itemId)
        entity.setExecutionOrder(1)

        when: "call toString"
        def result = entity.toString()

        then: "should contain eventId and itemId"
        result != null
        result.contains(String.valueOf(eventId))
        result.contains(String.valueOf(itemId))

        where:
        eventId     | itemId
        10000001L   | 10000001L
        10000002L   | 10000002L
    }

    @Unroll
    def "test ExecutionEventRelationEntity equals and hashCode - eventId: #eventId1, itemType: #itemType1, itemId: #itemId1 vs eventId: #eventId2, itemType: #itemType2, itemId: #itemId2, shouldEqual: #shouldEqual"() {
        given: "create two ExecutionEventRelationEntities"
        def entity1 = new ExecutionEventRelationEntity()
        entity1.setEventId(eventId1)
        entity1.setItemType(itemType1)
        entity1.setItemId(itemId1)
        entity1.setExecutionOrder(1)

        def entity2 = new ExecutionEventRelationEntity()
        entity2.setEventId(eventId2)
        entity2.setItemType(itemType2)
        entity2.setItemId(itemId2)
        entity2.setExecutionOrder(1)

        when: "compare entities"
        def equals = entity1.equals(entity2)
        def hashCode1 = entity1.hashCode()
        def hashCode2 = entity2.hashCode()

        then: "should equal or not as expected"
        equals == shouldEqual
        if (shouldEqual) {
            hashCode1 == hashCode2
        }

        where:
        eventId1    | itemType1                          | itemId1     | eventId2    | itemType2                          | itemId2     | shouldEqual
        10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | 10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | true
        10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | 10000002L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | false
        10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | 10000001L   | ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000001L   | false
        10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | 10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000002L   | false
    }

    @Unroll
    def "test ExecutionEventRelationEntity default values"() {
        when: "create ExecutionEventRelationEntity with default constructor"
        def entity = new ExecutionEventRelationEntity()

        then: "should have null or default values"
        entity.getEventId() == null
        entity.getItemType() == null
        entity.getItemId() == null
        entity.getExecutionOrder() == null
        entity.getCt() == null
        entity.getUt() == null
    }
}

