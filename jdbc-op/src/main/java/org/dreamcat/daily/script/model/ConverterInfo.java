package org.dreamcat.daily.script.model;

import java.util.Set;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2023-05-29
 */
@Data
public class ConverterInfo {

    private String template;
    private Set<String> types;
}
