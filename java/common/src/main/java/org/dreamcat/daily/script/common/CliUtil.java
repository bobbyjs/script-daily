package org.dreamcat.daily.script.common;

import java.util.List;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2023-03-24
 */
public class CliUtil {

    public static void checkParameter(Object value, String key, String names) {
        checkParameter(value, key, names, null);
    }

    public static void checkParameter(Object value, String key, String names, String env) {
        if (value instanceof List) {
            if (ObjectUtil.isNotEmpty((List<?>) value)) return;
        } else {
            if (ObjectUtil.isNotBlank((String) value)) return;
        }
        String envString = "";
        if (ObjectUtil.isNotBlank(env)) {
            envString = String.format(" or define the environment variable `%s`", env);
        }

        System.err.printf("required parameter `%s` is missing, "
                        + "pass it via %s%s",
                key, names, envString);
        System.exit(1);
    }
}
