package org.dreamcat.daily.script;

import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.daily.script.common.SchemaHandler;

/**
 * Create by tuke on 2021/4/5
 */
@Slf4j
public class App {

    static final String USAGE;

    public static void main(String[] args) {
        int length = args.length;
        if (length < 2) {
            if (length == 1 && (args[0].equals("-h") || args[0].equals("--help"))) {
                System.out.println(USAGE);
                System.exit(0);
            }
            System.err.println("fs-migrate: try 'fs-migrate --help' for more information");
            System.exit(1);
        }

        String command = args[0];
        // remaining args
        args = Arrays.copyOfRange(args, 1, length);

        SchemaHandler<?> schemaHandler;
        switch (command) {
            case "rename":
                schemaHandler = new RenameOp();
                break;
            default:
                System.err.printf("unsupported command: %s%n", command);
                System.exit(1);
                return;
        }
        schemaHandler.run(args);
    }

    static {
        try {
            USAGE = ClassPathUtil.getResourceAsString("usage.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
