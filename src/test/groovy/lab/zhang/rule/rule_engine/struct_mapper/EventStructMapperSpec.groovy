package lab.zhang.rule.rule_engine.struct_mapper


import lab.zhang.rule.rule_engine.model.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification
import spock.lang.Unroll

/**
 * EventStructMapper unit test
 */
@SpringBootTest
@ActiveProfiles("test")
class EventStructMapperSpec extends Specification {

    @Autowired
    EventStructMapper eventStructMapper

    @Unroll
    def "test entityToDTO - id: #id, name: #name, description: #description"() {
        given: "create Event"
        def entity = new Event()
        entity.setId(id)
        entity.setName(name)
        entity.setDescription(description)

        when: "convert to EventDTO"
        def dto = eventStructMapper.entityToDTO(entity)

        then: "should convert correctly"
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
    def 'test entityToDTO - null entity'() {
        when: "convert null entity to EventDTO"
        def dto = eventStructMapper.entityToDTO(null)

        then: "should return null"
        dto == null
    }

    @Unroll
    def 'test entityToDTO - entity with all fields'() {
        given: "create Event with all fields"
        def entity = new Event()
        entity.setId(10000001L)
        entity.setName("Test Event")
        entity.setDescription("Test Description")

        when: "convert to EventDTO"
        def dto = eventStructMapper.entityToDTO(entity)

        then: "should map all fields correctly"
        dto != null
        dto.id == 10000001L
        dto.name == "Test Event"
        dto.description == "Test Description"
    }

    @Unroll
    def 'test entityToDTO - multiple entities'() {
        given: "create multiple EventEntities"
        def entities = []
        3.times { i ->
            def entity = new Event()
            entity.setId(10000001L + i)
            entity.setName("Event ${i + 1}")
            entity.setDescription("Description ${i + 1}")
            entities.add(entity)
        }

        when: "convert all entities to DTOs"
        def dtos = entities.collect { eventStructMapper.entityToDTO(it) }

        then: "should convert all correctly"
        dtos.size() == 3
        dtos.eachWithIndex { dto, index ->
            assert dto != null
            assert dto.id == 10000001L + index
            assert dto.name == "Event ${index + 1}"
        }
    }
}

