package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserInject;
import org.dreamcat.common.argparse.ArgParserInject.InjectMethod;
import org.dreamcat.common.argparse.ArgParserInject.InjectParam;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.argparse.TypeArgParser;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.sql.JdbcTypeRandomInstance;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
@SuppressWarnings({"rawtypes"})
@ArgParserType(firstChar = true, allProperties = true,
        command = "type-table")
public class TypeTableHandler implements Base {

    // one type per line
    // varchar(%d)
    // decimal(%d, %d)
    private String file;
    private List<String> types;
    private List<String> partitionTypes;

    @ArgParserField(required = true, position = 0)
    private String tableName;
    @ArgParserField
    private boolean compact;
    @ArgParserField
    private boolean columnQuotation;
    // default use mysql column comment style
    @ArgParserField
    private boolean commentAlone;
    // such as: comment on column $table.$column is 'Column Type: $type'
    // or 'Column Type: $type' if commentAlone is ture
    private String columnCommentSql;
    // comment on column, or c int comment
    @ArgParserField
    private boolean postgresStyle;
    @ArgParserField
    private boolean doubleQuota; // "c1" or `c2`
    @ArgParserField
    private String tableDdlSuffix;
    @ArgParserField
    private String extraColumnDdl;
    @ArgParserField({"b"})
    private int batchSize = 1;
    @ArgParserField({"n"})
    private int rowNum = randi(1, 76);
    @ArgParserField
    private boolean debug;
    @ArgParserField(firstChar = true)
    private boolean help;

    @ArgParserInject(method = InjectMethod.Action)
    public void run(TypeArgParser argParser,  @ArgParserInject(param = InjectParam.Help) String helpInfo) throws Exception {
        if (help) {
            System.out.println(helpInfo);
            return;
        }

        if (StringUtil.isBlank(file) && CollectionUtil.isEmpty(types)) {
            System.err.println("required arg: -f|--file <file> or -t|--types <t1> <t2>,...");
            System.exit(1);
        }
        if (StringUtil.isNotBlank(file)) {
            types = FileUtil.readAsList(file).stream()
                    .filter(StringUtil::isNotBlank)
                    .map(String::trim)
                    .filter(it -> {
                        if (it.startsWith("@")) {
                            partitionTypes.add(it.substring(1));
                            return false;
                        }
                        return !it.startsWith("#");
                    })
                    .map(String::trim).collect(Collectors.toList());
        }
        if (CollectionUtil.isEmpty(types)) {
            System.err.println("at least one type is required in file " + file);
            System.exit(1);
        }

        List<String> sqlList = genSql();
        for (int i = 0, size = sqlList.size(); i < size; i++) {
            String sql = sqlList.get(i);
            if (compact) {
                if (i > 0) System.out.print(" ");
                System.out.print(sql);
            } else {
                System.out.println(sql);
            }
        }
        if (compact) System.out.println();
    }

    public List<String> genSql() {
        // debug
        if (debug) {
            Stream.concat(types.stream(), partitionTypes.stream()).distinct().forEach(type -> {
                type = getType(type).first();
                String raw = randomInstance.generateLiteral(type);
                System.out.println(type + ": " + raw);
            });
        }
        // afterPropertySet
        if (postgresStyle) {
            if (StringUtil.isNotBlank(columnCommentSql) && !columnCommentSql.trim().startsWith("comment on column")) {
                columnCommentSql = String.format("comment on column $table.$column is '%s'", columnCommentSql);
                commentAlone = true; // since use postgres style
                doubleQuota = true;
            }
        }

        List<String> sqlList = new ArrayList<>();
        StringBuilder createTableSql = new StringBuilder();
        String sep = compact ? " " : "\n";

        List<String> columnNames = new ArrayList<>();
        List<String> columnCommentSqlList = new ArrayList<>();
        String cols = types.stream().map(type -> this.fillColSql(type, columnNames, columnCommentSqlList))
                .collect(Collectors.joining("," + sep));
        createTableSql.append("create table ").append(tableName).append(" (").append(sep);
        if (StringUtil.isNotBlank(extraColumnDdl)) {
            createTableSql.append("    ").append(extraColumnDdl).append(",").append(sep);
        }
        createTableSql.append(cols).append(sep).append(")");
        String partitionDefSql = getPartitionDefSql(sep);
        if (partitionDefSql != null) createTableSql.append(" ").append(partitionDefSql);
        if (StringUtil.isNotBlank(tableDdlSuffix)) {
            createTableSql.append(" ").append(tableDdlSuffix);
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
            sqlList.add(String.format(insertIntoSql, getPartitionValueSql(), getValues(batchSize)));
        }
        if (rowNum > 0) {
            sqlList.add(String.format(insertIntoSql, getPartitionValueSql(), getValues(rowNum)));
        }
        return sqlList;
    }

