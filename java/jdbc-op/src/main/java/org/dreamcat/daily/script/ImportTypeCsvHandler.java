package org.dreamcat.daily.script;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Pair;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.CsvUtil;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-06-17
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-table-csv")
public class ImportTypeCsvHandler extends BaseDdlOutputHandler implements ArgParserEntrypoint {

    // partition is unsupported
    @ArgParserField("f")
    private String file;
    @ArgParserField("F")
    private String fileContent;
    @ArgParserField(required = true, position = 0)
    private String tableName;
    @ArgParserField("C")
    private boolean createTable;
    @ArgParserField({"b"})
    int batchSize = 1;

    private boolean tsv;
    private boolean emptyStringAsNull;
    @ArgParserField("t")
    private String textTypeFile;
    @ArgParserField("T")
    private String textTypeFileContent;

    transient EnumMap<TextValueType, List<String>> textTypeMap;

    @SneakyThrows
    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }
        run();
    }

    public void run() throws Exception {
        if (ObjectUtil.isEmpty(file) && ObjectUtil.isBlank(fileContent)) {
            System.err.println(
                    "required arg: -f|--file <file> or -F|--file-content <content>");
            System.exit(1);
        }
        if (createTable) {
            if (ObjectUtil.isEmpty(textTypeFile) && ObjectUtil.isBlank(textTypeFileContent)) {
                System.err.println("require args: -t|--text-type-file <file> or -T|--text-type-file-content <content> since --create-table is pass");
                System.exit(1);
            }
            this.textTypeMap = getTextTypeMap();
            if (!textTypeMap.containsKey(TextValueType.NULL)) {
                List<String> all = textTypeMap.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
                textTypeMap.put(TextValueType.NULL, all);
            }
            Set<TextValueType> both = new HashSet<>(Arrays.asList(TextValueType.values()));
            both.removeAll(textTypeMap.keySet());
            if (!both.isEmpty()) {
                System.err.println("miss text type: " + both);
                System.exit(1);
            }
        }

        List<List<String>> rows;
        if (ObjectUtil.isNotEmpty(file)) {
            if (tsv) {
                rows = CsvUtil.parseTsv(new File(file));
            } else {
                rows = CsvUtil.parse(new File(file));
            }
        } else {
            if (tsv) {
                rows = CsvUtil.parseTsv(new StringReader(fileContent));
            } else {
                rows = CsvUtil.parse(new StringReader(fileContent));
            }
        }
        if (rows.isEmpty()) {
            System.err.println("require some data in your csf file");
            System.exit(1);
        }
        if (rows.size() < 2) {
            System.err.println("require at least two rows in your data file");
            System.exit(1);
        }
        int headerWidth = rows.get(0).size();
        if (rows.stream().anyMatch(row -> row.size() < headerWidth)) {
            System.err.println("require same column width in your data file");
            System.exit(1);
        }

        run(connection -> this.handle(connection, rows));
    }

    protected void handle(Connection connection, List<List<String>> rows) throws Exception {
        List<String> header = rows.get(0);
        rows = rows.subList(1, rows.size());

        List<TypeInfo> typeInfos;
        if (createTable) {
            List<String> types = getTypesByData(rows, header);
            typeInfos = types.stream().map(type -> new TypeInfo(type, setEnumValues))
                    .collect(Collectors.toList());
        } else {
            // table already exists
            Pair<List<String>, List<String>> pair = TypeInfo.getTypes(connection, tableName);
            List<String> types = pair.first();
            Map<String, TypeInfo> typeInfoMap = types.stream().map(type -> new TypeInfo(type, setEnumValues))
                    .collect(Collectors.toMap(TypeInfo::getColumnName, Function.identity()));
            typeInfos = new ArrayList<>();
            for (String columnName : header) {
                TypeInfo typeInfo = typeInfoMap.get(columnName);
                if (typeInfo == null) {
                    System.err.println("column `" + columnName + "` doesn't exist in table " + tableName);
                    System.exit(1);
                }
                typeInfos.add(typeInfo);
            }
        }

        List<String> sqlList = genSqlList(rows, typeInfos);
        output(sqlList, connection);
    }

    public List<String> genSqlList(List<List<String>> rows, List<TypeInfo> typeInfos) {
        Pair<List<String>, List<String>> pair = genSql(rows, typeInfos);
        return CollectionUtil.concatToList(pair.first(), pair.second());
    }

    public Pair<List<String>, List<String>> genSql(List<List<String>> rows, List<TypeInfo> typeInfos) {
        // create
        Triple<List<String>, List<String>, List<String>> triple = genCreateTableSql(tableName, typeInfos,
                Collections.emptyList());
        List<String> ddlList = triple.first();
        List<String> columnNames = triple.second();

        // insert
        List<String> insertList = new ArrayList<>();
        String columnNameSql = StringUtil.join(
                ",", columnNames,
                this::formatColumnName);
        String insertIntoSql = String.format(
                "insert into %s(%s) values %%s;", tableName, columnNameSql);

        int pageNum = 1;
        List<List<String>> list;
        while (!(list = ListUtil.subList(rows, pageNum++, batchSize)).isEmpty()) {
            insertList.add(String.format(insertIntoSql, getValues(list, typeInfos)));
        }

        return Pair.of(ddlList, insertList);
    }

    // (%s,%s,%s),(%s,%s,%s),(%s,%s,%s)
    private String getValues(List<List<String>> rows, List<TypeInfo> typeInfos) {
        int count = typeInfos.size();
        List<String> valueSqlList = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> value = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String cell = row.get(i);
                String literal = getOneValue(cell, typeInfos.get(i).getTypeId());
                value.add(literal);
            }
            valueSqlList.add("(" + String.join(",", value) + ")");
        }
        return String.join(",", valueSqlList);
    }

    private String getOneValue(String value, String type) {
        if (emptyStringAsNull && ObjectUtil.isEmpty(value)) {
            return gen.nullLiteral();
        }
        Object sqlValue = TextValueType.parse(value);
        String literal = gen.formatAsLiteral(sqlValue);
        return gen.convertLiteral(literal, type);
    }

    // data detect
    private List<String> getTypesByData(List<List<String>> rows, List<String> header) {
        int headerWidth = header.size();
        List<Map<TextValueType, MutableInt>> typeCounter = new ArrayList<>(headerWidth);
        header.forEach(it -> {
            typeCounter.add(new LinkedHashMap<>());
        });

        for (List<String> row : rows) {
            for (int i = 0; i < headerWidth; i++) {
                String value = row.get(i);
                TextValueType textValueType = TextValueType.detect(value);
                typeCounter.get(i).computeIfAbsent(textValueType, k -> new MutableInt(0))
                        .incrAndGet();
            }
        }

        // compute top text-type
        List<String> types = new ArrayList<>();
        Map<TextValueType, MutableInt> textTypeUsedIndexMap = new HashMap<>();
        for (int i = 0; i < typeCounter.size(); i++) {
            Map<TextValueType, MutableInt> m = typeCounter.get(i);
            Entry<TextValueType, MutableInt> topEntry = null;
            for (Entry<TextValueType, MutableInt> entry : m.entrySet()) {
                if (topEntry == null || entry.getValue().compareTo(topEntry.getValue()) > 0) {
                    topEntry = entry;
                }
            }
            if (topEntry == null) {
                System.err.println("no enough data to detect text-type for column " + (i + 1));
                System.exit(1);
            }
            TextValueType textValueType = topEntry.getKey();
            List<String> candidateList = textTypeMap.get(textValueType);
            int index = textTypeUsedIndexMap.computeIfAbsent(textValueType, k -> new MutableInt(0)).getAndIncr();
            index %= candidateList.size();
            String type = candidateList.get(index);

            String alias = header.get(i);
            types.add(type + ": " + alias);
        }
        return types;
    }

    private EnumMap<TextValueType, List<String>> getTextTypeMap() throws IOException {
        List<String> lines;
        if (ObjectUtil.isNotEmpty(textTypeFile)) {
            lines = FileUtil.readAsList(textTypeFile);
        } else {
            lines = Arrays.asList(textTypeFileContent.split("\n"));
        }
        return lines.stream()
                .map(String::trim).filter(ObjectUtil::isNotBlank)
                .map(line -> {
                    String[] pair = line.split(":", 2);
                    if (pair.length != 2) {
                        System.err.println("invalid format line in your text-type-file: " + line);
                        System.exit(1);
                    }
                    String textType = pair[0].trim();
                    TextValueType textValueType = null;
                    for (TextValueType valueType : TextValueType.values()) {
                        if (valueType.name().equalsIgnoreCase(textType)) {
                            textValueType = valueType;
                            break;
                        }
                    }
                    if (textValueType == null) {
                        System.err.println("invalid format line in your text-type-file: " + line);
                        System.exit(1);
                    }
                    List<String> types = Arrays.stream(pair[1].trim().split(",")).map(String::trim)
                            .collect(Collectors.toList());
                    return Pair.of(textValueType, types);
                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue,
                        (a, b) -> a, () -> new EnumMap<>(TextValueType.class)));
    }
}
