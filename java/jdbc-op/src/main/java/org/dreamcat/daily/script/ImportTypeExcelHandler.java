package org.dreamcat.daily.script;

import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.excel.ExcelUtil;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2023-06-28
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-table-excel")
public class ImportTypeExcelHandler extends BaseDdlOutputHandler implements ArgParserEntrypoint {

    @ArgParserField("f")
    private String file;
    private boolean createTable;
    @ArgParserField({"b"})
    int batchSize = 1;

    private boolean emptyStringAsNull;
    @ArgParserField("t")
    private String textTypeFile;
    @ArgParserField("T")
    private String textTypeFileContent;

    @SneakyThrows
    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }
        run();
    }

    public void run() throws Exception {
        if (ObjectUtil.isEmpty(file)) {
            System.err.println(
                    "required arg: -f|--file <file>");
            System.exit(1);
        }

        Map<String, List<List<Object>>> sheets = ExcelUtil.parseAsMap(new File(file));

    }
}
