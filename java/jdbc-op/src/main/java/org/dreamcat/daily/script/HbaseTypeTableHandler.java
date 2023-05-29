package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserType;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@ArgParserType(firstChar = true, allProperties = true,
        command = "hbase-type-table")
public class HbaseTypeTableHandler implements ArgParserEntrypoint<HbaseTypeTableHandler> {

    @Override
    public void run(ArgParserContext<HbaseTypeTableHandler> context) {

    }
}
