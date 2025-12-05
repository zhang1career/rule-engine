package lab.zhang.rule.rule_engine.util;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

public class MapUtil {

    public static <K, V> Pair<K, V> getOneEntry(Map<K, V> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        Map.Entry<K, V> entry = map.entrySet().iterator().next();
        return Pair.of(entry.getKey(), entry.getValue());
    }
}
