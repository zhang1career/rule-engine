package lab.zhang.rule.rule_engine.util;

/**
 * Hash utility class
 * Provides murmur-hash algorithm for user ID hashing
 * 
 * @author Rongjin Zhang
 */
public class HashUtil {
    
    /**
     * MurmurHash3 32-bit hash function
     * This is a simplified implementation for hashing user IDs
     * 
     * @param data the data to hash (as string)
     * @return hash value as string
     */
    public static String murmurHash3(String data) {
        if (data == null) {
            return "0";
        }
        
        byte[] bytes = data.getBytes();
        int hash = murmurHash3_32(bytes, 0, bytes.length, 0);
        
        // Convert to positive integer string
        return String.valueOf(hash & 0x7FFFFFFF);
    }
    
    /**
     * MurmurHash3 32-bit implementation
     * 
     * @param data byte array
     * @param offset offset
     * @param length length
     * @param seed seed value
     * @return 32-bit hash value
     */
    private static int murmurHash3_32(byte[] data, int offset, int length, int seed) {
        int c1 = 0xcc9e2d51;
        int c2 = 0x1b873593;
        int r1 = 15;
        int r2 = 13;
        int m = 5;
        int n = 0xe6546b64;
        
        int hash = seed;
        int roundedEnd = offset + (length & 0xfffffffc); // round down to 4 byte block
        
        for (int i = offset; i < roundedEnd; i += 4) {
            // little endian load order
            int k = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | 
                    ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
            k *= c1;
            k = (k << r1) | (k >>> (32 - r1)); // ROTL32(k, r1)
            k *= c2;
            hash ^= k;
            hash = (hash << r2) | (hash >>> (32 - r2)); // ROTL32(hash, r2)
            hash = hash * m + n;
        }
        
        // tail
        int k1 = 0;
        switch (length & 0x03) {
            case 3:
                k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
                // fall through
            case 2:
                k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
                // fall through
            case 1:
                k1 ^= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = (k1 << r1) | (k1 >>> (32 - r1));
                k1 *= c2;
                hash ^= k1;
        }
        
        // finalization
        hash ^= length;
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        
        return hash;
    }
    
    /**
     * Convert hash string to integer in range [min, max] (inclusive)
     * 
     * @param hashStr hash string
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @return integer in range [min, max]
     * @throws IllegalArgumentException if min > max or min < 0
     */
    public static int hashToIntRange(String hashStr, int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must be <= max (" + max + ")");
        }
        if (min < 0) {
            throw new IllegalArgumentException("min (" + min + ") must be >= 0");
        }
        
        if (hashStr == null || hashStr.isEmpty()) {
            return min;
        }
        
        try {
            long hashLong = Long.parseLong(hashStr);
            // Map to [min, max]
            int range = max - min + 1;
            int result = (int) ((Math.abs(hashLong) % range) + min);
            return result;
        } catch (NumberFormatException e) {
            // Fallback: use string hashCode
            int hashCode = hashStr.hashCode();
            int range = max - min + 1;
            int result = (Math.abs(hashCode) % range) + min;
            return result;
        }
    }
}

