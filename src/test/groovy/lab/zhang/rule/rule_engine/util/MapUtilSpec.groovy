package lab.zhang.rule.rule_engine.util

import org.apache.commons.lang3.tuple.Pair
import spock.lang.Specification
import spock.lang.Unroll

/**
 * MapUtil unit test
 *
 * @author Rongjin Zhang
 */
class MapUtilSpec extends Specification {

    def "test getOneEntry - should return null for null map"() {
        when: "get one entry from null map"
        def result = MapUtil.getOneEntry(null)

        then: "should return null"
        result == null
    }

    def "test getOneEntry - should return null for empty map"() {
        when: "get one entry from empty map"
        def result = MapUtil.getOneEntry([:])

        then: "should return null"
        result == null
    }

    @Unroll
    def "test getOneEntry - should return first entry for single entry map - key: #key, value: #value"() {
        given: "a map with single entry"
        def map = [(key): value]

        when: "get one entry"
        def result = MapUtil.getOneEntry(map)

        then: "should return correct pair"
        result != null
        result.getLeft() == key
        result.getRight() == value

        where:
        key   | value
        "key1" | "value1"
        1L     | "value1"
        "key1" | 100
        1      | 2
        null   | "value1"
        "key1" | null
    }

    @Unroll
    def "test getOneEntry - should return first entry for multiple entries map - mapSize: #mapSize"() {
        given: "a map with multiple entries"
        def map = [:]
        for (int i = 0; i < mapSize; i++) {
            map["key${i}"] = "value${i}"
        }

        when: "get one entry"
        def result = MapUtil.getOneEntry(map)

        then: "should return first entry"
        result != null
        result.getLeft() != null
        result.getRight() != null
        map.containsKey(result.getLeft())
        map.get(result.getLeft()) == result.getRight()

        where:
        mapSize << [2, 3, 5, 10, 100]
    }

    def "test getOneEntry - should return first entry consistently for same map"() {
        given: "a map with multiple entries"
        def map = [
                "key1": "value1",
                "key2": "value2",
                "key3": "value3"
        ]

        when: "get one entry multiple times"
        def result1 = MapUtil.getOneEntry(map)
        def result2 = MapUtil.getOneEntry(map)
        def result3 = MapUtil.getOneEntry(map)

        then: "should return same entry (first entry)"
        result1 != null
        result2 != null
        result3 != null
        result1.getLeft() == result2.getLeft()
        result2.getLeft() == result3.getLeft()
        result1.getRight() == result2.getRight()
        result2.getRight() == result3.getRight()
    }

    def "test getOneEntry - should work with different key types"() {
        given: "maps with different key types"
        def stringKeyMap = ["key1": "value1", "key2": "value2"]
        def longKeyMap = [1L: "value1", 2L: "value2"]
        def intKeyMap = [1: "value1", 2: "value2"]

        when: "get one entry from each map"
        def stringResult = MapUtil.getOneEntry(stringKeyMap)
        def longResult = MapUtil.getOneEntry(longKeyMap)
        def intResult = MapUtil.getOneEntry(intKeyMap)

        then: "should return correct pairs with correct key types"
        stringResult != null
        stringResult.getLeft() instanceof String
        stringResult.getRight() == "value1"

        longResult != null
        longResult.getLeft() instanceof Long
        longResult.getRight() == "value1"

        intResult != null
        intResult.getLeft() instanceof Integer
        intResult.getRight() == "value1"
    }

    def "test getOneEntry - should work with different value types"() {
        given: "maps with different value types"
        def stringValueMap = ["key1": "value1", "key2": "value2"]
        def intValueMap = ["key1": 100, "key2": 200]
        def longValueMap = ["key1": 100L, "key2": 200L]
        def objectValueMap = ["key1": new Object(), "key2": new Object()]

        when: "get one entry from each map"
        def stringResult = MapUtil.getOneEntry(stringValueMap)
        def intResult = MapUtil.getOneEntry(intValueMap)
        def longResult = MapUtil.getOneEntry(longValueMap)
        def objectResult = MapUtil.getOneEntry(objectValueMap)

        then: "should return correct pairs with correct value types"
        stringResult != null
        stringResult.getRight() instanceof String
        stringResult.getRight() == "value1"

        intResult != null
        intResult.getRight() instanceof Integer
        intResult.getRight() == 100

        longResult != null
        longResult.getRight() instanceof Long
        longResult.getRight() == 100L

        objectResult != null
        objectResult.getRight() instanceof Object
    }

    def "test getOneEntry - should work with null key"() {
        given: "a map with null key"
        def map = [null: "value1", "key2": "value2"]

        when: "get one entry"
        def result = MapUtil.getOneEntry(map)

        then: "should return entry (may have null key)"
        result != null
        result.getRight() == "value1"
        // Note: The key might be null or "key2" depending on map iteration order
        // But the value should be one of the values in the map
        map.containsValue(result.getRight())
    }

    def "test getOneEntry - should work with null value"() {
        given: "a map with null value"
        def map = ["key1": null, "key2": "value2"]

        when: "get one entry"
        def result = MapUtil.getOneEntry(map)

        then: "should return entry (may have null value)"
        result != null
        result.getLeft() != null
        // Note: The value might be null or "value2" depending on map iteration order
        // But the key should be one of the keys in the map
        map.containsKey(result.getLeft())
    }

    def "test getOneEntry - should return Pair instance"() {
        given: "a map with entries"
        def map = ["key1": "value1", "key2": "value2"]

        when: "get one entry"
        def result = MapUtil.getOneEntry(map)

        then: "should return Pair instance"
        result != null
        result instanceof Pair
    }
}

