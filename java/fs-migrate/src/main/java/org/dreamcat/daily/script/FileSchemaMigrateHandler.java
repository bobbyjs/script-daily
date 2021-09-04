package org.dreamcat.daily.script;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.dreamcat.common.text.argparse.ArgParser;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.daily.script.common.SchemaMigrateHandler;

/**
 * Create by tuke on 2021/4/5
 */
public abstract class FileSchemaMigrateHandler extends SchemaMigrateHandler<Path> {

    protected List<String> paths;
    protected boolean recurse;
    protected boolean includeFile;
    protected boolean includeDir;
    // private final

    @Override
    protected Iterable<Path> getSchemas() {
        if (ObjectUtil.isEmpty(paths)) {
            log.warn("empty path, abort it");
            return Collections.emptyList();
        }

        List<Path> files = new ArrayList<>();
        FileVisitor<Path> fileVisitor = this.getFileVisitor(files);

        for (String path : paths) {
            Path file = Paths.get(path);
            try {
                Files.walkFileTree(file, fileVisitor);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return files;
    }

    @Override
    protected boolean existsSchema(Path schema) {
        return Files.exists(schema);
    }

    @Override
    protected void deleteSchema(Path schema) {
        try {
            if (verbose) {
                log.warn("delete {} {}", getSchemaKeyword(), schema);
            }
            if (effect) {
                Files.delete(schema);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void createSchema(Path sourceSchema, Path schema) {
        // nop
    }

    @Override
    protected ArgParser defineArgs() {
        ArgParser argParser = super.defineArgs();
        argParser.addBool("recurse", "r", "recurse");
        argParser.addList("type", "t", "type");
        return argParser;
    }

    @Override
    protected ArgParser parseArgs(String... args) {
        ArgParser argParser = super.parseArgs(args);
        this.recurse = argParser.getBool("recurse");
        Set<String> type = new HashSet<>(argParser.getList("type"));
        this.includeFile = type.isEmpty() || type.contains("f") ||
                type.contains("file");
        this.includeDir = type.contains("d") || type.contains("dir") ||
                type.contains("directory");
        this.paths = argParser.getValues();
        return argParser;
    }

    private FileVisitor<Path> getFileVisitor(List<Path> files) {
        return new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (attrs.isRegularFile()) {
                    if (includeFile) {
                        files.add(file);
                    }
                } else if (attrs.isDirectory()) {
                    if (includeDir) {
                        files.add(file);
                    }
                    if (recurse) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        };
    }

}
