package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.randi;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.SneakyThrows;
import org.dreamcat.common.argparse.ArgParserContext;
import org.dreamcat.common.argparse.ArgParserEntrypoint;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.math.CombinationUtil;
import org.dreamcat.common.util.CollectionUtil;
import org.dreamcat.common.util.NumberUtil;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@ArgParserType(firstChar = true, allProperties = true,
        command = "batch-type-table")
public class BatchTypeTableHandler extends BaseJdbcHandler
        implements ArgParserEntrypoint<BatchTypeTableHandler> {

    String file;
    Set<String> types; // combination
    @ArgParserField({"m"})
    Set<String> combinationMode = Collections.singleton("3-16"); // v1 v2 v3-v4 v5
    @ArgParserField({"r"})
    int combinationRepeat = 1;
    @ArgParserField
    String tableNamePrefix = "t_table_%d";
    @ArgParserField({"o"})
    int tableNameOffset = 0;
    @ArgParserField
    boolean ignoreError; //
    @ArgParserField
    String outputFile;
    @ArgParserField
    int outputFileSqlMaxCount = Integer.MAX_VALUE;

    @ArgParserField
    boolean commentAlone;
    @ArgParserField
    String columnCommentSql;
    @ArgParserField({"S"})
    private String dataSourceType;
    @ArgParserField
    private String converterFile;
    // binary:cast($value as $type)
    private Set<String> converters;
    @ArgParserField
    boolean doubleQuota; // "c1" or `c2`
    @ArgParserField
    String tableSuffixSql;
    @ArgParserField
    String extraColumnDdl;
    @ArgParserField({"b"})
    int batchSize = 1;
    @ArgParserField({"n"})
    int rowNum = randi(1, 76);
    @ArgParserField
    boolean debug;
    @ArgParserField(firstChar = true)
    private boolean help;

    transient int outputFileOffset = 1;
    transient int outputFileSqlCount; //

    @Override
    @SneakyThrows
    public void run(ArgParserContext<BatchTypeTableHandler> context) {
        if (help) {
            System.out.println(context.getHelp());
            return;
        }
        if (StringUtil.isBlank(file) && CollectionUtil.isEmpty(types)) {
            System.err.println("required arg: -f|--file <file> or -t|--types <t1> <t2>,...");
            System.exit(1);
        }
        List<List<String>> typesList;
        if (StringUtil.isNotBlank(file)) {
            typesList = FileUtil.readAsList(file).stream()
                    .filter(StringUtil::isNotBlank)
                    .map(String::trim)
                    .filter(it -> !it.startsWith("#"))
                    .map(String::trim)
                    .map(line -> Arrays.asList(line.split(", ")))
                    .collect(Collectors.toList());
        } else {
            int typeCount = types.size();
            if (typeCount > 30) {
                System.err.println("types is too much, must <= 30, but: " + typeCount);
                System.exit(1);
            }
            Set<Integer> combinationNums = new HashSet<>();
            for (String c : combinationMode) {
                String[] ss = c.split("-");
                if (ss.length > 1) {
                    IntStream.rangeClosed(Integer.parseInt(ss[0]), Integer.parseInt(ss[1]))
                            .forEach(combinationNums::add);
                } else combinationNums.add(Integer.parseInt(c));
            }

            typesList = new ArrayList<>();
            List<String> typeList = new ArrayList<>(types);
            for (int combinationNum : combinationNums) {
                if (combinationNum > typeCount) continue;
                CombinationUtil.all(combinationNum, typeCount).stream()
                        .map(indexes -> {
                            List<String> types = new ArrayList<>(combinationNum);
                            for (int i : indexes) {
                                types.add(typeList.get(i));
                            }
                            return types;
                        }).forEach(it -> {
                            // repeat it
                            int repeat = NumberUtil.limitRange(combinationRepeat, 1, 100);
                            for (int i = 0; i < repeat; i++) {
                                typesList.add(it);
                            }
                        });
            }
        }

        run(connection -> this.handle(connection, typesList));
    }

    private void handle(Connection connection, List<List<String>> typesList) {
        int size = typesList.size();
        for (int i = 1; i <= size; i++) {
            List<String> types = typesList.get(i - 1);
            try {
                handleOne(connection, types, i);
            } catch (Exception e) {
                if (!ignoreError) {
                    System.err.printf("error handle %s: %s%n", types, e.getMessage());
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private void handleOne(Connection connection, List<String> types, int index) throws Exception {
        String tableName = String.format(tableNamePrefix, index + tableNameOffset);
        TypeTableHandler typeTableHandler = new TypeTableHandler()
                .types(types)
                .tableName(tableName)
                .commentAlone(commentAlone)
                .columnCommentSql(columnCommentSql)
                .dataSourceType(dataSourceType)
                .converterFile(converterFile)
                .converters(converters)
                .doubleQuota(doubleQuota)
                .tableSuffixSql(tableSuffixSql)
                .extraColumnSql(extraColumnDdl)
                .batchSize(batchSize)
                .rowNum(rowNum)
                .debug(debug);
        typeTableHandler.afterPropertySet();
        List<String> sqlList = typeTableHandler.genSql();
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

    private void output(List<String> sqlList) throws Exception {
        for (String sql : sqlList) {
            System.out.println(sql);
        }
        if (ObjectUtil.isEmpty(outputFile)) return;

        outputFileSqlCount += sqlList.size();
        if (outputFileSqlCount > outputFileSqlMaxCount) {
            outputFileSqlCount = sqlList.size();
            outputFileOffset++;
        }
        String blockFile;
        int i = outputFile.lastIndexOf('.');
        if (i >= 0) {
            blockFile = outputFile.substring(0, i) + outputFileOffset + outputFile.substring(i);
        } else {
            blockFile = outputFile + outputFileOffset + ".sql";
        }
        FileUtil.writeFrom(blockFile, String.join("\n", sqlList), true);
    }
}