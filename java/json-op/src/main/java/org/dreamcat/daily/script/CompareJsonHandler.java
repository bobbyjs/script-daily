package org.dreamcat.daily.script;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.x.jackson.JsonUtil;

/**
 * @author Jerry Will
 * @version 2021-06-15
 */
public class CompareJsonHandler {

    private CompareJsonHandler() {
    }

    public static void print(String[] args) {
        if (args.length < 3) {
            System.err.printf("parameters: %s <leftFile> <rightFile>%n", args[0]);
            return;
        }
        File leftFile = new File(args[1]), rightFile = new File(args[2]);
        Map<String, Object> left = JsonUtil.fromJsonObject(leftFile);
        Map<String, Object> right = JsonUtil.fromJsonObject(rightFile);

        Map<String, Object> leftDiff = new LinkedHashMap<>();
        Map<String, Object> rightDiff = new LinkedHashMap<>();
        MapUtil.compare(left, right, leftDiff, rightDiff);

        String leftJson = JsonUtil.toJson(MapUtil.sort(leftDiff));
        String rightJson = JsonUtil.toJson(MapUtil.sort(rightDiff));
        System.out.printf("%s:\t%s%n", leftFile.getName(), leftJson);
        System.out.printf("%s:\t%s%n", rightFile.getName(), rightJson);
    }

    public static void preJsonOneLine(String[] args) throws IOException {
        if (args.length < 4) {
            System.err.printf("parameters: %s <inputFile> <outputLeftFile> <outputRightFile>%n", args[0]);
            return;
        }
        File inputFile = new File(args[1]), leftFile = new File(args[2]), rightFile = new File(args[3]);
        List<String> jsons = FileUtil.readAsList(inputFile);
        int size = jsons.size();
        if (size < 1) return;

        Map<String, Object> prev = JsonUtil.fromJsonObject(jsons.get(0)), next;
        for (int i = 1; i < size; i++) {
            next = JsonUtil.fromJsonObject(jsons.get(i));
            Map<String, Object> leftDiff = new LinkedHashMap<>();
            Map<String, Object> rightDiff = new LinkedHashMap<>();
            MapUtil.compare(prev, next, leftDiff, rightDiff);
            FileUtil.writeFrom(leftFile, JsonUtil.toJson(leftDiff) + '\n', true);
            FileUtil.writeFrom(rightFile, JsonUtil.toJson(rightDiff) + '\n', true);
            prev = next;
        }
    }
}
