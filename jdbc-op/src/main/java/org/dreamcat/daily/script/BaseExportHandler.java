package org.dreamcat.daily.script;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ExceptionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.BaseHandler;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Setter
@Accessors(fluent = true)
public abstract class BaseExportHandler extends BaseHandler {

    private String catalog;
    private String databasePattern;
    private List<String> databases;
    private boolean allDatabases;
    private String tableLike;
    private String tablePattern;
    private List<String> tableNames;
    private String selectSql = "select * from $database.$table";
    @ArgParserField({"b"})
    private int batchSize = 1000;
    private boolean abort;

    protected abstract void fetchSource(IConsumer<Connection, ?> f) throws Exception;

    protected abstract void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap);

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        if (ObjectUtil.isEmpty(databases) && !allDatabases && ObjectUtil.isBlank(databasePattern)) {
            System.out.println("require arg: --databases or --all-databases "
                    + "or --database-pattern");
            System.exit(1);
        }
    }

    @Override
    public void run() throws Exception {
        fetchSource(this::handle);
    }

    protected void handle(Connection connection) throws Exception {
        if (ObjectUtil.isEmpty(catalog)) {
            catalog = connection.getCatalog();
        }

        List<String> matchedDatabases = new ArrayList<>();
        if (ObjectUtil.isEmpty(databases)) {
            databases = JdbcUtil.getDatabases(connection, catalog);
            if (ObjectUtil.isEmpty(databases)) {
                System.out.println("no databases found in catalog: " + catalog);
                System.exit(0);
            } else if (ObjectUtil.isNotBlank(databasePattern)) {
                for (String database : databases) {
                    if (!database.matches(databasePattern)) {
                        System.out.println(database + " is unmatched by " + databasePattern);
                        continue;
                    }
                    matchedDatabases.add(database);
                }
            } else {
                matchedDatabases = databases;
            }
        } else {
            if (ObjectUtil.isNotBlank(databasePattern)) {
                System.out.println("already pass --databases so --database-pattern is ignored");
            }
            if (ObjectUtil.isNotBlank(databasePattern)) {
                System.out.println("already pass --databases so --all-database is ignored");
            }
        }

        outer:
        for (String database : matchedDatabases) {
            List<String> tables;
            if (ObjectUtil.isNotBlank(tableLike)) {
                tables = JdbcUtil.getTableLike(connection, catalog, database, tableLike);
            } else {
                tables = JdbcUtil.getTables(connection, catalog, database);
            }
            for (String table : tables) {
                if (tablePattern != null && !table.matches(tablePattern)) {
                    System.out.println(table + " is unmatched by " + tablePattern);
                    continue;
                }
                if (ObjectUtil.isNotEmpty(tableNames) && !tableNames.contains(table)) {
                    System.out.println(table + " is not in " + tableNames);
                    continue;
                }

                try {
                    handle(connection, database, table);
                } catch (Exception e) {
                    if (abort) break outer;
                }
            }
        }
    }

    private void handle(Connection connection, String database, String table) throws Exception {
        // schema
        List<JdbcColumnDef> columns = JdbcUtil.getColumns(
                connection, catalog, database, table);
        Map<String, JdbcColumnDef> columnMap = MapUtil.toMap(columns, JdbcColumnDef::getName);

        // query
        String sql = InterpolationUtil.format(selectSql,
                "database", database, "table", table);
        System.out.println("extract: " + sql);
        try (Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery(sql)) {
                JdbcUtil.getRows(rs, batchSize, rows -> {
                    System.out.printf("handling %d rows on %s.%s%n",
                            rows.size(), database, table);
                    handleRows(database, table, rows, columnMap);
                });
            }
        }

    }
}