package org.dreamcat.daily.script;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;

/**
 * @author Jerry Will
 * @version 2023-06-07
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-table")
public class ImportTypeHandler extends BaseHandler implements ArgParserEntrypoint {

    @ArgParserField("f")
    private String file; // supported subfix: csv, xls, xlsx
    @ArgParserField("T")
    private String tableName; // only for csv

    @Override
    public void run(ArgParserContext argParserContext) {

    }

    private void excel() throws Exception {
        Map<String, List<List<Object>>> sheets = parse(new File(file));

    }

    private void handleOne(List<List<Object>> rows) {

    }

    public static Map<String, List<List<Object>>> parse(File file) {
        return Collections.emptyMap();
    }
}