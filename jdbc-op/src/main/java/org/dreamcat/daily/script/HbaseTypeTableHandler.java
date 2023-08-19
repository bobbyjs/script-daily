package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.daily.script.model.TypeInfo;
import org.dreamcat.daily.script.module.RandomGenModule;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@ArgParserType(allProperties = true, command = "hbase-type-table")
public class HbaseTypeTableHandler implements ArgParserEntrypoint {

    @ArgParserField("f")
    private String file;
    @ArgParserField("t")
    private List<String> types;
    @ArgParserField(position = 0)
    private String tableName;
    @ArgParserField({"n"})
    private int rowNum = randi(1, 76);
    private boolean debug;
    @ArgParserField(firstChar = true)
    private boolean help;
    private String setEnumValues = "a,b,c,d";

    @ArgParserField(nested = true)
    RandomGenModule randomGenModule;

    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }

        List<String> sqlList = genSql();
        for (String sql : sqlList) {
            System.out.println(sql);
        }
    }

    public List<String> genSql() {
        // debug
        if (debug) {
            types.forEach(type -> {
                type = new TypeInfo(type, setEnumValues).getTypeId();
                String raw = randomGenModule.generateLiteral(type);
                System.out.println(type + ": " + raw);
            });
        }

        // generate
        List<String> sqlList = new ArrayList<>();
        // create 'mytable', 'c_int', 'c_string', 'c_date'
        String createTableSql = String.format("create '%s', %s\n", tableName, types.stream()
                .map(type -> "'c_" + new TypeInfo(type, setEnumValues).getColumnName() + "'")
                .collect(Collectors.joining(", ")));
        sqlList.add(createTableSql);

        // put 'talbe_name', 'row', 'colfamily:colname', 'value'

        return sqlList;
    }
}
