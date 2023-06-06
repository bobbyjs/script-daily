package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.MutableInt;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.sql.SqlValueRandomGenerator;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.util.ConverterInfo;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
public class BaseTypeTableHandler extends BaseJdbcHandler {

    @ArgParserField("C")
    String columnName = "c_$type";
    @ArgParserField("P")
    String partitionColumnName = "p_$type";
    boolean compact;
    boolean columnQuota;
    // comment on column, or c int comment
    // default use mysql column comment style
    boolean commentAlone;
    // such as: comment on column $table.$column is 'Column Type: $type'
    // or 'Column Type: $type' if commentAlone is ture
    @ArgParserField("c")
    String columnCommentSql;
    // postgresql, mysql, oracle, db2, sqlserver, and so on
    boolean doubleQuota; // "c1" or `c2`
    String tableSuffixSql;
    String extraColumnSql;

    @ArgParserField("S")
    String dataSourceType;
    String converterFile;
    // binary:cast($value as $type)
    Set<String> converters;

    @ArgParserField({"b"})
    int batchSize = 1;
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);
    boolean debug;
    @ArgParserField(firstChar = true)
    boolean help;
    String setEnumValues = "a,b,c,d";
    @ArgParserField({"R"})
    String rollingFile; // such as: output_${i+100}.sql
    @ArgParserField({"M"})
    int rollingFileMaxSqlCount = Integer.MAX_VALUE;

    transient int sqlCountCounter;
    transient int rollingFileIndex = 1;

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

    void output(List<String> sqlList) throws IOException {
        int size = sqlList.size();
        for (int i = 0; i < size; i++) {
            String sql = sqlList.get(i);
            if (compact) {
                if (i > 0) System.out.print(" ");
                System.out.print(sql);
            } else {
                System.out.println(sql);
            }
        }
        if (compact) System.out.println();

        if (ObjectUtil.isEmpty(rollingFile)) return;

        int expect = rollingFileMaxSqlCount - sqlCountCounter;
        int offset = 0;
        while (expect < size) {
            List<String> subSqlList = sqlList.subList(offset, expect);
            String blockFile = InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex);
            FileUtil.writeFrom(blockFile, String.join("\n", subSqlList) + "\n", true);
            offset += expect;
            size -= expect;
            expect = rollingFileMaxSqlCount;
            sqlCountCounter = 0;
            rollingFileIndex++;
        }
        if (size > 0) {
            sqlCountCounter += size;
            List<String> subSqlList = sqlList.subList(offset, sqlList.size());
            String blockFile = InterpolationUtil.formatEl(rollingFile, "i", rollingFileIndex);
            FileUtil.writeFrom(blockFile, String.join("\n", subSqlList) + "\n", true);
        }
    }

    final SqlValueRandomGenerator gen = new SqlValueRandomGenerator()
            .maxBitLength(1)
            .addEnumAlias("enum8") // clickhouse
            .addEnumAlias("enum16");
}
