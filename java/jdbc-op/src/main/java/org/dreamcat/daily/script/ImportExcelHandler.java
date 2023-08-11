package org.dreamcat.daily.script;

import static org.dreamcat.common.util.CollectionUtil.mapToList;
import static org.dreamcat.common.util.FunctionUtil.firstNotNull;
import static org.dreamcat.common.util.ListUtil.getOrNull;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.dreamcat.common.excel.ExcelUtil;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.TextValueType;
import org.dreamcat.common.util.ArrayUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ListUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-06-28
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-excel")
public class ImportExcelHandler extends BaseDdlOutputHandler implements ArgParserEntrypoint {

    @ArgParserField("f")
    private String file;
    @ArgParserField("L")
    private boolean detectFromJdbc;
    @ArgParserField({"b"})
    int batchSize = 1;
    @ArgParserField("A")
    boolean castAs;

    @ArgParserField("t")
    private String textTypeFile;
    @ArgParserField("T")
    private String textTypeFileContent; // line sep by \n or ;, and field sep by |
    @ArgParserField("sn")
    private String sheetNames; // mapping to table name
    @ArgParserField("cn")
    private List<String> columnNames; // sep by ,

    transient EnumMap<TextValueType, List<String>> textTypeMap;
    transient List<String> sheetNameList = Collections.emptyList();
    transient List<List<String>> columnNameList = Collections.emptyList();

