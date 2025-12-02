package lab.zhang.rule.rule_engine.util

import lab.zhang.rule.rule_engine.enums.ContentTypeEnum
import lab.zhang.rule.rule_engine.enums.RuleStatusEnum
import spock.lang.Specification
import spock.lang.Unroll

/**
 * EnumUtil unit test
 *
 * @author Rongjin Zhang
 */
class EnumUtilSpec extends Specification {

    // ========== fromId() tests ==========

    @Unroll
    def "test fromId - should return enum value for valid ID - enumClass: #enumClass.simpleName, id: #id, expected: #expected"() {
        when: "get enum value by ID"
        def result = EnumUtil.fromId(enumClass, id)

        then: "should return correct enum value"
        result == expected

        where:
        enumClass            | id | expected
        RuleStatusEnum.class | 0  | RuleStatusEnum.OFFLINE
        RuleStatusEnum.class | 1  | RuleStatusEnum.TEST
        RuleStatusEnum.class | 2  | RuleStatusEnum.GRAY
        RuleStatusEnum.class | 3  | RuleStatusEnum.AB_TEST
        RuleStatusEnum.class | 4  | RuleStatusEnum.FULL
        ContentTypeEnum.class | 0 | ContentTypeEnum.EXPRESSION
        ContentTypeEnum.class | 1 | ContentTypeEnum.API_QUERY
        ContentTypeEnum.class | 2 | ContentTypeEnum.SQL_QUERY
        ContentTypeEnum.class | 3 | ContentTypeEnum.SCRIPT
    }

    def "test fromId - should return null for null ID"() {
        when: "get enum value with null ID"
        def result = EnumUtil.fromId(RuleStatusEnum.class, null)

        then: "should return null"
        result == null
    }

    def "test fromId - should return null for null enumClass"() {
        when: "get enum value with null enumClass"
        def result = EnumUtil.fromId(null, 1)

        then: "should return null"
        result == null
    }

    def "test fromId - should return null for invalid ID"() {
        when: "get enum value with invalid ID"
        def result = EnumUtil.fromId(RuleStatusEnum.class, 999)

        then: "should return null"
        result == null
    }

    def "test fromId - should return null for negative ID"() {
        when: "get enum value with negative ID"
        def result = EnumUtil.fromId(RuleStatusEnum.class, -1)

        then: "should return null"
        result == null
    }

    // ========== getId() tests ==========

    @Unroll
    def "test getId - should return ID for enum value - enumValue: #enumValue, expectedId: #expectedId"() {
        when: "get ID from enum value"
        def result = EnumUtil.getId(enumValue)

        then: "should return correct ID"
        result == expectedId

        where:
        enumValue                    | expectedId
        RuleStatusEnum.OFFLINE      | 0
        RuleStatusEnum.TEST         | 1
        RuleStatusEnum.GRAY         | 2
        RuleStatusEnum.AB_TEST      | 3
        RuleStatusEnum.FULL         | 4
        ContentTypeEnum.EXPRESSION  | 0
        ContentTypeEnum.API_QUERY   | 1
        ContentTypeEnum.SQL_QUERY   | 2
        ContentTypeEnum.SCRIPT      | 3
    }

    def "test getId - should return null for null enumValue"() {
        when: "get ID from null enum value"
        def result = EnumUtil.getId(null)

        then: "should return null"
        result == null
    }

    // ========== getEnumInfo() tests ==========

    def "test getEnumInfo - should return enum info for RuleStatusEnum"() {
        when: "get enum info for RuleStatusEnum"
        def result = EnumUtil.getEnumInfo("lab.zhang.rule.rule_engine.enums.RuleStatusEnum")

        then: "should return list with all enum values"
        result != null
        result.size() == 5
        result[0].name == "OFFLINE"
        result[0].value == 0
        result[1].name == "TEST"
        result[1].value == 1
        result[2].name == "GRAY"
        result[2].value == 2
        result[3].name == "AB_TEST"
        result[3].value == 3
        result[4].name == "FULL"
        result[4].value == 4

        and: "each item should have name and value"
        result.every { it.containsKey("name") && it.containsKey("value") }
    }

