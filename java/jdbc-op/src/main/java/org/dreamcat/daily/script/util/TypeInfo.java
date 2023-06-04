package org.dreamcat.daily.script.util;

import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dreamcat.common.Triple;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeInfo {

    private String columnName; // sql column, no prefix c_ or p
    private String typeName; // sql type
    private String typeId; // generator

    // type: such as `$type` or `$type: $mappingType`
    public TypeInfo(String type, String setEnumValues) {
        String[] tt = type.split(":");
        if (tt.length > 1) {
            type = tt[0].trim();
            this.columnName = tt[1].trim();
        }
        if (Arrays.asList("set", "enum", "enum8", "enum16").contains(type.toLowerCase())) {
            this.typeName = this.typeId = String.format("%s(%s)", type, Arrays.stream(setEnumValues.split(","))
                    .map(it -> "'" + it + "'")
                    .collect(Collectors.joining(",")));
            if (this.columnName == null) this.columnName = type;
            return;
        }

        int d = type.split("%d").length;
        if (d == 1) {
            this.typeName = type;
        } else if (d == 2) {
            this.typeName = type.replaceAll("%d", "16");
        } else if (d == 3) {
            this.typeName = type.replaceFirst("%d", "16")
                    .replaceFirst("%d", "6");
        } else {
            System.err.println("invalid type: " + type);
            System.exit(1);
        }
        this.typeId = type.toLowerCase().replace("%d", "")
                .replace("(", "")
                .replace(")", "")
                .replaceAll(",[ ]*", "");
        if (this.columnName == null) {
            this.columnName = this.typeId
                    .replace(' ', '_')
                    .replace(',', '_')
                    .replace("(", "_")
                    .replace(")", "")
                    .replace("'", "");
        }
    }
}
