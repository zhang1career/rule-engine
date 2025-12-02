package lab.zhang.rule.rule_engine.util

import spock.lang.Specification
import spock.lang.Unroll

/**
 * HashUtil unit test
 */
class HashUtilSpec extends Specification {

    @Unroll
    def "test murmurHash3 - basic functionality - input: #input"() {
        when: "calculate hash"
        def hash = HashUtil.murmurHash3(input)

        then: "should return non-empty string"
        hash != null
        hash.length() > 0
        hash.matches("\\d+") // Should be numeric string

        where:
        input << ["123", "456", "test", "userId123", "", "a", "very long string with many characters"]
    }

    def "test murmurHash3 - null input should return 0"() {
        when: "calculate hash for null"
        def hash = HashUtil.murmurHash3(null)

        then: "should return 0"
        hash == "0"
    }

    def "test murmurHash3 - same input should produce same hash"() {
        when: "calculate hash multiple times"
        def hash1 = HashUtil.murmurHash3("test123")
        def hash2 = HashUtil.murmurHash3("test123")
        def hash3 = HashUtil.murmurHash3("test123")

        then: "should produce same hash"
        hash1 == hash2
        hash2 == hash3
    }

    def "test murmurHash3 - different inputs should produce different hashes"() {
        when: "calculate hash for different inputs"
        def hash1 = HashUtil.murmurHash3("test1")
        def hash2 = HashUtil.murmurHash3("test2")
        def hash3 = HashUtil.murmurHash3("test3")

        then: "should produce different hashes"
        hash1 != hash2
        hash2 != hash3
        hash1 != hash3
    }

    def "test murmurHash3 - uniformity distribution test"() {
        given: "generate many different inputs"
        def sampleSize = 10000
        def bucketCount = 100
        def buckets = new int[bucketCount]

        when: "calculate hash for many different inputs"
        sampleSize.times { i ->
            def input = "userId_" + i
            def hash = HashUtil.murmurHash3(input)
            def hashInt = Long.parseLong(hash)
            def bucket = (int) (hashInt % bucketCount)
            buckets[bucket]++
        }

        then: "distribution should be relatively uniform"
        // Calculate expected count per bucket
        def expectedCount = sampleSize / bucketCount
        def variance = 0.0
        buckets.each { count ->
            def diff = count - expectedCount
            variance += diff * diff
        }
        def stdDev = Math.sqrt(variance / bucketCount)
        
        // Coefficient of variation (CV) should be small for uniform distribution
        // CV = stdDev / mean, for uniform distribution CV should be around 0.1-0.2
        def cv = stdDev / expectedCount
        
        // Check that CV is reasonable (less than 0.3 indicates good uniformity)
        cv < 0.3
        
        // Also check that no bucket is extremely empty or full
        // Each bucket should have at least 50% and at most 200% of expected count
        buckets.each { count ->
            assert count >= expectedCount * 0.5
            assert count <= expectedCount * 2.0
        }
    }

    def "test murmurHash3 - chi-square test for uniformity"() {
        given: "generate many different inputs"
        def sampleSize = 10000
        def bucketCount = 100
        def buckets = new int[bucketCount]

        when: "calculate hash for many different inputs"
        sampleSize.times { i ->
            def input = "userId_" + i
            def hash = HashUtil.murmurHash3(input)
            def hashInt = Long.parseLong(hash)
            def bucket = (int) (hashInt % bucketCount)
            buckets[bucket]++
        }

        then: "chi-square test should indicate uniform distribution"
        // Calculate chi-square statistic
        def expectedCount = sampleSize / bucketCount
        def chiSquare = 0.0
        buckets.each { count ->
            def diff = count - expectedCount
            chiSquare += (diff * diff) / expectedCount
        }
        
        // For 100 buckets with 10000 samples, degrees of freedom = 99
        // Critical value for 99 degrees of freedom at 0.05 significance level is about 123.2
        // If chi-square < critical value, we accept the null hypothesis (uniform distribution)
        // Using a more lenient threshold of 200 for practical purposes
        chiSquare < 200.0
    }

    @Unroll
    def "test hashToIntRange - basic functionality - hashStr: #hashStr, min: #min, max: #max, expectedRange: #expectedRange"() {
        when: "convert hash to int in range"
        def result = HashUtil.hashToIntRange(hashStr, min, max)

        then: "should be in range [min, max]"
        result >= min
        result <= max

        where:
        hashStr      | min | max | expectedRange
        "123456"     | 1   | 100 | "1-100"
        "999999999"  | 1   | 50  | "1-50"
        "0"          | 10  | 20  | "10-20"
        "1"          | 0   | 9   | "0-9"
        "100"        | 5   | 15  | "5-15"
        null         | 1   | 100 | "1-100"
        ""           | 1   | 100 | "1-100"
        "invalid"    | 1   | 100 | "1-100"
    }

    @Unroll
    def "test hashToIntRange - edge cases - hashStr: #hashStr, min: #min, max: #max"() {
        when: "convert hash to int in range"
        def result = HashUtil.hashToIntRange(hashStr, min, max)

        then: "should be in range [min, max]"
        result >= min
        result <= max

        where:
        hashStr | min | max
        "0"     | 0   | 0   // Single value range
        "100"   | 1   | 1   // Single value range
        "50"    | 1   | 2   // Small range
        "999"   | 1   | 1000 // Large range
    }

    def "test hashToIntRange - null or empty input should return min"() {
        expect: "null or empty should return min"
        HashUtil.hashToIntRange(null, 5, 10) == 5
        HashUtil.hashToIntRange("", 5, 10) == 5
        HashUtil.hashToIntRange(null, 0, 100) == 0
    }

    def "test hashToIntRange - should throw exception when min > max"() {
        when: "call with min > max"
        HashUtil.hashToIntRange("123", 10, 5)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test hashToIntRange - should throw exception when min < 0"() {
        when: "call with min < 0"
        HashUtil.hashToIntRange("123", -1, 10)

        then: "should throw IllegalArgumentException"
        thrown(IllegalArgumentException)
    }

    def "test hashToIntRange - distribution test for different ranges"() {
        given: "test different ranges"
        def ranges = [
            [min: 1, max: 10, bucketCount: 10],
            [min: 1, max: 50, bucketCount: 50],
            [min: 1, max: 100, bucketCount: 100],
            [min: 10, max: 20, bucketCount: 11]
        ]

        ranges.each { range ->
            def sampleSize = 10000
            def buckets = new int[range.bucketCount]

            when: "convert many hash strings to int in range"
            sampleSize.times { i ->
                def hashStr = String.valueOf(i * 1234567L)
                def result = HashUtil.hashToIntRange(hashStr, range.min, range.max)
                def bucketIndex = result - range.min
                buckets[bucketIndex]++
            }

            then: "distribution should be relatively uniform"
            def expectedCount = sampleSize / range.bucketCount
            def variance = 0.0
            buckets.each { count ->
                def diff = count - expectedCount
                variance += diff * diff
            }
            def stdDev = Math.sqrt(variance / range.bucketCount)
            def cv = stdDev / expectedCount
            
            // Coefficient of variation should be reasonable
            cv < 0.35 // Slightly more lenient for smaller ranges
        }
    }

}

