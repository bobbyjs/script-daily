package org.dreamcat.daily.script;

import java.io.IOException;
import org.dreamcat.common.argparse.ArgParserInject;
import org.dreamcat.common.argparse.ArgParserInject.InjectMethod;
import org.dreamcat.common.argparse.ArgParserInject.InjectParam;
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
                InsertRandomHandler.class,
                TypeTableHandler.class,
                BatchTypeTableHandler.class,
        }
)
public class App implements Base {

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
        // only support: en, zh
        String lang = SystemUtil.getEnv("LANG", "en_US.UTF-8");
        String name = "usage.yaml";
        if (lang.startsWith("zh")) {
            name = "usage_zh.yaml";
        }
        return YamlUtil.fromJson(ClassPathUtil.getResourceAsString(name), SubcommandHelpInfo.class);
    }

    @ArgParserInject(method = InjectMethod.Action)
    public void run(@ArgParserInject(param = InjectParam.Help) String helpInfo) {
        if (help) {
            System.out.println(helpInfo);
            return;
        }
        System.err.println("require a subcommand");
        System.err.println("rustup: try 'jdbc-op --help' for more information");
        System.exit(1);
    }
}
