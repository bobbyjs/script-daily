package org.dreamcat.daily.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
    protected boolean excludeFile;
    protected boolean includeDir;
    private boolean abort;
    // regex like: ^(.+)\?.+$
    @ArgParserField({"s", "st"})
    private String sourceRegex;
    // support regex like: $1
    @ArgParserField({"r", "rr"})
    private String replacementRegex;
    @ArgParserField(firstChar = true)
    private boolean force;
    @ArgParserField({"y", "yes"})
    private boolean effect;
    @ArgParserField("V")
    private boolean verbose;

    @Override
    protected void afterPropertySet() throws Exception {
        CliUtil.checkParameter(sourcePath, "<sourcePath>");
    }

    @Override
    public void run() throws Exception {
        Path path = Paths.get(sourcePath);
        List<Path> files = FileUtil.getFileTree(path, !excludeFile, includeDir);

        for (Path file : files) {
            try {
                handle(file);
            } catch (Exception e) {
                System.err.printf("handle file %s, error occurred: %s%n", file, e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
                if (abort) {
                    System.out.println("operation is aborted by exception");
                    break;
                }
            }
        }
    }

    private void handle(Path source) throws IOException {
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

        if (!effect) {
            System.out.printf("rename file %s to %s%n", source, target);
            return;
        }
        System.out.printf("rename file %s to %s%n", source, target);
        if (effect) {
            Files.move(source, target);
        }
    }
}
