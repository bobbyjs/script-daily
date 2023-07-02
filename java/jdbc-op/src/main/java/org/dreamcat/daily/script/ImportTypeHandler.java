package org.dreamcat.daily.script;

import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.util.ObjectUtil;

/**
 * @author Jerry Will
 * @version 2023-06-07
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-table")
public class ImportTypeHandler extends BaseDdlOutputHandler implements ArgParserEntrypoint {

    @ArgParserField("f")
    private String file; // supported subfix: csv, tsv, xls, xlsx
    @ArgParserField("T")
    private String tableName; // only for csv
    @ArgParserField("C")
    private boolean createTable;
    @ArgParserField({"b"})
    int batchSize = 1;

    private boolean emptyStringAsNull;
    @ArgParserField("t")
    private String textTypeFile;
    @ArgParserField("T")
    private String textTypeFileContent;

    @SneakyThrows
    @Override
    public void run(ArgParserContext argParserContext) {
        if (ObjectUtil.isEmpty(file)) {
            System.err.println("required arg: -f|--file <file>");
            System.exit(1);
        }

        String suffix = FileUtil.suffix(file);
        if ("csv".equalsIgnoreCase(suffix)) {
            runForCsv(false);
        } else if ("tsv".equalsIgnoreCase(suffix)) {
            runForCsv(true);
        } else if ("xlsx".equalsIgnoreCase(suffix) || "xls".equalsIgnoreCase(suffix)) {
            runForExcel();
        } else {
            System.err.println("unsupported file type: " + file);
            System.exit(1);
        }
    }

    private void runForCsv(boolean tsv) throws Exception {
        if (ObjectUtil.isEmpty(tableName)) {
            tableName = FileUtil.basename(file);
        }
        ImportTypeCsvHandler handler = (ImportTypeCsvHandler) new ImportTypeCsvHandler()
                .file(file)
                .tsv(tsv)
                .tableName(tableName)
                .createTable(createTable)
                .batchSize(batchSize)
                .emptyStringAsNull(emptyStringAsNull)
                .textTypeFile(textTypeFile)
                .textTypeFileContent(textTypeFileContent)
                .columnName(columnName)
                .columnQuota(columnQuota)
                .doubleQuota(doubleQuota)
                .commentAlone(commentAlone)
                .columnCommentSql(columnCommentSql)
                .tableSuffixSql(tableSuffixSql)
                .extraColumnSql(extraColumnSql)
                .setEnumValues(setEnumValues)
                .compact(compact)
                .rollingFile(rollingFile)
                .rollingFileMaxSqlCount(rollingFileMaxSqlCount)
                .dataSourceType(dataSourceType)
                .converterFile(converterFile)
                .converters(converters)
                .nullRatio(nullRatio)
                .enableNeg(enableNeg)
                .yes(yes)
                .debug(debug);
        handler.run();
    }

    private void runForExcel() throws Exception {
        // ImportTypeExcelHandler handler = (ImportTypeExcelHandler) new ImportTypeExcelHandler()
        //         .createTable(createTable)
        //         .batchSize(batchSize)
        //         .emptyStringAsNull(emptyStringAsNull)
        //         .textTypeFile(textTypeFile)
        //         .textTypeFileContent(textTypeFileContent)
        //         .columnName(columnName)
        //         .columnQuota(columnQuota)
        //         .doubleQuota(doubleQuota)
        //         .commentAlone(commentAlone)
        //         .columnCommentSql(columnCommentSql)
        //         .tableSuffixSql(tableSuffixSql)
        //         .extraColumnSql(extraColumnSql)
        //         .setEnumValues(setEnumValues)
        //         .compact(compact)
        //         .rollingFile(rollingFile)
        //         .rollingFileMaxSqlCount(rollingFileMaxSqlCount)
        //         .dataSourceType(dataSourceType)
        //         .converterFile(converterFile)
        //         .converters(converters)
        //         .nullRatio(nullRatio)
        //         .enableNeg(enableNeg)
        //         .yes(yes)
        //         .debug(debug);
        // handler.run();
    }

}