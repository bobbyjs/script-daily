package org.dreamcat.daily.script.module;

import static org.dreamcat.common.util.StringUtil.isNotEmpty;

import java.net.URL;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.sql.DriverUtil;
import org.dreamcat.daily.script.common.CliUtil;
import org.dreamcat.daily.script.model.DataSourceItem;

/**
 * @author Jerry Will
 * @version 2023-08-14
 */
@ArgParserType(allProperties = true)
public class JdbcModule {

    @ArgParserField(value = {"j"})
    String jdbcUrl;
    @ArgParserField(value = {"u"})
    String user;
    @ArgParserField(value = {"p"})
    String password;
    @ArgParserField(value = {"dc"})
    String driverClass;
    // driver directory
    @ArgParserField(value = {"dp"})
    List<String> driverPaths;
    @ArgParserField({"D"})
    Properties props;
    @ArgParserField({"ds"})
    String datasource;

    public void afterPropertySet() throws Exception {

    }

    private void fillByDataSourceItem(DataSourceItem item) {
        this.driverPaths = Arrays.asList(item.getDriverPath().split(","));
        this.driverClass = item.getDriverClass();
        this.jdbcUrl = item.getUrl();
        this.user = item.getUser();
        this.password = item.getPassword();
        this.props = new Properties();
        this.props.putAll(item.getProps());
    }

    public void run(IConsumer<Connection, ?> f) throws Exception {
        if (jdbcUrl != null) {
            validateJdbc();
        }
        // jdbcUrl == null only if yes is false
        if (jdbcUrl == null) {
            f.accept(null);
            return;
        }
        if (isNotEmpty(user)) props.put("user", user);
        if (isNotEmpty(password)) props.put("password", password);

        List<URL> urls = DriverUtil.parseJarPaths(driverPaths);
        for (URL url : urls) {
            System.out.println("add url to classloader: " + url);
        }
        DriverUtil.runIsolated(jdbcUrl, props, urls, driverClass, f);
    }

    public void validateJdbc() {
        // validate args
        CliUtil.checkParameter(jdbcUrl, "-j|--jdbc-url");
        CliUtil.checkParameter(driverPaths, "--dp|--driver-paths");
        CliUtil.checkParameter(driverClass, "--dc|--driver-class");
    }
}
