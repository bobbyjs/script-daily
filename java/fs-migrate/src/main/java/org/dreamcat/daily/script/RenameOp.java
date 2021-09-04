package org.dreamcat.daily.script;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.common.text.argparse.ArgParser;

/**
 * Create by tuke on 2021/4/5
 */
public class RenameOp extends FileSchemaMigrateHandler {

    // regex like: ^(.+)\?.+$
    private String sourceRegex;
    // support regex like: $1
    private String replacementRegex;

    @Override
    protected void migrateSchema(Path sourceSchema, Path targetSchema) throws Exception {
        log.info("rename file {} to {}", sourceSchema, targetSchema);
        if (effect) {
            Files.move(sourceSchema, targetSchema);
        }
    }

    @Override
    protected Path getTargetSchema(Path sourceSchema) {
        String sourceName = FileUtil.basename(sourceSchema.toAbsolutePath().toString());
        if (!sourceName.matches(sourceRegex)) {
            log.warn("unmatched source pattern, skip it: {}", sourceSchema);
            return null;
        }

        String targetName = sourceName.replaceAll(sourceRegex, replacementRegex);
        return Paths.get(sourceSchema.toAbsolutePath().getParent().toString(), targetName);
    }

    @Override
    protected ArgParser defineArgs() {
        ArgParser argParser = super.defineArgs();
        argParser.add("sourceRegex", "sr", "sourceRegex");
        argParser.add("replacementRegex", "rr", "replacementRegex");
        return argParser;
    }

    @Override
    protected ArgParser parseArgs(String... args) {
        ArgParser argParser = super.parseArgs(args);
        this.sourceRegex = argParser.get("sourceRegex");
        this.replacementRegex = argParser.get("replacementRegex");

        if (sourceRegex == null || replacementRegex == null) {
            log.error("missing args: sourceRegex, replacementRegex, abort it");
            throw new IllegalArgumentException();
        }
        return argParser;
    }
}
