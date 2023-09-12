package org.dreamcat.daily.script.model;

import java.util.Map;
import lombok.Data;

/**
 * @author Jerry Will
 * @version 2023-09-13
 */
@Data
public class DataSourceItem {

    private String type;
    private String driverPath; // comma sep
    private String driverClass;
    private String url;
    private String user;
    private String password;
    private Map<String, Object> props;
}
