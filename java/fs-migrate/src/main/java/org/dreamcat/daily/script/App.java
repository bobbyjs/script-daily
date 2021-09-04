package org.dreamcat.daily.script;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.daily.script.common.SchemaHandler;

/**
 * Create by tuke on 2021/4/5
 */
@Slf4j
public class App {

    public static void main(String[] args) {
        int length = args.length;
        if (length < 2) {
            log.error("fs-migrate: try 'fs-migrate --help' for more information");
            System.exit(1);
        }

        String command = args[0];
        // remaining args
        args = Arrays.copyOfRange(args, 1, length);

        SchemaHandler<?> schemaHandler;
        switch (command) {
            case "move":
            case "rename":
                schemaHandler = new RenameOp();
                break;
            default:
                log.error("unsupported command: {}", command);
                System.exit(1);
                return;
        }
        schemaHandler.run(args);
    }
}
