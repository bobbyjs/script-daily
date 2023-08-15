package org.dreamcat.daily.script;

import java.io.IOException;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.argparse.SubcommandArgParser;
import org.dreamcat.common.argparse.SubcommandHelpInfo;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.SystemUtil;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@ArgParserType(
        allProperties = true,
        firstChar = true,
        subcommands = {
                // insert
                InsertRandomHandler.class,
                // type
                TypeTableHandler.class,
                BatchTypeTableHandler.class,
                HbaseTypeTableHandler.class,
                // import
                ImportCsvHandler.class,
                ImportExcelHandler.class
        }
)
public class App implements ArgParserEntrypoint {

    boolean help;

    public static void main(String[] args) throws Exception {
        SubcommandArgParser argParser = new SubcommandArgParser(App.class);
        SubcommandHelpInfo helpInfo = getI18nHelpInfo();
        if (helpInfo != null) {
            argParser.setSubcommandHelpInfo(helpInfo);
        }
        argParser.run(args);
    }

    private static SubcommandHelpInfo getI18nHelpInfo() throws IOException  {
        String name = "usage.yaml";
        return YamlUtil.fromJson(ClassPathUtil.getResourceAsString(name),
                SubcommandHelpInfo.class);
    }

    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }
        System.err.println("require a subcommand");
        System.err.println("jdbc-op: try 'jdbc-op --help' for more information");
        System.exit(1);
    }
}