    def "test getEnumInfo - should return enum info for ContentTypeEnum"() {
        when: "get enum info for ContentTypeEnum"
        def result = EnumUtil.getEnumInfo("lab.zhang.rule.rule_engine.enums.ContentTypeEnum")

        then: "should return list with all enum values"
        result != null
        result.size() == 4
        result[0].name == "EXPRESSION"
        result[0].value == 0
        result[1].name == "API_QUERY"
        result[1].value == 1
        result[2].name == "SQL_QUERY"
        result[2].value == 2
        result[3].name == "SCRIPT"
        result[3].value == 3

        and: "each item should have name and value"
        result.every { it.containsKey("name") && it.containsKey("value") }
    }

    def "test getEnumInfo - should return empty list for non-existent class"() {
        when: "get enum info for non-existent class"
        def result = EnumUtil.getEnumInfo("lab.zhang.rule.rule_engine.enums.NonExistentEnum")

        then: "should return empty list"
        result != null
        result.isEmpty()
    }

    def "test getEnumInfo - should return empty list for non-enum class"() {
        when: "get enum info for non-enum class"
        def result = EnumUtil.getEnumInfo("java.lang.String")

        then: "should return empty list"
        result != null
        result.isEmpty()
    }

    def "test getEnumInfo - should return empty list for invalid class name"() {
        when: "get enum info for invalid class name"
        def result = EnumUtil.getEnumInfo("InvalidClassName")

        then: "should return empty list"
        result != null
        result.isEmpty()
    }

    // ========== getEnumInfoMap() tests ==========

    def "test getEnumInfoMap - should return map for single enum class"() {
        when: "get enum info map for single class"
        def result = EnumUtil.getEnumInfoMap("RuleStatusEnum")

        then: "should return map with one entry"
        result != null
        result.size() == 1
        result.containsKey("RuleStatusEnum")
        result["RuleStatusEnum"].size() == 5
        result["RuleStatusEnum"][0].name == "OFFLINE"
        result["RuleStatusEnum"][0].value == 0
    }

    def "test getEnumInfoMap - should return map for multiple enum classes"() {
        when: "get enum info map for multiple classes"
        def result = EnumUtil.getEnumInfoMap("RuleStatusEnum,ContentTypeEnum")

        then: "should return map with multiple entries"
        result != null
        result.size() == 2
        result.containsKey("RuleStatusEnum")
        result.containsKey("ContentTypeEnum")
        result["RuleStatusEnum"].size() == 5
        result["ContentTypeEnum"].size() == 4
    }

    def "test getEnumInfoMap - should handle short class names"() {
        when: "get enum info map with short class names"
        def result = EnumUtil.getEnumInfoMap("RuleStatus,ContentTypeEnum")

        then: "should return map with resolved class names"
        result != null
        result.size() == 2
        result.containsKey("RuleStatus")
        result.containsKey("ContentTypeEnum")
    }

    def "test getEnumInfoMap - should handle full class names"() {
        when: "get enum info map with full class names"
        def result = EnumUtil.getEnumInfoMap("lab.zhang.rule.rule_engine.enums.RuleStatusEnum")

        then: "should return map with full class name as key"
        result != null
        result.size() == 1
        result.containsKey("lab.zhang.rule.rule_engine.enums.RuleStatusEnum")
    }

    def "test getEnumInfoMap - should handle spaces in class names"() {
        when: "get enum info map with spaces"
        def result = EnumUtil.getEnumInfoMap("RuleStatusEnum , ContentTypeEnum ")

        then: "should trim spaces and return map"
        result != null
        result.size() == 2
        result.containsKey("RuleStatusEnum")
        result.containsKey("ContentTypeEnum")
    }

    def "test getEnumInfoMap - should return empty map for null input"() {
        when: "get enum info map with null input"
        def result = EnumUtil.getEnumInfoMap(null)

        then: "should return empty map"
        result != null
        result.isEmpty()
    }

