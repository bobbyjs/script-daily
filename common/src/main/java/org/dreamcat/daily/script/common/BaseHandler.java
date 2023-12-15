package org.dreamcat.daily.script.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.SubcommandArgParser;
import org.dreamcat.common.argparse.SubcommandHelpInfo;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
public abstract class BaseHandler implements ArgParserEntrypoint {

    @ArgParserField(firstChar = true)
    private boolean help;

    public static void run(Class<? extends BaseHandler> clazz, String[] args) {
        if (ObjectUtil.isNotBlank(System.getenv("DEBUG")) && !"0".equals(System.getenv("DEBUG"))) {
            System.out.println(Arrays.toString(args));
        }
        SubcommandArgParser argParser = new SubcommandArgParser(clazz);
        // help info
        try (InputStream file = clazz.getClassLoader().getResourceAsStream("usage.yaml")) {
            SubcommandHelpInfo helpInfo = YamlUtil.fromJson(file, SubcommandHelpInfo.class);
            argParser.setSubcommandHelpInfo(helpInfo);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        argParser.run(args);
    }

    protected void afterPropertySet() throws Exception {
        // nop
    }

    public abstract void run() throws Exception;

    @SneakyThrows
    @Override
    public final void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }

        this.afterPropertySet();
        this.run();
    }
}
