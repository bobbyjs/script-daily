package org.dreamcat.daily.script;

import java.sql.Connection;
import java.util.List;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.sql.JdbcUtil;
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

    private List<String> databaseNames;
    private boolean allDatabases;
    private String tablePattern;
    private List<String> tableNames;

    @ArgParserField(nested = true)
    JdbcModule jdbc;

    @SneakyThrows
    @Override
    public void run(ArgParserContext argParserContext) {
        if (ObjectUtil.isEmpty(databaseNames) && !allDatabases) {
            System.out.println("no databases");
            System.exit(0);
        }

        jdbc.run(this::handle);
    }

    protected void handle(Connection connection) throws Exception {
        if (ObjectUtil.isEmpty(databaseNames)) {
            databaseNames = JdbcUtil.getDatabases(connection);
            if (ObjectUtil.isEmpty(databaseNames)) {
                System.out.println("no databases");
                System.exit(0);
            }
        }

        for (String database : databaseNames) {
            List<String> tables = JdbcUtil.getTables(connection, database);
            for (String table : tables) {
                if (tablePattern != null && !table.matches(tablePattern)) {
                    System.out.println(table + " is unmatched by " + tablePattern);
                    continue;
                }
                if (ObjectUtil.isNotEmpty(tableNames) && !tableNames.contains(table)) {
                    System.out.println(table + " is not in " + tableNames);
                    continue;
                }
                handle(connection, database, table);
            }
        }
    }

    private void handle(Connection connection, String database, String table) throws Exception {

    }
}