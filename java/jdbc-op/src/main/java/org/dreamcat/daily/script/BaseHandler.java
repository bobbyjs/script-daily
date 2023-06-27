package org.dreamcat.daily.script;

import static org.dreamcat.common.util.StringUtil.isNotEmpty;

import com.fasterxml.jackson.core.type.TypeReference;
import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.function.IConsumer;
import org.dreamcat.common.io.UrlUtil;
import org.dreamcat.common.javac.FileClassLoader;
import org.dreamcat.common.json.YamlUtil;
import org.dreamcat.common.sql.SqlValueRandomGenerator;
import org.dreamcat.common.text.InterpolationUtil;
import org.dreamcat.common.util.ArrayUtil;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.MapUtil;
import org.dreamcat.common.util.ReflectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.CliUtil;
import org.dreamcat.daily.script.model.ConverterInfo;

/**
 * @author Jerry Will
 * @version 2023-03-30
 */
@Setter
@Accessors(fluent = true)
public class BaseHandler {

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
    String driverPath;
    @ArgParserField({"D"})
    Properties props;
    @ArgParserField(value = {"y"})
    boolean yes; // execute sql or not actually
    @ArgParserField(firstChar = true)
    boolean help;
    boolean debug;

    @ArgParserField("S")
    String dataSourceType;
    String converterFile;
    // binary:cast($value as $type)
    Set<String> converters;
    boolean enableNeg; // generate neg number for number types
    double nullRatio = 0;

    transient final SqlValueRandomGenerator gen = new SqlValueRandomGenerator()
            .maxBitLength(1)
            .addEnumAlias("enum8") // clickhouse
            .addEnumAlias("enum16");

    protected void afterPropertySet() throws Exception {
        gen.enableNeg(enableNeg)
                .globalConvertor(this::convert);

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
    }

    protected String convert(String literal, String typeName) {
        if (nullRatio < 1 && nullRatio > 0) {
            if (Math.random() <= nullRatio) {
                return gen.nullLiteral();
            }
        }
        return null;
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(IConsumer<Connection, ?> f) throws Exception {
        if (yes || jdbcUrl != null) {
            validateJdbc();
        }
        // jdbcUrl == null only if yes is false
        if (jdbcUrl == null) {
            f.accept(null);
            return;
        }

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

    protected void validateJdbc() {
        // validate args
        CliUtil.checkParameter(jdbcUrl, "jdbcUrl", "-j|--jdbc-url");
        CliUtil.checkParameter(driverPath, "driverPath", "--dp|--driver-path");
        CliUtil.checkParameter(driverClass, "driverClass", "--dc|--driver-class");
    }
}