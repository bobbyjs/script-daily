package org.dreamcat.daily.script;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.module.JdbcModule;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "export-csv")
public class ExportCsvHandler implements ArgParserEntrypoint {

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

    @ArgParserField(nested = true)
    JdbcModule jdbc;

    @SneakyThrows
    @Override
    public void run(ArgParserContext argParserContext) {
        if (ObjectUtil.isEmpty(databases) && !allDatabases && ObjectUtil.isBlank(databasePattern)) {
            System.out.println("require arg: --databases or --all-databases "
                    + "or --database-pattern");
            System.exit(1);
        }

        jdbc.run(this::handle);
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
                try (Statement statement = connection.createStatement()) {
                    handle(statement, database, table);
                }
            }
        }
    }

    private void handle(Statement statement, String database, String table) throws Exception {
        String sql = InterpolationUtil.format(selectSql,
                "database", database, "table", table);
        System.out.println("extract: " + sql);
        try (ResultSet rs = statement.executeQuery(sql)) {
            JdbcUtil.getRows(rs, batchSize, this::handleRows);
        }
    }

    private void handleRows(List<Map<String, Object>> rows) {
        System.out.println("handling " + rows.size() + " rows");

    }
}