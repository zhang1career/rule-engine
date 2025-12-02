package lab.zhang.rule.rule_engine.dto

import lab.zhang.rule.rule_engine.pojo.dto.EventDTO
import lab.zhang.rule.rule_engine.pojo.qo.EventQO
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

/**
 * EventDTO unit test
 */
class EventDTOSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    @Unroll
    def "test EventDTO builder - id: #id, name: #name, description: #description"() {
        when: "create EventDTO using builder"
        def dto = EventDTO.builder()
                .id(id)
                .name(name)
                .description(description)
                .build()

        then: "should create DTO correctly"
        dto != null
        dto.id == id
        dto.name == name
        dto.description == description

        where:
        id          | name       | description
        10000001L   | "Event 1"  | "Description 1"
        10000002L   | "Event 2"  | null
        10000003L   | null       | "Description 3"
    }

    @Unroll
    def "test EventDTO validation - Create group - id: #id, name: #name, description: #description, shouldPass: #shouldPass"() {
        given: "create EventDTO"
        def qo = EventQO.builder()
                .id(id)
                .name(name)
                .description(description)
                .build()

        when: "validate with Create group"
        Set<ConstraintViolation<EventQO>> violations = validator.validate(qo, EventQO.Create.class)

        then: "validation should pass or fail as expected"
        (violations.isEmpty()) == shouldPass

        where:
        id          | name       | description        | shouldPass
        10000001L   | "Event 1"  | "Description 1"   | true
        10000002L   | "Event 2"  | null              | true
        null        | "Event 1" | "Description"     | false  // id is null
        10000001L   | null       | "Description"     | false  // name is null
        10000001L   | ""         | "Description"     | false  // name is blank
        10000001L   | "   "      | "Description"     | false  // name is blank
        -1L         | "Event 1" | "Description"     | false  // id is negative
        0L          | "Event 1" | "Description"      | false  // id is zero
        10000001L   | "Event 1" | "a" * 251          | false  // description too long
        10000001L   | "a" * 101 | "Description"     | false  // name too long
    }

    @Unroll
    def "test EventDTO validation - Update group - name: #name, description: #description, shouldPass: #shouldPass"() {
        given: "create EventDTO"
        def qo = EventQO.builder()
                .id(10000001L)
                .name(name)
                .description(description)
                .build()

        when: "validate with Update group"
        Set<ConstraintViolation<EventQO>> violations = validator.validate(qo, EventQO.Update.class)

        then: "validation should pass or fail as expected"
        (violations.isEmpty()) == shouldPass

        where:
        name       | description        | shouldPass
        "Event 1"  | "Description 1"   | true
        null       | "Description"     | true   // name is optional in update
        ""         | "Description"     | true   // name is optional in update
        "Event 1"  | null               | true   // description is optional
        "Event 1"  | "a" * 251          | false  // description too long
        "a" * 101  | "Description"     | false  // name too long
    }

    @Unroll
    def "test EventDTO getter and setter - id: #id, name: #name, description: #description"() {
        given: "create EventDTO"
        def dto = new EventDTO()

        when: "set values"
        dto.setId(id)
        dto.setName(name)
        dto.setDescription(description)

        then: "should get correct values"
        dto.getId() == id
        dto.getName() == name
        dto.getDescription() == description

        where:
        id          | name       | description
        10000001L   | "Event 1"  | "Description 1"
        10000002L   | "Event 2"  | null
        10000003L   | null       | "Description 3"
    }

    @Unroll
    def "test EventDTO toString - id: #id, name: #name"() {
        given: "create EventDTO"
        def dto = EventDTO.builder()
                .id(id)
                .name(name)
                .description("Description")
                .build()

        when: "call toString"
        def result = dto.toString()

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
    def "test EventDTO equals and hashCode - id: #id1, name: #name1 vs id: #id2, name: #name2, shouldEqual: #shouldEqual"() {
        given: "create two EventDTOs"
        def dto1 = EventDTO.builder()
                .id(id1)
                .name(name1)
                .description("Description")
                .build()
        def dto2 = EventDTO.builder()
                .id(id2)
                .name(name2)
                .description("Description")
                .build()

        when: "compare DTOs"
        def equals = dto1.equals(dto2)
        def hashCode1 = dto1.hashCode()
        def hashCode2 = dto2.hashCode()

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
}

