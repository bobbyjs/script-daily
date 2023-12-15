package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.common.BaseHandler;

/**
 * Create by tuke on 2021/4/5
 */
@ArgParserType(allProperties = true,
        firstChar = true,
        subcommands = {
                RenameOp.class,
        })
public class Main extends BaseHandler {

    public static void main(String[] args) {
        run(Main.class, args);
    }

    @Override
    public void run() throws Exception {
        System.err.println("require a subcommand");
        System.err.println("fs-op: try 'fs-op --help' for more information");
        System.exit(1);
    }
}
