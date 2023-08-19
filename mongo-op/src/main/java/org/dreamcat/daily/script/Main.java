package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.common.BaseHandler;

/**
 * Create by tuke on 2021/3/22
 */
@ArgParserType(allProperties = true,
        firstChar = true,
        subcommands = {
                MigrateOp.class,
        })
public class Main extends BaseHandler {

    public static void main(String[] args) {
        run(Main.class, args);
    }

    @Override
    public void run() {
        System.err.println("require a subcommand");
        System.err.println("mongo-op: try 'mongo-op --help' for more information");
        System.exit(1);
    }
}
