package org.dreamcat.daily.script;

import static org.dreamcat.common.util.RandomUtil.choose36;
import static org.dreamcat.common.util.RandomUtil.rand;
import static org.dreamcat.common.util.RandomUtil.randi;
import static org.dreamcat.daily.script.Util.fromJdbcType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.dreamcat.common.Pair;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserInject;
import org.dreamcat.common.argparse.ArgParserInject.InjectMethod;
import org.dreamcat.common.argparse.ArgParserInject.InjectParam;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.argparse.TypeArgParser;
import org.dreamcat.common.reflect.ObjectType;
import org.dreamcat.common.reflect.RandomInstance;
import org.dreamcat.common.sql.JdbcColumnDef;
import org.dreamcat.common.sql.JdbcUtil;
import org.dreamcat.common.util.DateUtil;
import org.dreamcat.common.util.FunctionUtil;
import org.dreamcat.common.util.NumberUtil;
import org.dreamcat.common.util.StringUtil;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
@ArgParserType(firstChar = true, allProperties = true,
        command = "insert-random")
public class InsertRandomHandler extends BaseJdbcHandler {

    @ArgParserField(required = true, position = 0)
    private String tableName;
    @ArgParserField
    private String formattedNameTemplate; // such as `%s` or "%s"
    private Set<String> ignoredColumns;
    private int batchSize = 100;
    @ArgParserField({"n"})
    private int rowNum = randi(1, 314);
    @ArgParserField(firstChar = true)
    private boolean help;

    @ArgParserInject(method = InjectMethod.Action)
    public void run(@ArgParserInject(param = InjectParam.Help) String helpInfo) throws Exception {
        if (help) {
            System.out.println(helpInfo);
            return;
        }
        if (tableName == null) {
            System.err.println("required arg <tableName> is absent");
            System.exit(1);
        }

        run(this::handle);
    }

    private void handle(Connection connection) throws SQLException {
        String s = null, t = tableName;
        if (tableName.contains(".")) {
            String[] ss = tableName.split("\\.", 2);
            s = ss[0];
            t = ss[1];
        }
        List<JdbcColumnDef> columns = JdbcUtil.getTableSchema(connection, s, t);
        List<Pair<String, ObjectType>> needInsertColumns = columns.stream()
                .filter(it -> !this.ignoredColumns.contains(it.getName()))
                .map(it -> Pair.of(it.getName(), FunctionUtil.ifNull(fromJdbcType(it.getType()), ObjectType.fromType(Void.class))))
                .collect(Collectors.toList());

        String columnStr = needInsertColumns.stream()
                .map(Pair::first).map(this::formatName)
                .collect(Collectors.joining(", "));
        List<ObjectType> needInsertColumnTypes = needInsertColumns.stream().map(Pair::second)
                .collect(Collectors.toList());

        String sql = String.format("insert into %s (%s) values ", formatName(tableName), columnStr);
        try (Statement statement = connection.createStatement()) {
            while (rowNum > batchSize) {
                rowNum -= batchSize;
                executeSql(statement, sql + randomValues(needInsertColumnTypes, batchSize));
            }
            if (rowNum > 0) {
                executeSql(statement, sql + randomValues(needInsertColumnTypes, rowNum));
            }
        }
    }

    private String randomValues(List<ObjectType> needInsertColumnTypes, int n) {
        String valuesStr = IntStream.range(0, n)
                .mapToObj(i -> needInsertColumnTypes.stream()
                        .map(randomInstance::generate)
                        .map(this::convertValue)
                        .collect(Collectors.joining(", ")))
                .collect(Collectors.joining("), ("));
        return String.format("(%s)", valuesStr);
    }

    private String convertValue(Object value) {
        if (value instanceof Number) {
            Number n = (Number) value;
            if (NumberUtil.isFloatLike(n)) {
                return String.format("%.2f", n.doubleValue());
            } else {
                return n.toString();
            }
        }
        String s;
        if (value instanceof String) {
            s = StringUtil.escape((String) value, '\'');
        } else if (value instanceof LocalDate) {
            s = DateUtil.formatDate((LocalDate) value);
        } else if (value instanceof LocalTime) {
            s = DateUtil.formatTime((LocalTime) value);
        } else if (value instanceof LocalDateTime) {
            s = DateUtil.format((LocalDateTime) value);
        } else if (value instanceof byte[]) {
            s = new String((byte[]) value);
        } else if (value instanceof Date) {
            s = DateUtil.format((Date) value);
        } else if (value instanceof Boolean) {
            return value.toString();
        } else {
            s = Objects.toString(value);
        }
        return String.format("'%s'", s);
    }

    private void executeSql(Statement statement, String sql) throws SQLException {
        System.out.println(sql);
        if (!this.yes) return;
        statement.execute(sql);
    }

    private String formatName(String name) {
        if (formattedNameTemplate != null) {
            name = String.format(formattedNameTemplate, name);
        }
        return name;
    }

    /// randomInstance

    private static final RandomInstance randomInstance = new RandomInstance();
    static {
        randomInstance.register(() -> randi(0, 127), byte.class, Byte.class, short.class, Short.class,
                int.class, Integer.class, long.class, Long.class, BigInteger.class);
        randomInstance.register(() -> rand(0, 256), float.class, Float.class, double.class, Double.class,
                BigDecimal.class);
        randomInstance.register(randomInstance.getDefaultGen(Date.class), java.sql.Date.class);
        randomInstance.register(() -> choose36(randi(1, 8)), byte[].class);
    }
}
