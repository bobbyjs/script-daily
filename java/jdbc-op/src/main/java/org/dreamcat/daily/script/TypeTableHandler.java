package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.common.util.RandomUtil.uuid32;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "type-table")
public class TypeTableHandler extends BaseDdlOutputHandler implements ArgParserEntrypoint {

    // one type per line: varchar(%d), decimal(%d, %d)
    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField("t")
    private List<String> types;
    @ArgParserField("P")
    private List<String> partitionTypes;
    @ArgParserField(required = true, position = 0)
    private String tableName = "t_" + StringUtil.reverse(uuid32()).substring(0, 8);
    // like this 'ratio,rows', example: 0.5,10
    private String rowNullRatio;
    @ArgParserField({"b"})
    int batchSize = 1;
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);

    transient RowNullRatioBasedGen rowNullRatioBasedGen;

    @SneakyThrows
    @Override
    public void run(ArgParserContext context) {
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
        this.reset();
        // debug
        if (debug) {
            Stream.concat(types.stream(), partitionTypes.stream()).distinct().forEach(type -> {
                type = new TypeInfo(type, setEnumValues).getTypeId();
                String raw = gen.generateLiteral(type);
                System.out.println(type + ": " + raw);
            });
        }
        run(connection -> output(genSqlList(), connection));
    }

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();

        if (ObjectUtil.isNotBlank(rowNullRatio)) {
            Pair<Double, Integer> pair = Pair.fromSep(rowNullRatio, ",",
                    Double::valueOf, Integer::valueOf);
            if (!pair.isFull()) {
                throw new IllegalArgumentException(
                        "invalid smartRowNullRatio: " + rowNullRatio);
            }
            double ratio = pair.first();
            int rows = pair.second();
            this.rowNullRatioBasedGen = new RowNullRatioBasedGen(ratio, rows);
        }
    }

    @Override
    protected String getDefaultColumnName() {
        return "c_$name";
    }

    @Override
    protected String getDefaultPartitionColumnName() {
        return "p_$name";
    }

    public List<String> genSqlList() {
        Pair<List<String>, List<String>> pair = genSql();
        return CollectionUtil.concatToList(pair.first(), pair.second());
    }

    // generate ddl & dml
    public Pair<List<String>, List<String>> genSql() {
        // generate

        Triple<List<String>, List<String>, List<String>> triple = genCreateTableSql(tableName,
                CollectionUtil.mapToList(types,
                        type -> new TypeInfo(type, setEnumValues)),
                CollectionUtil.mapToList(partitionTypes,
                        type -> new TypeInfo(type, setEnumValues)));

        List<String> ddlList = triple.first();
        List<String> columnNames = triple.second();
        List<String> partitionColumnNames = triple.third();

        // insert
        List<String> insertList = new ArrayList<>();
        String columnNameSql = StringUtil.join(",", columnNames, this::formatColumnName);
        String insertIntoSql = String.format(
                "insert into %s%s%%s values %%s;", tableName,
                CollectionUtil.isEmpty(partitionTypes) ? "(" + columnNameSql + ")" : "");
        while (rowNum > batchSize) {
            rowNum -= batchSize;
            insertList.add(String.format(insertIntoSql,
                    getPartitionValue(partitionColumnNames), getValues(batchSize)));
        }
        if (rowNum > 0) {
            insertList.add(String.format(insertIntoSql,
                    getPartitionValue(partitionColumnNames), getValues(rowNum)));
        }
        return Pair.of(ddlList, insertList);
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

    private String getPartitionValue(List<String> partitionColumnNames) {
        if (CollectionUtil.isEmpty(partitionTypes)) return "";
        List<String> list = new ArrayList<>();
        for (int i = 0, size = partitionTypes.size(); i < size; i++) {
            TypeInfo typeInfo = new TypeInfo(partitionTypes.get(i), setEnumValues);
            String columnName = partitionColumnNames.get(i);
            list.add(columnName + "=" + gen.generateLiteral(typeInfo.getTypeId()));
        }
        return String.format(" partition(%s)", String.join(",", list));
    }

    @Override
    protected String convert(String literal, String typeName) {
        literal = super.convert(literal, typeName);
        if (literal != null) return literal;
        if (rowNullRatioBasedGen != null && rowNullRatioBasedGen.generate()) {
            return gen.nullLiteral();
        }
        return null;
    }

    void reset() {
        if (rowNullRatioBasedGen != null) {
            rowNullRatioBasedGen.reset(types.size());
        }
    }

    static class RowNullRatioBasedGen {

        final int rows;
        final double ratio;
        int columns;
        int total;
        int offset;

        RowNullRatioBasedGen(double ratio, int rows) {
            this.ratio = ratio;
            this.rows = rows;
        }

        void reset(int columns) {
            this.columns = columns;
            this.total = columns * rows;
            this.offset = 0;
        }

        boolean generate() {
            if (total == 0) {
                throw new IllegalStateException("must call rest once before call generate");
            }
            if (offset >= total) {
                offset = 0;
            }
            if (offset++ < columns) {
                return Math.random() <= ratio;
            }
            return false;
        }
    }
}
