package org.dreamcat.daily.script;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.module.JdbcModule;
import org.dreamcat.daily.script.module.RandomGenModule;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "export-jdbc")
public class ExportJdbcHandler extends BaseExportHandler {

    boolean columnQuota;
    boolean doubleQuota; // "c1" or `c2`
    boolean yes;

    @ArgParserField(nested = true, nestedPrefix = "source.")
    JdbcModule source;
    @ArgParserField(nested = true, nestedPrefix = "target.")
    JdbcModule target;

    @ArgParserField(nested = true)
    RandomGenModule randomGen;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        randomGen.afterPropertySet();
        source.validateJdbc();

    }

    @Override
    protected void fetchSource(IConsumer<Connection, ?> f) throws Exception {
        source.run(f);
    }

    @SneakyThrows
    @Override
    protected void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap) {
        List<List<Object>> list = rows.stream().map(this::mapToRow)
                .collect(Collectors.toList());

        List<String> columnNames = new ArrayList<>(columnMap.keySet());
        List<String> typeNames = columnMap.values().stream().map(JdbcColumnDef::getType)
                .collect(Collectors.toList());

        String columnNameSql = StringUtil.join(",", columnNames, this::formatColumnName);
        String insertIntoSql = String.format(
                "insert into %s.%s(%s) values ", database, table, columnNameSql);

        String sql = insertIntoSql + randomGen.generateValues(list, typeNames);

        System.out.println(sql);
        if (!yes) return;

        target.run(connection -> {
            try (Statement statement = connection.createStatement()) {
                long t = System.currentTimeMillis();
                int rowEffect = statement.executeUpdate(sql);
                System.out.printf("%d row effect, cost %.2fs%n",
                        rowEffect, (System.currentTimeMillis() - t) / 1000.);
            }
        });
    }

    private List<Object> mapToRow(Map<String, Object> map) {
        List<Object> row = new ArrayList<>(map.size());
        row.addAll(map.values());
        return row;
    }

    public String formatColumnName(String columnName) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }
}