package org.dreamcat.daily.script;

import static org.dreamcat.common.util.StringUtil.isNotEmpty;

import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.DriverUtil;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.CliUtil;
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
    boolean verbose;
    boolean yes;

    @ArgParserField(value = {"j1"})
    String jdbcUrl1;
    @ArgParserField(value = {"u1"})
    String user1;
    @ArgParserField(value = {"p1"})
    String password1;
    @ArgParserField(value = {"dc1"})
    String driverClass1;
    @ArgParserField(value = {"dp1"})
    List<String> driverPaths1; // driver directory

    @ArgParserField(value = {"j2"})
    String jdbcUrl2;
    @ArgParserField(value = {"u2"})
    String user2;
    @ArgParserField(value = {"p2"})
    String password2;
    @ArgParserField(value = {"dc2"})
    String driverClass2;
    @ArgParserField(value = {"dp2"})
    List<String> driverPaths2; // driver directory


    @ArgParserField(nested = true)
    RandomGenModule randomGen;

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        randomGen.afterPropertySet();
        CliUtil.checkParameter(jdbcUrl1, "-j1|--jdbc-url1");
        CliUtil.checkParameter(driverPaths1, "--dp1|--driver-paths1");
        CliUtil.checkParameter(driverClass1, "--dc1|--driver-class1");
        if (yes) {
            CliUtil.checkParameter(jdbcUrl2, "-j2|--jdbc-url2");
            CliUtil.checkParameter(driverPaths2, "--dp2|--driver-paths2");
            CliUtil.checkParameter(driverClass2, "--dc2|--driver-class2");
        }
    }

    @Override
    protected void fetchSource(IConsumer<Connection, ?> f) throws Exception {
        List<URL> urls = DriverUtil.parseJarPaths(driverPaths1);
        for (URL url : urls) {
            if (verbose) System.out.println("add url to classloader: " + url);
        }
        Properties props = new Properties();
        if (isNotEmpty(user1)) props.put("user", user1);
        if (isNotEmpty(password1)) props.put("password", password1);
        DriverUtil.runIsolated(jdbcUrl1, props, urls, driverClass1, f);
    }

    protected void writeTarget(IConsumer<Connection, ?> f) throws Exception {
        List<URL> urls = DriverUtil.parseJarPaths(driverPaths2);
        for (URL url : urls) {
            if (verbose) System.out.println("add url to classloader: " + url);
        }
        Properties props = new Properties();
        if (isNotEmpty(user2)) props.put("user", user2);
        if (isNotEmpty(password2)) props.put("password", password2);
        DriverUtil.runIsolated(jdbcUrl2, props, urls, driverClass2, f);
    }

    @SneakyThrows
    @Override
    protected void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap,
            Connection targetConnection) {
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

        try (Statement statement = targetConnection.createStatement()) {
            long t = System.currentTimeMillis();
            int rowEffect = statement.executeUpdate(sql);
            System.out.printf("%d row effect, cost %.2fs%n",
                    rowEffect, (System.currentTimeMillis() - t) / 1000.);
        }
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