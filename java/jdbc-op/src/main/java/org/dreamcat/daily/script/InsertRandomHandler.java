package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@ArgParserType(allProperties = true, command = "insert-random")
public class InsertRandomHandler extends BaseOutputHandler implements ArgParserEntrypoint {

    @ArgParserField(required = true, position = 0)
    private String tableName;
    @ArgParserField("i")
    private Set<String> ignoredColumns;
    @ArgParserField("P")
    private Set<String> partitionColumns;

    boolean columnQuota;
    boolean doubleQuota; // "c1" or `c2`
    @ArgParserField("S")
    String dataSourceType;
    String converterFile;
    Set<String> converters; // binary:cast($value as $type)
    String setEnumValues = "a,b,c,d";

    @ArgParserField({"b"})
    int batchSize = 1;
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);

    transient TypeTableHandler typeTableHandler;

    @SneakyThrows
    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }
        if (tableName == null) {
            System.err.println("required arg <tableName> is absent");
            System.exit(1);
        }

        this.afterPropertySet();
        run(this::handle);
    }

    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();

        this.typeTableHandler = (TypeTableHandler) new TypeTableHandler()
                .batchSize(batchSize)
                .rowNum(rowNum)
                .tableName(tableName)
                .columnNameTemplate("$type")
                .partitionColumnNameTemplate("$type")
                .columnQuota(columnQuota)
                .doubleQuota(doubleQuota)
                .setEnumValues(setEnumValues)
                .compact(compact)
                .rollingFile(rollingFile)
                .rollingFileMaxSqlCount(rollingFileMaxSqlCount)
                .dataSourceType(dataSourceType)
                .converterFile(converterFile)
                .converters(converters)
                .yes(yes)
                .debug(debug);
        typeTableHandler.afterPropertySet();
    }

    void handle(Connection connection) throws Exception {
        Pair<List<String>, List<String>> pair = TypeInfo.getTypes(connection, tableName, ignoredColumns, partitionColumns);
        List<String> types = pair.first(), partitionTypes = pair.second();

        typeTableHandler
                .types(types)
                .partitionTypes(partitionTypes);

        List<String> sqlList = typeTableHandler.genSql().second();
        if (!yes) {
            output(sqlList);
            return;
        }
        try (Statement statement = connection.createStatement()) {
            output(sqlList);
            for (String sql : sqlList) {
                statement.execute(sql);
            }
        }
    }
}