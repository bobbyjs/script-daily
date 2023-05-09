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
    void testPostgresStyle() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table", "--postgres-style",
                "-c", "Column Type: $type", "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "postgresql-types.txt").split("\n")));
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }

    @Test
    void testMysqlStyle() throws Exception {
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
    void testHiveStyle() throws Exception {
        List<String> args = new ArrayList<>();
        args.addAll(Arrays.asList("type-table", "my_table", "--column-quotation",
                "-t"));
        args.addAll(Arrays.asList(ClassPathUtil.getResourceAsString(
                "hive-types.txt").split("\n")));
        args.addAll(Arrays.asList("-p", "date", "string"));

        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        argParser.run(args);
    }
}
/*

set hive.exec.dynamic.partition=true
set hive.exec.dynamic.partition_mode=nonstrict

*/
