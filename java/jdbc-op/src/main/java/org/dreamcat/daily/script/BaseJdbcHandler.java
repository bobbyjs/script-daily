package org.dreamcat.daily.script;

import static org.dreamcat.common.util.StringUtil.isNotEmpty;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.io.UrlUtil;
import org.dreamcat.common.javac.FileClassLoader;
import org.dreamcat.common.util.ArrayUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
public class BaseJdbcHandler {

    @ArgParserField(value = {"j"})
    String jdbcUrl;
    @ArgParserField(value = {"u"})
    String user;
    @ArgParserField(value = {"w"})
    String password;
    @ArgParserField(value = {"dc"})
    String driverClass;
    // driver directory
    @ArgParserField(value = {"dp"})
    String driverPath;
    @ArgParserField({"D"})
    Properties props;
    @ArgParserField(value = {"y"})
    boolean yes; // execute sql or not actually

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(IConsumer<Connection, ?> f) throws Exception {
        if (!yes) {
            f.accept(null);
            return;
        }
        // validate args
        CliUtil.checkParameter(jdbcUrl, "jdbcUrl", "-j|--jdbc-url");
        CliUtil.checkParameter(driverPath, "driverPath", "--dp|--driver-path");
        CliUtil.checkParameter(driverClass, "driverClass", "--dc|--driver-class");
        if (isNotEmpty(user) && isNotEmpty(password)) {
            props.put("user", user);
            props.put("password", password);
        }

        File driverDir = new File(driverPath);
        if (!driverDir.exists() || !driverDir.isDirectory()) {
            System.err.println("driver path doesn't exist or not a directory: " + driverPath);
            System.exit(1);
        }

        URL[] urls = ArrayUtil.map(driverDir.listFiles(),
                File::toURI, UrlUtil::toURL, URL[]::new);
        FileClassLoader driverClassLoader = new FileClassLoader(urls);
        Class<? extends Driver> driverClass = (Class) driverClassLoader.loadClass(this.driverClass);

        Driver driver = ReflectUtil.newInstance(driverClass);
        try (Connection connection = driver.connect(jdbcUrl, props)) {
            f.accept(connection);
        }
    }
}