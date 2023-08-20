package org.dreamcat.daily.script;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2021-06-15
 */
@ArgParserType(command = "compare")
public class CompareOp extends BaseHandler {

    @ArgParserField(position = 1)
    private File leftFile;
    @ArgParserField(position = 2)
    private File rightFile;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        CliUtil.checkParameter(leftFile, "<leftFile>");
        CliUtil.checkParameter(leftFile, "<leftFile>");
    }

    @Override
    public void run() throws Exception {
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
}
