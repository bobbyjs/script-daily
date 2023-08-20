package org.dreamcat.daily.script;

import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-08-21
 */
class ExportJdbcHandlerTest {

    @Test
    void testMysql() throws Exception {
        Main.main(new String[]{
                "export-jdbc", "-h"
        });
    }
}
