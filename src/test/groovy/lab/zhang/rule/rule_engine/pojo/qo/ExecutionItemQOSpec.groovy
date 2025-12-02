package lab.zhang.rule.rule_engine.pojo.qo

import lab.zhang.rule.rule_engine.enums.ExecutionItemTypeEnum
import spock.lang.Specification
import spock.lang.Unroll

import javax.validation.ConstraintViolation
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

/**
 * ExecutionItemQO unit test
 */
class ExecutionItemQOSpec extends Specification {

    Validator validator

    def setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory()
        validator = factory.getValidator()
    }

    @Unroll
    def "test ExecutionItemQO builder - itemType: #itemType, itemId: #itemId"() {
        when: "create ExecutionItemQO using builder"
        def qo = ExecutionItemQO.builder()
                .itemType(itemType)
                .itemId(itemId)
                .build()

        then: "should create QO correctly"
        qo != null
        qo.itemType == itemType
        qo.itemId == itemId

        where:
        itemType                          | itemId
        ExecutionItemTypeEnum.RULE.getId() | 10000001L
        ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000002L
    }

    @Unroll
    def "test ExecutionItemQO validation - itemType: #itemType, itemId: #itemId, shouldPass: #shouldPass"() {
        given: "create ExecutionItemQO"
        def qo = ExecutionItemQO.builder()
                .itemType(itemType)
                .itemId(itemId)
                .build()

        when: "validate"
        Set<ConstraintViolation<ExecutionItemQO>> violations = validator.validate(qo)

        then: "validation should pass or fail as expected"
        (violations.isEmpty()) == shouldPass

        where:
        itemType                          | itemId      | shouldPass
        ExecutionItemTypeEnum.RULE.getId() | 10000001L   | true
        ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000002L | true
        null                               | 10000001L   | false  // itemType is null
        ExecutionItemTypeEnum.RULE.getId() | null        | false  // itemId is null
        ExecutionItemTypeEnum.RULE.getId() | -1L         | false  // itemId is negative
        ExecutionItemTypeEnum.RULE.getId() | 0L          | false  // itemId is zero
        2                                 | 10000001L   | false  // invalid itemType
    }

    @Unroll
    def "test ExecutionItemQO getter and setter - itemType: #itemType, itemId: #itemId"() {
        given: "create ExecutionItemQO"
        def qo = new ExecutionItemQO()

        when: "set values"
        qo.setItemType(itemType)
        qo.setItemId(itemId)

        then: "should get correct values"
        qo.getItemType() == itemType
        qo.getItemId() == itemId

        where:
        itemType                          | itemId
        ExecutionItemTypeEnum.RULE.getId() | 10000001L
        ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000002L
    }

    @Unroll
    def "test ExecutionItemQO toString - itemType: #itemType, itemId: #itemId"() {
        given: "create ExecutionItemQO"
        def qo = ExecutionItemQO.builder()
                .itemType(itemType)
                .itemId(itemId)
                .build()

        when: "call toString"
        def result = qo.toString()

        then: "should contain itemType and itemId"
        result != null
        result.contains(String.valueOf(itemType))
        result.contains(String.valueOf(itemId))

        where:
        itemType                          | itemId
        ExecutionItemTypeEnum.RULE.getId() | 10000001L
        ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000002L
    }

    @Unroll
    def "test ExecutionItemQO equals and hashCode - itemType: #itemType1, itemId: #itemId1 vs itemType: #itemType2, itemId: #itemId2, shouldEqual: #shouldEqual"() {
        given: "create two ExecutionItemQOs"
        def qo1 = ExecutionItemQO.builder()
                .itemType(itemType1)
                .itemId(itemId1)
                .build()
        def qo2 = ExecutionItemQO.builder()
                .itemType(itemType2)
                .itemId(itemId2)
                .build()

        when: "compare QOs"
        def equals = qo1.equals(qo2)
        def hashCode1 = qo1.hashCode()
        def hashCode2 = qo2.hashCode()

        then: "should equal or not as expected"
        equals == shouldEqual
        if (shouldEqual) {
            hashCode1 == hashCode2
        }

        where:
        itemType1                         | itemId1     | itemType2                         | itemId2     | shouldEqual
        ExecutionItemTypeEnum.RULE.getId() | 10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000001L   | true
        ExecutionItemTypeEnum.RULE.getId() | 10000001L   | ExecutionItemTypeEnum.RULE.getId() | 10000002L   | false
        ExecutionItemTypeEnum.RULE.getId() | 10000001L   | ExecutionItemTypeEnum.RULE_GROUP.getId() | 10000001L   | false
    }
}

