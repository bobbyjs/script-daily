package org.dreamcat.daily.script;

import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2023-12-14
 */
@ArgParserType(allProperties = true, command = "rename-by")
public class RenameByOp extends BaseHandler {

    @ArgParserField(position = 1)
    private SubOp subOp;
    @ArgParserField(position = 2)
    private String sourcePath;

    @Override
    protected void afterPropertySet() throws Exception {
        CliUtil.checkParameter(sourcePath, "<sourcePath>");
    }

    @Override
    public void run() throws Exception {

    }

    private enum SubOp {
        trim_blank,
        ;

    }
}
