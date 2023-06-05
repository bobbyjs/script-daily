package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.uuid32;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.util.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "type-table")
public class TypeTableHandler extends BaseTypeTableHandler implements ArgParserEntrypoint<TypeTableHandler> {

    // one type per line: varchar(%d), decimal(%d, %d)
    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("p")
    private List<String> partitionTypes;
    @ArgParserField(required = true, position = 0)
    private String tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);

    transient Map<String, MutableInt> columnNameCounter = new HashMap<>();
    transient Map<String, MutableInt> partitionColumnNameCounter = new HashMap<>();

    @SneakyThrows
    @Override
    public void run(ArgParserContext<TypeTableHandler> context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }

        if (ObjectUtil.isBlank(file) && ObjectUtil.isEmpty(types) &&
                ObjectUtil.isEmpty(fileContent)) {
            System.err.println(
                    "required arg: -f|--file <file> or -t|--types <t1> <t2>... or -F|--file-content <content>");
            System.exit(1);
        }
        if (ObjectUtil.isNotBlank(file) || ObjectUtil.isNotBlank(fileContent)) {
            List<String> lines;
            if (ObjectUtil.isNotBlank(file)) {
                lines = FileUtil.readAsList(file);
            } else {
                lines = Arrays.asList(fileContent.split("\n"));
            }
            types = lines.stream()
                    .filter(StringUtil::isNotBlank).map(String::trim)
                    .filter(it -> {
                        if (it.startsWith("@")) {
                            partitionTypes.add(it.substring(1));
                            return false;
                        }
                        return !it.startsWith("#");
                    }).map(String::trim).collect(Collectors.toList());
        }

        if (ObjectUtil.isEmpty(types)) {
            System.err.println("at least one type is required in file " + file);
            System.exit(1);
        }

        this.afterPropertySet();
        run(this::handle);
    }

    void handle(Connection connection) throws Exception {
        List<String> sqlList = genSql();

        if (!yes) {
            output(sqlList);
            return;
        }

        try (Statement statement = connection.createStatement()) {
            output(sqlList);
            for (String sql : sqlList) {
                statement.execute(sql);
            }
        }
    }

    public List<String> genSql() {
        // debug
        if (debug) {
            Stream.concat(types.stream(), partitionTypes.stream()).distinct().forEach(type -> {
                type = new TypeInfo(type, setEnumValues).getTypeId();
                String raw = gen.generateLiteral(type);
                System.out.println(type + ": " + raw);
            });
        }

        // generate
        List<String> sqlList = new ArrayList<>();
        StringBuilder createTableSql = new StringBuilder();
        String sep = compact ? " " : "\n";

        List<String> columnNames = new ArrayList<>();
        List<String> columnCommentSqlList = new ArrayList<>();
        String cols = types.stream().map(type -> this.fillColSql(type, columnNames, columnCommentSqlList))
                .collect(Collectors.joining("," + sep));
        createTableSql.append("create table ").append(tableName).append(" (").append(sep);
        if (StringUtil.isNotBlank(extraColumnSql)) {
            createTableSql.append("    ").append(extraColumnSql).append(",").append(sep);
        }
        createTableSql.append(cols).append(sep).append(")");
        List<String> partitionColumnNames = new ArrayList<>();
        String partitionDefSql = getPartitionDefSql(partitionColumnNames, sep);
        if (StringUtil.isNotEmpty(partitionDefSql)) createTableSql.append(" ").append(partitionDefSql);
        if (StringUtil.isNotBlank(tableSuffixSql)) {
            createTableSql.append(" ").append(tableSuffixSql);
        }
        createTableSql.append(";");
        sqlList.add(createTableSql.toString());

        for (String s : columnCommentSqlList) {
            if (!s.endsWith(";")) s += ";";
            sqlList.add(s);
        }

        String columnNameSql = StringUtil.join(",", columnNames, this::formatColumnName);
        String insertIntoSql = String.format(
                "insert into %s%s%%s values %%s;", tableName,
                CollectionUtil.isEmpty(partitionTypes) ? "(" + columnNameSql + ")" : "");
        while (rowNum > batchSize) {
            rowNum -= batchSize;
            sqlList.add(String.format(insertIntoSql, getPartitionValueSql(partitionColumnNames), getValues(batchSize)));
        }
        if (rowNum > 0) {
            sqlList.add(String.format(insertIntoSql, getPartitionValueSql(partitionColumnNames), getValues(rowNum)));
        }
        return sqlList;
    }

    private String fillColSql(String type, List<String> columnNames, List<String> columnCommentSqlList) {
        TypeInfo typeInfo = new TypeInfo(type, setEnumValues);

        int index = columnNameCounter.computeIfAbsent(
                typeInfo.getColumnName(), k -> new MutableInt(0)).incrAndGet();
        String col = InterpolationUtil.format(columnName,
                "t", typeInfo.getColumnName(), "type", typeInfo.getColumnName(),
                "i", index + "", "index", index + "");
        columnNames.add(col);

        String columnDefSql = formatColumnName(col);
        Map<String, Object> ctx = MapUtil.of(
                "table", tableName,
                "column", col,
                "type", typeInfo.getTypeName());

        columnDefSql = columnDefSql + " " + typeInfo.getTypeName();
        if (!compact) columnDefSql = "    " + columnDefSql;

        if (StringUtil.isNotBlank(columnCommentSql)) {
            String comment = InterpolationUtil.format(columnCommentSql, ctx);
            comment = StringUtil.escape(comment, "'");
            if (commentAlone) {
                ctx.put("comment", comment);
                columnCommentSqlList.add(InterpolationUtil.format(columnCommentSql, ctx));
            } else {
                columnDefSql = String.format("%s comment '%s'", columnDefSql, comment);
            }
        }
        return columnDefSql;
    }

    private String formatColumnName(String columnName) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }

    private String getValues(int n) {
        return IntStream.range(0, n).mapToObj(i -> getOneValues())
                .collect(Collectors.joining(","));
    }

    private String getOneValues() {
        return "(" + types.stream().map(type -> {
            type = new TypeInfo(type, setEnumValues).getTypeId();
            return gen.generateLiteral(type);
        }).collect(Collectors.joining(",")) + ")";
    }

    private String getPartitionDefSql(List<String> partitionColumnNames, String sep) {
        if (CollectionUtil.isEmpty(partitionTypes)) return "";
        List<String> list = new ArrayList<>();
        for (String partitionType : partitionTypes) {
            TypeInfo typeInfo = new TypeInfo(partitionType, setEnumValues);
            int index = partitionColumnNameCounter.computeIfAbsent(
                    typeInfo.getColumnName(), k -> new MutableInt(0)).incrAndGet();
            String columnName = InterpolationUtil.format(partitionColumnName,
                    "t", typeInfo.getColumnName(), "type", typeInfo.getColumnName(),
                    "i", index + "", "index", index + "");
            partitionColumnNames.add(columnName);
            String columnDefSql = columnName;
            if (!compact) columnDefSql = "    " + columnDefSql;
            list.add(columnDefSql + " " + typeInfo.getTypeName());
        }
        return String.format("partitioned by (%s%s%s)", sep, String.join(
                "," + sep, list), sep);
    }

    private String getPartitionValueSql(List<String> partitionColumnNames) {
        if (CollectionUtil.isEmpty(partitionTypes)) return "";
        List<String> list = new ArrayList<>();
        for (int i = 0, size = partitionTypes.size(); i < size; i++) {
            TypeInfo typeInfo = new TypeInfo(partitionTypes.get(i), setEnumValues);
            String columnName = partitionColumnNames.get(i);
            list.add(columnName + "=" + gen.generateLiteral(typeInfo.getTypeId()));
        }
        return String.format(" partition(%s)", String.join(",", list));
    }
}
