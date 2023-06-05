package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.sql.SqlValueRandomGenerator;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.util.ConverterInfo;
import org.dreamcat.daily.script.util.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true,
        command = "type-table")
public class TypeTableHandler implements ArgParserEntrypoint<TypeTableHandler> {

    // one type per line
    // varchar(%d)
    // decimal(%d, %d)
    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("p")
    private List<String> partitionTypes;

    @ArgParserField(required = true, position = 0)
    private String tableName;
    @ArgParserField("C")
    private String columnNameTemplate = "c_$type";
    @ArgParserField("P")
    private String partitionColumnNameTemplate = "p_$type";

    private boolean compact;
    private boolean columnQuota;
    // comment on column, or c int comment
    // default use mysql column comment style
    private boolean commentAlone;
    // such as: comment on column $table.$column is 'Column Type: $type'
    // or 'Column Type: $type' if commentAlone is ture
    @ArgParserField("c")
    private String columnCommentSql;
    // postgresql, mysql, oracle, db2, sqlserver, and so on
    private boolean doubleQuota; // "c1" or `c2`
    private String tableSuffixSql;
    private String extraColumnSql;

    @ArgParserField("S")
    private String dataSourceType;
    private String converterFile;
    // binary:cast($value as $type)
    private Set<String> converters;

    @ArgParserField({"b"})
    private int batchSize = 1;
    @ArgParserField({"n"})
    private int rowNum = randi(1, 76);
    private boolean debug;
    @ArgParserField(firstChar = true)
    private boolean help;
    private String setEnumValues = "a,b,c,d";

    // mappingType -> types
    transient Map<String, List<String>> mappingTypes;
    transient Map<String, List<String>> mappingPartitionTypes;
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
            System.err.println("required arg: -f|--file <file> or -t|--types <t1> <t2>... or -F|--file-content <content>");
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

        if (ObjectUtil.isEmpty(types)) {
            System.err.println("at least one type is required in file " + file);
            System.exit(1);
        }
        if (ObjectUtil.isBlank(tableName)) {
            tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);
        }

        this.afterPropertySet();
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

    void afterPropertySet() throws Exception {
        // builtin converters
        Map<String, List<ConverterInfo>> converterInfos = YamlUtil.fromJson(
                ClassPathUtil.getResourceAsString("converters.yaml"),
                new TypeReference<Map<String, List<ConverterInfo>>>() {
                });
        registerConvertors(converterInfos);
        // customer converters
        if (StringUtil.isNotEmpty(converterFile)) {
            converterInfos = YamlUtil.fromJson(new File(converterFile),
                    new TypeReference<Map<String, List<ConverterInfo>>>() {
                    });
            registerConvertors(converterInfos);
        }
        for (String converter : converters) {
            String[] ss = converter.split(",", 2);
            if (ss.length != 2) {
                throw new IllegalArgumentException("invalid converter: " + converter);
            }
            String type = ss[0], template = ss[1];
            registerConvertor(type, template);
        }

        if (Arrays.asList("pg", "postgres", "postgresql").contains(dataSourceType)) {
            if (StringUtil.isNotBlank(columnCommentSql) && !columnCommentSql.trim().startsWith("comment on column")) {
                columnCommentSql = String.format("comment on column $table.$column is '%s'", columnCommentSql);
                commentAlone = true; // since use postgres style
                doubleQuota = true;
            }
        }
    }

    private void registerConvertors(Map<String, List<ConverterInfo>> converterInfoMap) {
        Map<String, List<ConverterInfo>> map = new HashMap<>();
        converterInfoMap.forEach((k, v) -> {
            for (String ds : k.split(",")) {
                if (StringUtil.isNotEmpty(ds)) map.put(ds, v);
            }
        });
        List<ConverterInfo> converterInfos = map.get(dataSourceType);
        if (converterInfos == null) return;

        for (ConverterInfo converterInfo : converterInfos) {
            for (String type : converterInfo.getTypes()) {
                registerConvertor(type, converterInfo.getTemplate());
            }
        }
    }

    private void registerConvertor(String type, String template) {
        gen.registerConvertor((literal, typeName) -> InterpolationUtil.format(
                template, MapUtil.of("value", literal, "type", type)), type);
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
        String col = InterpolationUtil.format(columnNameTemplate,
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
            String columnName = InterpolationUtil.format(partitionColumnNameTemplate,
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

    private final SqlValueRandomGenerator gen = new SqlValueRandomGenerator()
            .maxBitLength(1)
            .addEnumAlias("enum8") // clickhouse
            .addEnumAlias("enum16");
}
