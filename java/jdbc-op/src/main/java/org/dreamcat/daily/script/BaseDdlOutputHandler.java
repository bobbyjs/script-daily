package org.dreamcat.daily.script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.Triple;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.model.TypeInfo;

/**
 * @author Jerry Will
 * @version 2023-06-27
 */
@Setter
@Accessors(fluent = true)
public class BaseDdlOutputHandler extends BaseOutputHandler {

    @ArgParserField("cn")
    String columnName = "c_$type";
    @ArgParserField("pcn")
    String partitionColumnName = "p_$type";
    boolean columnQuota;
    // comment on column, or c int comment
    // default use mysql column comment style
    boolean commentAlone;
    // such as: comment on column $table.$column is 'Column Type: $type'
    // or 'Column Type: $type' if commentAlone is ture
    @ArgParserField("c")
    String columnCommentSql;
    // postgresql, mysql, oracle, db2, sqlserver, and so on
    String tableSuffixSql;
    String extraColumnSql;
    boolean doubleQuota; // "c1" or `c2`
    String setEnumValues = "a,b,c,d";

    transient Map<String, MutableInt> columnNameCounter = new HashMap<>();
    transient Map<String, MutableInt> partitionColumnNameCounter = new HashMap<>();

    @Override
    protected void afterPropertySet() throws Exception {
        super.afterPropertySet();

        if (Arrays.asList("pg", "postgres", "postgresql").contains(dataSourceType)) {
            if (StringUtil.isNotBlank(columnCommentSql) && !columnCommentSql.trim().startsWith("comment on column")) {
                columnCommentSql = String.format("comment on column $table.$column is '%s'", columnCommentSql);
                commentAlone = true; // since use postgres style
                doubleQuota = true;
            }
        }
    }

    protected String formatColumnName(String columnName) {
        if (!columnQuota) return columnName;
        return StringUtil.escape(columnName, doubleQuota ? "\"" : "`");
    }

    // return createTableSql, columnNames, partitionColumnNames
    public Triple<List<String>, List<String>, List<String>> genCreateTableSql(String tableName, List<TypeInfo> typeInfos,
            List<TypeInfo> partitionTypeInfos) {
        List<String> ddlList = new ArrayList<>();

        String sep = compact ? " " : "\n";
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("create table ").append(tableName).append(" (").append(sep);
        if (StringUtil.isNotBlank(extraColumnSql)) {
            createTableSql.append("    ").append(extraColumnSql).append(",").append(sep);
        }

        // column
        int columnCount = typeInfos.size();
        List<String> columnDefSqlList = new ArrayList<>(columnCount);
        List<String> columnCommentSqlList = new ArrayList<>(columnCount);

        List<String> columnNames = new ArrayList<>(columnCount);
        for (TypeInfo typeInfo : typeInfos) {
            String columnDefSql = getColumnDefSqlAndFillComment(
                    typeInfo, tableName,
                    columnNames, columnCommentSqlList, columnNameCounter);
            columnDefSqlList.add(columnDefSql);
        }
        createTableSql.append(String.join("," + sep, columnDefSqlList))
                .append(sep).append(")");

        // partition
        int partitionColumnCount = partitionTypeInfos.size();
        List<String> partitionColumnDefSqlList = new ArrayList<>(partitionColumnCount);

        List<String> partitionColumnNames = new ArrayList<>(partitionColumnCount);
        for (TypeInfo partitionTypeInfo : partitionTypeInfos) {
            String partitionColumnDefSql = getColumnDefSqlAndFillComment(
                    partitionTypeInfo, tableName,
                    partitionColumnNames, columnCommentSqlList, partitionColumnNameCounter);
            partitionColumnDefSqlList.add(partitionColumnDefSql);
        }
        if (ObjectUtil.isNotEmpty(partitionColumnDefSqlList)) {
            createTableSql.append(" partitioned by (").append(sep)
                    .append(String.join("," + sep, partitionColumnDefSqlList))
                    .append(sep).append(")");
        }

        // suffix
        if (StringUtil.isNotBlank(tableSuffixSql)) {
            createTableSql.append(" ").append(tableSuffixSql);
        }
        createTableSql.append(";");
        ddlList.add(createTableSql.toString());

        for (String s : columnCommentSqlList) {
            if (!s.endsWith(";")) s += ";";
            ddlList.add(s);
        }

        return Triple.of(ddlList, columnNames, partitionColumnNames);
    }

    private String getColumnDefSqlAndFillComment(TypeInfo typeInfo, String tableName,
            List<String> columnNames, List<String> columnCommentSqlList,
            Map<String, MutableInt> counter) {
        String col = typeInfo.computeColumnName(columnName, counter);
        columnNames.add(col);

        String columnDefSql = formatColumnName(col) + " " + typeInfo.getTypeName();
        if (!compact) columnDefSql = "    " + columnDefSql;

        if (StringUtil.isNotBlank(columnCommentSql)) {
            Map<String, Object> ctx = MapUtil.of(
                    "table", tableName, "column", col, "type", typeInfo.getTypeName());
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
}
