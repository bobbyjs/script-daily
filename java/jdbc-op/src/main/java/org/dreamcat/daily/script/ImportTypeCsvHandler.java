package org.dreamcat.daily.script;

import java.io.File;
import java.io.StringReader;
import java.sql.Connection;
import java.util.List;
import java.util.Set;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-06-17
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-table-csv")
public class ImportTypeCsvHandler extends BaseHandler implements ArgParserEntrypoint {

    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    private String tableName;
    @ArgParserField("i")
    private Set<String> ignoredColumns;
    @ArgParserField("P")
    private Set<String> partitionColumns;

    private boolean noHeader;

    @SneakyThrows
    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }

        if (ObjectUtil.isEmpty(file) && ObjectUtil.isBlank(fileContent)) {
            System.err.println(
                    "required arg: -f|--file <file> or -F|--file-content <content>");
            System.exit(1);
        }

        List<List<String>> rows;
        if (ObjectUtil.isNotEmpty(file)) {
            rows = CsvUtil.parse(new File(file));
        } else {
            rows = CsvUtil.parse(new StringReader(fileContent));
        }

        run(connection -> this.handle(connection, rows));
    }

    private void handle(Connection connection, List<List<String>> rows) throws Exception {
        if (rows.isEmpty()) return;

        Pair<List<String>, List<String>> pair = TypeInfo.getTypes(connection, tableName, ignoredColumns, partitionColumns);
        List<String> types = pair.first(), partitionTypes = pair.second();

        if (noHeader) {

        }
    }
}