    private Pair<String, String> getType(String type) {
        String typeName = type, typeVal = type;
        if (type.equals("set") || type.equals("enum")) {
            int n = randi(2, randomInstance.maxArrayLength() + 1);
            typeVal = String.format("%s(%s)", type, IntStream.range(0, 3 * n + 2)
                    .mapToObj(i -> randomInstance.generateLiteral("string"))
                    .distinct().limit(randi(1, n))
                    .collect(Collectors.joining(",")));
            return Pair.of(typeVal, typeVal);
        }

        int d = type.split("%d").length;
        if (d == 2) {
            typeVal = type.replaceAll("%d", "16");
        } else if (d == 3) {
            typeVal = type.replaceFirst("%d", "16")
                    .replaceFirst("%d", "6");
        } else if (d != 1) {
            System.err.println("invalid type: " + type);
            System.exit(1);
        }
        typeName = typeName.replace("%d", "")
                .replace("(", "")
                .replace(")", "")
                .replace(",", "");
        return Pair.of(typeName, typeVal);
    }

    private String fillColSql(String type, List<String> columnNames, List<String> columnCommentSqlList) {
        Pair<String, String> pair = getType(type);
        String col = "c_" + pair.first()
                .replace(' ', '_')
                .replace(',', '_')
                .replace("(", "_")
                .replace(")", "")
                .replace("'", "");
        columnNames.add(col);
        col = formatColumnName(col);
        Map<String, Object> ctx = MapUtil.of(
                "table", tableName,
                "column", col,
                "type", pair.second());

        col = col + " " + pair.second();
        if (!compact) col = "    " + col;

        if (StringUtil.isNotBlank(columnCommentSql)) {
            String comment = InterpolationUtil.format(columnCommentSql, ctx);
            comment = StringUtil.escape(comment, "'");
            if (commentAlone) {
                ctx.put("comment", comment);
                columnCommentSqlList.add(InterpolationUtil.format(columnCommentSql, ctx));
            } else {
                col = String.format("%s comment '%s'", col, comment);
            }
        }
        return col;
    }

    private String formatColumnName(String columnName) {
        if (!columnQuotation) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }

    private String getValues(int n) {
        return IntStream.range(0, n).mapToObj(i -> getOneValues())
                .collect(Collectors.joining(","));
    }

    private String getOneValues() {
        return "(" + types.stream().map(type -> {
            type = getType(type).first();
            String raw = randomInstance.generateLiteral(type);
            // fit postgresql
            if (postgresStyle) {
                if (type.equals("bit")) {
                    raw += "::bit(16)";
                }
            }
            return raw;
        }).collect(Collectors.joining(",")) + ")";
    }

    private String getPartitionDefSql(String sep) {
        if (CollectionUtil.isEmpty(partitionTypes)) return "";
        List<String> list = new ArrayList<>();
        for (String partitionType : partitionTypes) {
            Pair<String, String> pair = getType(partitionType);
            String columnName = "p_" + pair.first().replace(' ', '_');
            if (!compact) columnName = "    " + columnName;
            list.add(columnName + " " + pair.second());
        }
        return String.format("partitioned by (%s%s%s)", sep, String.join(
                "," + sep, list), sep);
    }

    private String getPartitionValueSql() {
        if (CollectionUtil.isEmpty(partitionTypes)) return "";
        List<String> list = new ArrayList<>();
        for (String partitionType : partitionTypes) {
            Pair<String, String> pair = getType(partitionType);
            String columnName = "p_" + pair.first().replace(' ', '_');
            list.add(columnName + "=" + randomInstance.generateLiteral(pair.second()));
        }
        return String.format(" partition(%s)", String.join(",", list));
    }

    private static final JdbcTypeRandomInstance randomInstance = new JdbcTypeRandomInstance()
            .maxBitLength(1);
}