    @SneakyThrows
    @Override
    public void run(ArgParserContext context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }
        if (ObjectUtil.isEmpty(file)) {
            System.err.println("required arg: -f|--file <file>");
            System.exit(1);
        }
        if (!detectFromJdbc) {
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
        if (ObjectUtil.isNotEmpty(columnNames)) {
            columnNameList = mapToList(columnNames,
                    it -> ArrayUtil.mapToList(it.split(","), String::trim));
        }
        if (ObjectUtil.isNotEmpty(sheetNames)) {
            sheetNameList = ArrayUtil.mapToList(sheetNames.split(","), String::trim);
        }
        run();
    }

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
    }

    public void run() throws Exception {
        Map<String, List<List<Object>>> sheets = ExcelUtil.parseAsMap(new File(file));
        this.afterPropertySet();
        run(connection -> {
            int sheetIndex = 0;
            for (Entry<String, List<List<Object>>> entry : sheets.entrySet()) {
                String sheetName = entry.getKey();
                List<List<Object>> rows = entry.getValue();
                List<String> header = mapToList(rows.get(0), Objects::toString);
                rows = rows.subList(1, rows.size());

                // mapping sheet
                String mappingSheetName = getOrNull(sheetNameList, sheetIndex);
                if (ObjectUtil.isNotEmpty(mappingSheetName) && !"*".equals(mappingSheetName)) {
                    sheetName = mappingSheetName;
                }
                // mapping header
                List<String> list = getOrNull(columnNameList, sheetIndex);
                if (list != null) {
                    List<String> mappingHeader = new ArrayList<>(header.size());
                    for (int i = 0; i < header.size(); i++) {
                        String h = firstNotNull(getOrNull(list, i), header.get(i));
                        if (ObjectUtil.isEmpty(h) || "*".equals(h)) h = header.get(i);
                        mappingHeader.add(h);
                    }
                    header = mappingHeader;
                }

                List<TypeInfo> typeInfos = getTypeInfos(connection, sheetName, header, rows);
                List<String> sqlList = genSqlList(sheetName, rows, typeInfos);
                output(sqlList, connection);
                sheetIndex++;
            }
        });
    }

    public List<String> genSqlList(String tableName, List<List<Object>> rows, List<TypeInfo> typeInfos) {
        Pair<List<String>, List<String>> pair = genSql(tableName, rows, typeInfos);
        return CollectionUtil.concatToList(pair.first(), pair.second());
    }

    public Pair<List<String>, List<String>> genSql(String tableName, List<List<Object>> rows,
            List<TypeInfo> typeInfos) {
        // create
        Triple<List<String>, List<String>, List<String>> triple = genCreateTableSql(
                tableName, typeInfos, Collections.emptyList());
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
        List<List<Object>> list;
        while (!(list = ListUtil.subList(rows, pageNum++, batchSize)).isEmpty()) {
            String valueSql = getValues(list, typeInfos);
            if (valueSql == null) continue;
            insertList.add(String.format(insertIntoSql, valueSql));
        }

        return Pair.of(ddlList, insertList);
    }

    // (%s,%s,%s),(%s,%s,%s),(%s,%s,%s)
    private String getValues(List<List<Object>> rows, List<TypeInfo> typeInfos) {
        int count = typeInfos.size();
        List<String> valueSqlList = new ArrayList<>();
        for (List<Object> row : rows) {
            if (row == null) {
                System.err.println("found a null row");
                continue;
            }
            List<String> value = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Object cell = getOrNull(row, i);
                String literal = getOneValue(cell, typeInfos.get(i).getTypeId());
                value.add(literal);
            }
            valueSqlList.add("(" + String.join(",", value) + ")");
        }
        if (valueSqlList.isEmpty()) return null;
        else return String.join(",", valueSqlList);
    }

    private String getOneValue(Object value, String type) {
        String literal = gen.formatAsLiteral(value);
        String convertedLiteral = gen.convertLiteral(literal, type);
        if (!Objects.equals(convertedLiteral, literal)) return convertedLiteral;
        if (castAs) return String.format("cast(%s as %s)", literal, type);
        return literal;
    }

    private List<TypeInfo> getTypeInfos(Connection connection, String tableName,
            List<String> header, List<List<Object>> rows) throws SQLException {
        if (detectFromJdbc) {
            Pair<List<String>, List<String>> pair = TypeInfo.getTypes(connection, tableName);
            List<TypeInfo> typeInfos = pair.first().stream()
                    .map(type -> new TypeInfo(type, setEnumValues))
                    .collect(Collectors.toList());
            Set<String> headerSet = new HashSet<>(header);
            for (TypeInfo typeInfo : typeInfos) {
                if (!headerSet.contains(typeInfo.getColumnName())) {
                    System.err.println("column `" + columnNameTemplate + "` doesn't exist in table " + tableName);
                    System.exit(1);
                }
            }
            return typeInfos;
        } else {
            List<String> types = getTypesFromData(rows, header);
            return types.stream().map(type -> new TypeInfo(type, setEnumValues))
                    .collect(Collectors.toList());
        }
    }

    // data detect
    private List<String> getTypesFromData(List<List<Object>> rows, List<String> header) {
        int headerWidth = header.size();
        List<String> types = IntStream.range(0, headerWidth).mapToObj(i -> (String) null)
                .collect(Collectors.toList());
        Map<TextValueType, MutableInt> textTypeUsedIndexMap = new HashMap<>();
        for (List<Object> row : rows) {
            for (int i = 0; i < headerWidth; i++) {
                if (types.get(i) != null) continue;
                TextValueType textValueType = TextValueType.detectObject(row.get(i));
                if (TextValueType.NULL.equals(textValueType)) continue;

                List<String> candidateList = textTypeMap.get(textValueType);
                int index = textTypeUsedIndexMap.computeIfAbsent(textValueType, k -> new MutableInt(0)).getAndIncr();
                index %= candidateList.size();
                String type = candidateList.get(index);

                String alias = header.get(i);
                types.set(i, type + ": " + alias);
            }
        }
        return types;
    }

    private EnumMap<TextValueType, List<String>> getTextTypeMap() throws IOException {
        List<String> lines;
        if (ObjectUtil.isNotEmpty(textTypeFile)) {
            lines = FileUtil.readAsList(textTypeFile);
        } else {
            lines = Arrays.asList(textTypeFileContent.replace('\n', ';').split(";"));
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
                    List<String> types = Arrays.stream(pair[1].trim().split("\\|")).map(String::trim)
                            .collect(Collectors.toList());
                    return Pair.of(textValueType, types);
                }).collect(Collectors.toMap(Pair::getKey, Pair::getValue,
                        (a, b) -> a, () -> new EnumMap<>(TextValueType.class)));
    }
}
