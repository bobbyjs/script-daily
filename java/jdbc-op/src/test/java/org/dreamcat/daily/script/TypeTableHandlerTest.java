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
class TypeTableHandlerTest {

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
        args.addAll(Arrays.asList("type-table", "my_table", "--column-quota", "--debug",
                "--extra-column-sql", "id bigint(20) not null auto_increment primary key",
                "-c", "Column Type: $type", "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "mysql-types.txt").split("\n")));
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testHive() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table", "--column-quota",
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
                "--table-suffix-sql", "engine = MergeTree order by c_uuid",
                "--column-quota", "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "clickhouse-types.txt").split("\n")));

        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testPresto() throws Exception {
        List<String> args = Arrays.asList("type-table", "test.my_table", "--column-quota", "--debug",
                "--extra-column-sql", "id bigint(20) not null auto_increment primary key",
                "-c", "Column Type: $type",
                "--cn", "${type}_col_$index", "--pcn", "p_${type}_col_$index",
                "-F", ClassPathUtil.getResourceAsString("presto-mapping-types.txt"));
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testNullNeg() {
        new SubcommandArgParser(App.class).run("type-table", "my_table",
                "-t", "int", "string", "date",
                "--enable-neg", "--null-ratio", "0.25", "-b", "10", "-n", "100");
    }

    @Test
    void testSmartNull() {
        new SubcommandArgParser(App.class).run("type-table", "my_table",
                "-t", "int", "string", "date",
                "--enable-neg", "--row-null-ratio", "0.5,2", "-b", "10", "-n", "100");
    }
}
/*

set hive.exec.dynamic.partition=true
set hive.exec.dynamic.partition_mode=nonstrict

*/
