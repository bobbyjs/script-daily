package org.dreamcat.daily.script;

import org.junit.jupiter.api.Test;

/**
 * @author Jerry Will
 * @version 2023-08-19
 */
class MigrateHandlerTest {

    @Test
    void testHelp() {
        // Main.main(new String[]{"-h"});
        Main.main(new String[]{"migrate", "--help"});
    }
}
