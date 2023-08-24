package org.dreamcat.daily.script;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.text.InterpolationUtil;
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

    private boolean useMetadata;
    private String showDatabases = "show databases";
    private String showTables = "show tables from $database";

    private String catalog;
    private String databasePattern;
    private List<String> databases;
    private boolean allDatabases;
    private String tablePattern; // .* for sql, or % for metadata
    private List<String> tableNames;
    private String selectSql = "select * from $database.$table";
    @ArgParserField({"b"})
    private int batchSize = 1000;
    boolean verbose;
    private boolean abort;

    protected abstract void readSource(IConsumer<Connection, ?> f) throws Exception;

    protected void writeTarget(IConsumer<Connection, ?> f) throws Exception {
        f.accept(null);
    }

    protected abstract void handleRows(String database, String table,
            List<Map<String, Object>> rows, Map<String, JdbcColumnDef> columnMap,
            Connection targetConnection);

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
        readSource(this::handle);
    }

    private void handle(Connection connection) throws Exception {
        List<String> matchedDatabases = new ArrayList<>();
        if (ObjectUtil.isEmpty(databases)) {
            List<String> allDatabases = getDatabases(connection);
            if (ObjectUtil.isEmpty(allDatabases)) {
                System.out.println("no databases found in catalog: " + catalog);
                System.exit(0);
            } else if (ObjectUtil.isNotBlank(databasePattern)) {
                for (String database : allDatabases) {
                    if (!database.matches(databasePattern)) {
                        System.out.println(database + " is unmatched by " + databasePattern);
                        continue;
                    }
                    matchedDatabases.add(database);
                }
            } else {
                matchedDatabases = allDatabases;
            }
        } else {
            matchedDatabases = databases;
            if (ObjectUtil.isNotBlank(databasePattern)) {
                System.out.println("already pass --databases so --database-pattern is ignored");
            }
            if (ObjectUtil.isNotBlank(databasePattern)) {
                System.out.println("already pass --databases so --all-database is ignored");
            }
        }

        List<String> dbs = matchedDatabases;
        writeTarget(targetConnection -> {
            handle(connection, dbs, targetConnection);
        });
    }

    private void handle(Connection connection, List<String> matchedDatabases,
            Connection targetConnection) throws SQLException {
        outer:
        for (String database : matchedDatabases) {
            List<String> tables = getTables(connection, database);
            if (ObjectUtil.isEmpty(tables)) {
                System.out.println("no tables found in database " + database);
                continue;
            }
            for (String table : tables) {
                if (ObjectUtil.isNotEmpty(tableNames) && !tableNames.contains(table)) {
                    System.out.println(table + " is not in " + tableNames);
                    continue;
                }

                try {
                    handle(connection, database, table, targetConnection);
                } catch (Exception e) {
                    System.err.printf("failed to migrate %s.%s: %s%n",
                            database, table, e.getMessage());
                    if (verbose) e.printStackTrace();
                    if (abort) break outer;
                }
            }
        }
    }

    private void handle(Connection connection, String database, String table,
            Connection targetConnection) throws Exception {
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
                    handleRows(database, table, rows, columnMap, targetConnection);
                });
            }
        }
    }

    private List<String> getDatabases(Connection connection) throws SQLException {
        if (useMetadata) {
            return JdbcUtil.getDatabases(connection, catalog);
        }
        return JdbcUtil.getRows(connection, showDatabases).stream()
                .map(map -> new ArrayList<>(map.values()).get(0).toString())
                .collect(Collectors.toList());
    }

    private List<String> getTables(Connection connection, String database)
            throws SQLException {
        if (useMetadata) {
            if (ObjectUtil.isNotBlank(tablePattern)) {
                return JdbcUtil.getTableLike(connection, catalog, database, tablePattern);
            } else {
                return JdbcUtil.getTables(connection, catalog, database);
            }
        }
        String sql =  InterpolationUtil.format(showTables,
                "database", database, "db", database);
        return JdbcUtil.getRows(connection, sql).stream()
                .map(map -> new ArrayList<>(map.values()).get(0).toString())
                .filter(name -> ObjectUtil.isBlank(tablePattern) || name.matches(tablePattern))
                .collect(Collectors.toList());
    }
}