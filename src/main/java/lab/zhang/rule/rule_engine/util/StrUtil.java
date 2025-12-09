package lab.zhang.rule.rule_engine.util;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collection;

public class StrUtil {
    /**
     * Join collection of strings with comma delimiter
     *
     * @param argColl collection of strings to join
     * @return joined string
     */
    public static String implode(@NotNull Collection<String> argColl) {
        return implode(argColl, ",");
    }

    /**
     * Join collection of strings with specified delimiter
     *
     * @param argColl   collection of strings to join
     * @param delimiter delimiter string
     * @return joined string
     */
    public static String implode(@NotNull Collection<String> argColl, @NotEmpty String delimiter) {
        return String.join(delimiter, argColl);
    }

    /**
     * Check if string is null
     *
     * @param str string to check
     * @return true if null, false otherwise
     */
    public static boolean isNull(String str) {
        return str == null;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
}
