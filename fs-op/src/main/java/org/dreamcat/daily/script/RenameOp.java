package org.dreamcat.daily.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.io.FileUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * Create by tuke on 2021/4/5
 */
@Slf4j
@ArgParserType(allProperties = true, command = "rename")
public class RenameOp extends BaseHandler {

    @ArgParserField(position = 1)
    private String sourcePath;
    @ArgParserField("t")
    private Set<String> types = Collections.singleton("file");
    @ArgParserField("R")
    private boolean recursive;
    // regex like: ^(.+)\?.+$
    @ArgParserField({"s", "sr"})
    private String sourceRegex;
    // support regex like: $1
    @ArgParserField({"r", "rr"})
    private String replacementRegex;
    @ArgParserField(firstChar = true)
    private boolean force;
    @ArgParserField({"y", "yes"})
    private boolean effect;
    private boolean abort;
    @ArgParserField("V")
    private boolean verbose;

    protected transient boolean includeFile;
    protected transient boolean includeDir;

    @Override
    protected void afterPropertySet() throws Exception {
        CliUtil.checkParameter(sourcePath, "<sourcePath>");
        this.includeFile = types.contains("f") || types.contains("F") ||
                types.contains("file") || types.contains("FILE");
        this.includeDir = types.contains("d") || types.contains("D") ||
                types.contains("dir") || types.contains("DIR");
    }

    @Override
    public void run() throws Exception {
        Path source = Paths.get(sourcePath);
        handleChildren(source);
    }

    private void handleChildren(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) {
            Iterator<Path> iter = paths.iterator();
            while (iter.hasNext()) {
                Path path = iter.next();
                if (Files.isDirectory(path)) {
                    // handle children first
                    if (recursive) handleChildren(dir);
                    // then handle itself
                    if (includeDir) handleSelf(dir);
                } else if (Files.isRegularFile(path)) {
                    if (includeFile) handleSelf(path);
                }
            }
        } catch (IOException e) {
            if (abort) throw e;
            System.err.printf("handle dir %s, error occurred: %s%n", dir, e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    private void handleSelf(Path path) throws IOException {
        try {
            handle0(path);
        } catch (Exception e) {
            if (abort) throw e;
            System.err.printf("handle file %s, error occurred: %s%n", path, e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
        }
    }

    private void handle0(Path source) throws IOException {
        String sourceName = FileUtil.basename(source.toAbsolutePath().toString());
        if (!sourceName.matches(sourceRegex)) {
            log.warn("unmatched source pattern, skip it: {}", source);
            return;
        }

        String targetName = sourceName.replaceAll(sourceRegex, replacementRegex);
        Path target = Paths.get(source.toAbsolutePath().getParent().toString(), targetName);
        if (Files.exists(target)) {
            if (force) {
                // force delete existing schemas
                System.out.printf("delete target file %s%n", target);
                if (effect) {
                    Files.delete(target);
                }
            } else {
                System.out.printf("file %s already exists in the target%n", target);
                return;
            }
        }

        System.out.printf("rename file %s to %s%n", source, target);
        if (effect) {
            Files.move(source, target);
        }
    }
}
