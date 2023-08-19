package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.SubcommandArgParser;
import org.dreamcat.common.util.ClassPathUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
class ImportCsvHandlerTest {

    @Test
    void test() throws Exception {
        new SubcommandArgParser(App.class).run(
                "import-csv", "t_table_test", "-b", "3",
                "-F",ClassPathUtil.getResourceAsString("test.csv"),
                "-T", ClassPathUtil.getResourceAsString("mysql-text-types.txt"));
    }
}
