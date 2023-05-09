package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParseException;
import org.dreamcat.common.argparse.ArgParserInject;
import org.dreamcat.common.argparse.ArgParserInject.InjectMethod;
import org.dreamcat.common.argparse.TypeArgParser;

/**
 * @author Jerry Will
 * @version 2023-05-03
 */
public interface Base {

    @ArgParserInject(method = InjectMethod.Exception)
    default void onException(ArgParseException e, TypeArgParser<?> argParser) {
        if (e.isMissKey()) {
            System.err.println("require arg: " + String.join(", ",
                    argParser.getArgument(e.getKey()).getHelpNames()));
        } else if (e.isUnknownKey()) {
            System.err.println("unknown arg: " + e.getName());
        } else {
            System.err.println("invalid arg: " + e);
        }
    }

    @ArgParserInject(method = InjectMethod.UnmatchedSubcommand)
    default void onUnmatchedSubcommand(String subcommand) {
        System.err.println("unknown subcommand: " + subcommand);
    }
}