    def "test getEnumInfoMap - should return empty map for empty string"() {
        when: "get enum info map with empty string"
        def result = EnumUtil.getEnumInfoMap("")

        then: "should return empty map"
        result != null
        result.isEmpty()
    }

    def "test getEnumInfoMap - should return empty map for whitespace only"() {
        when: "get enum info map with whitespace only"
        def result = EnumUtil.getEnumInfoMap("   ")

        then: "should return empty map"
        result != null
        result.isEmpty()
    }

    def "test getEnumInfoMap - should skip non-existent classes"() {
        when: "get enum info map with non-existent class"
        def result = EnumUtil.getEnumInfoMap("RuleStatusEnum,NonExistentEnum")

        then: "should only return existing classes"
        result != null
        result.size() == 1
        result.containsKey("RuleStatusEnum")
        !result.containsKey("NonExistentEnum")
    }

    def "test getEnumInfoMap - should handle empty class names in comma-separated list"() {
        when: "get enum info map with empty class names"
        def result = EnumUtil.getEnumInfoMap("RuleStatusEnum,,ContentTypeEnum")

        then: "should skip empty class names"
        result != null
        result.size() == 2
        result.containsKey("RuleStatusEnum")
        result.containsKey("ContentTypeEnum")
    }

    def "test getEnumInfoMap - should handle backward compatibility aliases"() {
        when: "get enum info map with backward compatibility aliases"
        def result = EnumUtil.getEnumInfoMap("RuleStatus,ItemType")

        then: "should resolve aliases correctly"
        result != null
        result.size() == 2
        result.containsKey("RuleStatus")
        result.containsKey("ItemType")
    }

    // ========== Integration tests ==========

    @Unroll
    def "test fromId and getId - should be inverse operations - enumValue: #enumValue"() {
        when: "get ID and then get enum value back"
        def id = EnumUtil.getId(enumValue)
        def result = EnumUtil.fromId(RuleStatusEnum.class, id)

        then: "should get original enum value"
        result == enumValue

        where:
        enumValue << [RuleStatusEnum.OFFLINE, RuleStatusEnum.TEST, RuleStatusEnum.GRAY, RuleStatusEnum.AB_TEST, RuleStatusEnum.FULL]
    }

    def "test getEnumInfo and fromId - should be consistent"() {
        when: "get enum info and verify fromId works"
        def enumInfo = EnumUtil.getEnumInfo("lab.zhang.rule.rule_engine.enums.RuleStatusEnum")

        then: "each enum info should correspond to a valid enum value"
        enumInfo.size() > 0
        enumInfo.every { item ->
            def enumValue = EnumUtil.fromId(RuleStatusEnum.class, item.value as Integer)
            enumValue != null && enumValue.name() == item.name
        }
    }

    def "test getEnumInfoMap and getEnumInfo - should be consistent"() {
        when: "get enum info map and compare with getEnumInfo"
        def mapResult = EnumUtil.getEnumInfoMap("RuleStatusEnum")
        def listResult = EnumUtil.getEnumInfo("lab.zhang.rule.rule_engine.enums.RuleStatusEnum")

        then: "should have same content"
        mapResult["RuleStatusEnum"].size() == listResult.size()
        mapResult["RuleStatusEnum"].size() == 5
        mapResult["RuleStatusEnum"][0].name == listResult[0].name
        mapResult["RuleStatusEnum"][0].value == listResult[0].value
        mapResult["RuleStatusEnum"][1].name == listResult[1].name
        mapResult["RuleStatusEnum"][1].value == listResult[1].value
        mapResult["RuleStatusEnum"][2].name == listResult[2].name
        mapResult["RuleStatusEnum"][2].value == listResult[2].value
        mapResult["RuleStatusEnum"][3].name == listResult[3].name
        mapResult["RuleStatusEnum"][3].value == listResult[3].value
        mapResult["RuleStatusEnum"][4].name == listResult[4].name
        mapResult["RuleStatusEnum"][4].value == listResult[4].value
    }
}

