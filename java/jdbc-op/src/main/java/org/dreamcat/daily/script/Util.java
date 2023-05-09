package org.dreamcat.daily.script;

import static org.dreamcat.common.reflect.ObjectType.fromArrayType;
import static org.dreamcat.common.reflect.ObjectType.fromType;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.dreamcat.common.reflect.ObjectType;

;

/**
 * @author Jerry Will
 * @version 2023-03-22
 */
public class Util {

    /**
     * convert TYPE_NAME to java type
     *
     * @param jdbcType TYPE_NAME
     * @return java type
     * @see java.sql.JDBCType
     */
    public static ObjectType fromJdbcType(String jdbcType) {
        jdbcType = jdbcType.toLowerCase();
        switch (jdbcType) {
            case "bit":
            case "varbyte":
            case "binary":
            case "varbinary":
            case "longbinary":
            case "blob":
            case "tinyblob":
            case "mediumblob":
            case "longblob":
            case "bytea":
                return fromArrayType(byte.class);
            case "bool":
            case "boolean":
                return fromType(Boolean.class);
            case "int1":
            case "byte":
            case "byteint":
            case "tinyint":
                return fromType(Byte.class);
            case "int2":
            case "short":
            case "smallint":
                return fromType(Short.class);
            case "int4":
            case "int":
            case "integer":
                return fromType(Integer.class);
            case "int8":
            case "long":
            case "bigint":
                return fromType(Long.class);
            case "float":
            case "float32":
                return fromType(Float.class);
            case "float64":
            case "double":
            case "real":
                return fromType(Double.class);
            case "numeric":
            case "decimal":
                return fromType(BigDecimal.class);
            case "name":
            case "char":
            case "varchar":
            case "long varchar":
            case "graphic":
            case "vargraphic":
            case "long vargraphic":
            case "text":
            case "tinytext":
            case "mediumtext":
            case "longtext":
            case "clob":
            case "json":
            case "xml":
            case "string":
                return fromType(String.class);
            case "date":
                return fromType(LocalDate.class);
            case "time":
            case "time with time zone":
                return fromType(LocalTime.class);
            case "datetime":
            case "timestamp with time zone":
                return fromType(LocalDateTime.class);
            case "timestamp":
                return fromType(Date.class);
            default:
                return ObjectType.VOID; // unsupported type
        }
    }
}
