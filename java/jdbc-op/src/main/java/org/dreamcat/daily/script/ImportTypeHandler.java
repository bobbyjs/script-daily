package org.dreamcat.daily.script;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;

/**
 * @author Jerry Will
 * @version 2023-06-07
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-table")
public class ImportTypeHandler extends BaseHandler implements ArgParserEntrypoint<ImportTypeHandler> {

    @ArgParserField("f")
    private String file; // supported subfix: csv, xls, xlsx
    @ArgParserField("T")
    private String tableName; // only for csv

    @Override
    public void run(ArgParserContext<ImportTypeHandler> argParserContext) {

    }


}