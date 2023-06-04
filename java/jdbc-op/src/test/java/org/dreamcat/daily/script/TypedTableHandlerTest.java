package org.dreamcat.daily.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.dreamcat.common.argparse.SubcommandArgParser;
import org.dreamcat.common.util.ClassPathUtil;
import org.junit.jupiter.api.Test;

/**
 * <pre><code>
 *     typed-table my_table -t int string char(%d)
 * </code></pre>
 *
 * @author Jerry Will
 * @version 2023-04-01
 */
class TypedTableHandlerTest {

    @Test
    void testPostgres() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table", "-S", "postgres",
                "-c", "Column Type: $type", "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "postgresql-types.txt").split("\n")));
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testMysql() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table", "--column-quotation", "--debug",
                "--extra-column-ddl", "id bigint(20) not null auto_increment primary key",
                "-c", "Column Type: $type", "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "mysql-types.txt").split("\n")));
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testHive() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table", "--column-quotation",
                "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "hive-types.txt").split("\n")));
        args.addAll(Arrays.asList("-p", "date", "string"));

        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testClickhouse() throws Exception {

        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table",
                "--table-ddl-suffix", "engine = MergeTree order by c_uuid",
                "--column-quotation",
                "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "clickhouse-types.txt").split("\n")));

        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testPresto() throws Exception {
        List<String> args = Arrays.asList("type-table", "test.my_table", "--quota", "--debug",
                "--extra-column-sql", "id bigint(20) not null auto_increment primary key",
                "-c", "Column Type: $type",
                "-C", "${type}_col_$index", "-P", "p_${type}_col_$index",
                "-F", ClassPathUtil.getResourceAsString("presto-mapping-types.txt"));
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }
}
/*

set hive.exec.dynamic.partition=true
set hive.exec.dynamic.partition_mode=nonstrict

*/
