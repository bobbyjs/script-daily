package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.util.CollectionUtil;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@ArgParserType(allProperties = true, command = "insert-random")
public class InsertRandomHandler extends BaseOutputHandler
        implements ArgParserEntrypoint<InsertRandomHandler> {

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
    public void run(ArgParserContext<InsertRandomHandler> context) {
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

    void afterPropertySet() throws Exception {
        this.typeTableHandler = (TypeTableHandler) new TypeTableHandler()
                .tableName(tableName)
                .columnName("$type")
                .partitionColumnName("$type")
                .columnQuota(columnQuota)
                .doubleQuota(doubleQuota)
                .dataSourceType(dataSourceType)
                .converterFile(converterFile)
                .converters(converters)
                .batchSize(batchSize)
                .rowNum(rowNum)
                .setEnumValues(setEnumValues)
                .compact(compact)
                .rollingFile(rollingFile)
                .rollingFileMaxSqlCount(rollingFileMaxSqlCount)
                .yes(yes)
                .debug(debug);
        typeTableHandler.afterPropertySet();
    }

    void handle(Connection connection) throws Exception {
        String s = null, t = tableName;
        if (tableName.contains(".")) {
            String[] ss = tableName.split("\\.", 2);
            s = ss[0];
            t = ss[1];
        }
        List<JdbcColumnDef> columns = JdbcUtil.getTableSchema(connection, s, t);
        columns = columns.stream().filter(column -> !ignoredColumns.contains(column.getName()))
                .collect(Collectors.toList());

        Pair<List<JdbcColumnDef>, List<JdbcColumnDef>> pair = CollectionUtil.partitioningBy(
                columns, column -> !partitionColumns.contains(column.getName()));

        List<String> types = CollectionUtil.mapToList(pair.first(), column -> {
            String columnName = column.getName();
            String type = column.getType();
            return type + ":" + columnName;
        });
        List<String> partitionTypes = CollectionUtil.mapToList(pair.second(), column -> {
            String columnName = column.getName();
            String type = column.getType();
            return type + ":" + columnName;
        });

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