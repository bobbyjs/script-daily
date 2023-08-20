package org.dreamcat.daily.script;

import java.io.File;
import java.util.Map;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.json.JsonUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2021-09-10
 */
@ArgParserType(command = "sort")
public class SortOp extends BaseHandler {

    @ArgParserField(position = 1)
    private File file;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        CliUtil.checkParameter(file, "<file>");
    }

    @Override
    public void run() throws Exception {
        Map<String, Object> map = JsonUtil.fromJsonObject(file);
        Map<String, Object> newMap = MapUtil.sort(map);
        String json = JsonUtil.toJson(newMap);
        System.out.println(json);
    }
}
