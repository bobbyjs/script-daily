package org.dreamcat.daily.script;

import java.io.File;
import java.util.Map;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.json.JsonUtil;

/**
 * @author Jerry Will
 * @version 2021-09-10
 */
public class SortJsonFieldHandler {

    private SortJsonFieldHandler() {
    }

    public static void print(String[] args) {
        if (args.length < 2) {
            System.err.printf("parameters: %s <file>%n", args[0]);
            return;
        }
        String file = args[1];
        Map<String, Object> map = JsonUtil.fromJsonObject(new File(file));
        Map<String, Object> newMap = MapUtil.sort(map);
        String json = JsonUtil.toJson(newMap);
        System.out.println(json);
    }
}
