package lab.zhang.rule.rule_engine.entity

import spock.lang.Specification
import spock.lang.Unroll

/**
 * EventEntity unit test
 */
class EventEntitySpec extends Specification {

    @Unroll
    def "test EventEntity getter and setter - id: #id, name: #name, description: #description"() {
        given: "create EventEntity"
        def entity = new EventEntity()

        when: "set values"
        entity.setId(id)
        entity.setName(name)
        entity.setDescription(description)
        entity.setCt(ct)
        entity.setUt(ut)

        then: "should get correct values"
        entity.getId() == id
        entity.getName() == name
        entity.getDescription() == description
        entity.getCt() == ct
        entity.getUt() == ut

        where:
        id          | name       | description        | ct          | ut
        10000001L   | "Event 1"  | "Description 1"   | 1234567890  | 1234567890
        10000002L   | "Event 2"  | null              | 1234567891  | 1234567891
        10000003L   | null       | "Description 3"   | 1234567892  | 1234567892
    }

    @Unroll
    def "test EventEntity toString - id: #id, name: #name"() {
        given: "create EventEntity"
        def entity = new EventEntity()
        entity.setId(id)
        entity.setName(name)
        entity.setDescription("Description")
        entity.setCt(1234567890)
        entity.setUt(1234567890)

        when: "call toString"
        def result = entity.toString()

        then: "should contain id and name"
        result != null
        result.contains(String.valueOf(id))
        result.contains(name)

        where:
        id          | name
        10000001L   | "Event 1"
        10000002L   | "Event 2"
    }

    @Unroll
    def "test EventEntity equals and hashCode - id: #id1, name: #name1 vs id: #id2, name: #name2, shouldEqual: #shouldEqual"() {
        given: "create two EventEntities"
        def entity1 = new EventEntity()
        entity1.setId(id1)
        entity1.setName(name1)
        entity1.setDescription("Description")
        entity1.setCt(1234567890)
        entity1.setUt(1234567890)

        def entity2 = new EventEntity()
        entity2.setId(id2)
        entity2.setName(name2)
        entity2.setDescription("Description")
        entity2.setCt(1234567890)
        entity2.setUt(1234567890)

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
        id1         | name1      | id2         | name2      | shouldEqual
        10000001L   | "Event 1"  | 10000001L   | "Event 1"  | true
        10000001L   | "Event 1"  | 10000002L   | "Event 1"  | false
        10000001L   | "Event 1"  | 10000001L   | "Event 2"  | false
    }

    @Unroll
    def "test EventEntity default values"() {
        when: "create EventEntity with default constructor"
        def entity = new EventEntity()

        then: "should have null or default values"
        entity.getId() == null
        entity.getName() == null
        entity.getDescription() == null
        entity.getCt() == null
        entity.getUt() == null
    }
}

