package org.dreamcat.daily.script;

import static org.dreamcat.common.util.CollectionUtil.mapToList;
import static org.dreamcat.common.util.FunctionUtil.firstNotNull;
import static org.dreamcat.common.util.ListUtil.getOrNull;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.excel.ExcelUtil;
import org.dreamcat.common.util.ArrayUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.model.TypeInfo;
import org.dreamcat.daily.script.module.TextTypeModule;

/**
 * @author Jerry Will
 * @version 2023-06-28
 */
@Setter
@Accessors(fluent = true)
@ArgParserType(allProperties = true, command = "import-excel")
public class ImportExcelHandler extends BaseDdlHandler {

    @ArgParserField("f")
    private String file;
    @ArgParserField("E")
    private boolean existing;
    @ArgParserField("A")
    boolean castAs;

    @ArgParserField("sn")
    private String sheetNames; // mapping to table name
    @ArgParserField("cn")
    private List<String> columnNames; // sep by ,

    @ArgParserField(nested = true)
    TextTypeModule textType;

    transient List<String> sheetNameList = Collections.emptyList();
    transient List<List<String>> columnNameList = Collections.emptyList();

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();
        if (ObjectUtil.isEmpty(file)) {
            System.err.println("required arg: -f|--file <file>");
            System.exit(1);
        }
        if (!existing) {
            if (debug) {
                System.out.println("I will require the textTypes args since --existing isn't passed");
            }
            textType.afterPropertySet();
        }
        if (ObjectUtil.isNotEmpty(columnNames)) {
            columnNameList = mapToList(columnNames,
                    it -> ArrayUtil.mapToList(it.split(","), String::trim));
        }
        if (ObjectUtil.isNotEmpty(sheetNames)) {
            sheetNameList = ArrayUtil.mapToList(sheetNames.split(","), String::trim);
        }
    }

    protected String getDefaultColumnName() {
        return "$name";
    }

    protected String getDefaultPartitionColumnName() {
        return "$name";
    }

    @Override
    public void run() throws Exception {
        Map<String, List<List<Object>>> sheets = ExcelUtil.parseAsMap(new File(file));
        this.afterPropertySet();
        jdbc.run(connection -> {
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
                Pair<List<String>, List<String>> pair = genSql(sheetName, rows, typeInfos);
                List<String> sqlList = CollectionUtil.concatToList(pair.first(), pair.second());
                output(sqlList, connection);
                sheetIndex++;
            }
        });
    }

    private List<TypeInfo> getTypeInfos(Connection connection, String tableName,
            List<String> header, List<List<Object>> rows) throws SQLException {
        if (existing) {
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
        for (List<Object> row : rows) {
            for (int i = 0; i < headerWidth; i++) {
                if (types.get(i) != null) continue;
                String type = textType.computeCandidateType(row.get(i));
                if (type == null) continue;

                String alias = header.get(i);
                types.set(i, type + ": " + alias);
            }
        }
        return types;
    }
}
