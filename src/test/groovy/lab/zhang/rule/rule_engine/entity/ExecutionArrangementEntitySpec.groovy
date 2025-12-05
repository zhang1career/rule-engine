package lab.zhang.rule.rule_engine.entity

import spock.lang.Specification
import spock.lang.Unroll

/**
 * ExecutionEventRelationEntity unit test
 */
class ExecutionArrangementEntitySpec extends Specification {

    @Unroll
    def "test ExecutionEventRelationEntity getter and setter - eventId: #eventId, ruleId: #ruleId, groupId: #groupId, exeOrder: #exeOrder, abRatio: #abRatio"() {
        given: "create ExecutionEventRelationEntity"
        def entity = new ExecutionArrangementEntity()

        when: "set values"
        entity.setEventId(eventId)
        entity.setRuleId(ruleId)
        entity.setGroupId(groupId)
        entity.setExeOrder(exeOrder)
        entity.setAbRatio(abRatio)
        entity.setCt(1234567890)
        entity.setUt(1234567891)

        then: "should get correct values"
        entity.getEventId() == eventId
        entity.getRuleId() == ruleId
        entity.getGroupId() == groupId
        entity.getExeOrder() == exeOrder
        entity.getAbRatio() == abRatio
        entity.getCt() == 1234567890
        entity.getUt() == 1234567891

        where:
        eventId      | ruleId      | groupId     | exeOrder | abRatio
        10000001     | 10000001L   | 0L          | 0        | 0
        10000002     | 10000002L   | 10000001L   | 1        | 50
        10000003     | 10000003L   | 0L          | 2        | 0
    }

    @Unroll
    def "test ExecutionEventRelationEntity toString - eventId: #eventId, ruleId: #ruleId"() {
        given: "create ExecutionEventRelationEntity"
        def entity = new ExecutionArrangementEntity()
        entity.setEventId(eventId)
        entity.setRuleId(ruleId)
        entity.setGroupId(0L)
        entity.setExeOrder(0)
        entity.setAbRatio(0)

        when: "call toString"
        def result = entity.toString()

        then: "should contain eventId and ruleId"
        result != null
        result.contains(String.valueOf(eventId))
        result.contains(String.valueOf(ruleId))

        where:
        eventId     | ruleId
        10000001    | 10000001L
        10000002    | 10000002L
    }

    @Unroll
    def "test ExecutionEventRelationEntity equals and hashCode - eventId: #eventId1, ruleId: #ruleId1, groupId: #groupId1 vs eventId: #eventId2, ruleId: #ruleId2, groupId: #groupId2, shouldEqual: #shouldEqual"() {
        given: "create two ExecutionEventRelationEntities"
        def entity1 = new ExecutionArrangementEntity()
        entity1.setEventId(eventId1)
        entity1.setRuleId(ruleId1)
        entity1.setGroupId(groupId1)
        entity1.setExeOrder(0)
        entity1.setAbRatio(0)

        def entity2 = new ExecutionArrangementEntity()
        entity2.setEventId(eventId2)
        entity2.setRuleId(ruleId2)
        entity2.setGroupId(groupId2)
        entity2.setExeOrder(0)
        entity2.setAbRatio(0)

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
        eventId1    | ruleId1     | groupId1    | eventId2    | ruleId2     | groupId2    | shouldEqual
        10000001    | 10000001L   | 0L          | 10000001    | 10000001L   | 0L          | true
        10000001    | 10000001L   | 0L          | 10000002    | 10000001L   | 0L          | false
        10000001    | 10000001L   | 0L          | 10000001    | 10000002L   | 0L          | false
        10000001    | 10000001L   | 0L          | 10000001    | 10000001L   | 10000001L   | false
    }

    @Unroll
    def "test ExecutionEventRelationEntity default values"() {
        when: "create ExecutionEventRelationEntity with default constructor"
        def entity = new ExecutionArrangementEntity()

        then: "should have null or default values"
        entity.getEventId() == null
        entity.getRuleId() == null
        entity.getGroupId() == null
        entity.getExeOrder() == null
        entity.getAbRatio() == null
        entity.getCt() == null
        entity.getUt() == null
    }
}

