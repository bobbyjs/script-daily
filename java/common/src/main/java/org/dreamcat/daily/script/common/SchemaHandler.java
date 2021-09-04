package org.dreamcat.daily.script.common;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.dreamcat.common.text.argparse.ArgParseException;
import org.dreamcat.common.text.argparse.ArgParser;
import org.dreamcat.common.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create by tuke on 2021/3/22
 */
public abstract class SchemaHandler<T> {

    protected boolean verbose;
    protected boolean force;
    protected boolean effect;

    private boolean exclude;
    private List<Pattern> patterns;
    private boolean abort; // abort by exception

    protected String getSchemaKeyword() {
        return DEFAULT_SCHEMA_KEYWORD;
    }

    protected String formatSchema(T schema) {
        return schema.toString();
    }

    protected abstract Iterable<T> getSchemas();

    protected abstract void handle(T schema) throws Exception;

    public void run(String... args) {
        this.parseArgs(args);
        String schemaKeyword = this.getSchemaKeyword();
        for (T schema : this.getSchemas()) {
            String schemaName = this.formatSchema(schema);
            if (!matchSchema(schema)) {
                if (verbose) log.info("skip unmatched {} {}", schemaKeyword, schemaName);
                continue;
            }
            try {
                this.handle(schema);
            } catch (Exception e) {
                log.error(String.format("handle %s %s, error occurred: %s",
                        schemaKeyword, schemaName, e.getMessage()), e);
                if (abort) {
                    log.info("operation is aborted by exception");
                    break;
                }
            }
        }
    }

    protected ArgParser defineArgs() {
        ArgParser argParser = ArgParser.newInstance();
        argParser.addList("include", "i", "include");
        argParser.addList("exclude", "e", "exclude");
        argParser.addBool("verbose", "v", "verbose");
        argParser.addBool("force", "f", "force");
        argParser.addBool("abort", "a", "abort");
        argParser.addBool("effect", "E", "effect");
        return argParser;
    }

    protected ArgParser parseArgs(String... args) {
        ArgParser argParser = this.defineArgs();
        try {
            argParser.parse(args);
        } catch (ArgParseException e) {
            log.error("fail to parse args: {}", e.toString());
            System.exit(1);
        }

        List<String> includeList = argParser.getList("include");
        List<String> excludeList = argParser.getList("exclude");
        this.verbose = argParser.getBool("verbose");
        this.force = argParser.getBool("force");
        this.abort = argParser.getBool("abort");
        this.effect = argParser.getBool("effect");

        List<String> list = Collections.emptyList();
        if (ObjectUtil.isNotEmpty(includeList)) {
            this.exclude = false;
            list = includeList;
        } else if (ObjectUtil.isNotEmpty(excludeList)) {
            this.exclude = true;
            list = excludeList;
        }
        this.patterns = list.stream().map(Pattern::compile).collect(Collectors.toList());
        return argParser;
    }

    private boolean matchSchema(T schema) {
        String schemaName = this.formatSchema(schema);
        if (ObjectUtil.isEmpty(patterns)) return true;
        for (Pattern pattern : patterns) {
            if (pattern.matcher(schemaName).find()) {
                // in matched case
                // if exclude, then return false to skip it
                return !exclude;
            }
        }
        // in unmatched case
        // if include, then return false to skip it
        return exclude;
    }

    private static final String DEFAULT_SCHEMA_KEYWORD = "schema";

    protected static final Logger log = LoggerFactory.getLogger(SchemaHandler.class);
}
