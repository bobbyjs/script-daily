package org.dreamcat.daily.script;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.dreamcat.common.argparse.ArgParserField;
import org.dreamcat.common.argparse.ArgParserType;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.daily.script.common.BaseHandler;
import org.dreamcat.daily.script.common.CliUtil;

/**
 * Create by tuke on 2021/4/5
 */
@Slf4j
@ArgParserType(allProperties = true, command = "rename")
public class RenameOp extends BaseHandler {

    @ArgParserField(position = 1)
    String sourcePath;
    @ArgParserField("t")
    Set<String> types = Collections.singleton("file");
    @ArgParserField("R")
    boolean recursive;
    @ArgParserField("sr")
    List<String> sourceRegex; // full match to the filename
    @ArgParserField("tc")
    String trimChar;
    @ArgParserField("ts")
    List<String> trimStr;
    @ArgParserField("tr")
    List<String> trChar; // translate a char to another char: --tr '&_' '#_' '$_'
    // --rr regex1 replacement1 regex2 replacement2 ..., regex like: ^(.+)\?.+$, replacement like: $1
    @ArgParserField("rr")
    List<String> replacementRegex;
    @ArgParserField(firstChar = true)
    boolean force;
    @ArgParserField({"y", "yes"})
    boolean effect;
    boolean abort;
    @ArgParserField("V")
    boolean verbose;

    transient boolean includeFile;
    transient boolean includeDir;

    @Override
    protected void afterPropertySet() throws Exception {
        CliUtil.checkParameter(sourcePath, "<sourcePath>");
        this.includeFile = types.contains("f") || types.contains("F") ||
                types.contains("file") || types.contains("FILE");
        this.includeDir = types.contains("d") || types.contains("D") ||
                types.contains("dir") || types.contains("DIR");
        if (ObjectUtil.isEmpty(replacementRegex) && ObjectUtil.isEmpty(trimChar)
                && ObjectUtil.isEmpty(trimStr) && ObjectUtil.isEmpty(trChar)) {
            System.err.println("required one of --tr-regex | --trim-char | --trim-str");
            System.exit(1);
        }
        if (ObjectUtil.isNotEmpty(trChar)) {
            for (String s : trChar) {
                if (s.length() != 2) {
                    System.err.printf("invalid value `%s` for --tr%n", s);
                    System.exit(1);
                }
            }
        }
    }

    @Override
    public void run() throws Exception {
        Path source = Paths.get(sourcePath).toAbsolutePath().normalize();
        handleChildren(source);
    }

    private void handleChildren(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) {
            Iterator<Path> iter = paths.iterator();
            while (iter.hasNext()) {
                Path path = iter.next();
                if (verbose) {
                    System.out.println("start to handle: " + path);
                }
                if (Files.isDirectory(path)) {
                    // handle children first
                    if (recursive) handleChildren(path);
                    // then handle itself
                    if (includeDir) handleSelf(path);
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

    private void handle0(Path path) throws IOException {
        String sourceName = path.toFile().getName();
        if (!isMatchSourceRegex(sourceName)) {
            log.warn("unmatched source pattern, skip it: {}", path);
            return;
        }
        String targetName = sourceName;
        if (ObjectUtil.isNotEmpty(trimChar)) {
            targetName = StringUtil.prune(targetName, trimChar);
        }
        if (ObjectUtil.isNotEmpty(trimStr)) {
            for (String s : trimStr) {
                targetName = targetName.replace(s, "");
            }
        }
        if (ObjectUtil.isNotEmpty(trChar)) {
            Map<Character, Character> trMap = new HashMap<>(trChar.size());
            for (String s : trChar) {
                trMap.put(s.charAt(0), s.charAt(1));
            }
            targetName = StringUtil.translate(targetName, trMap);
        }
        if (ObjectUtil.isNotEmpty(replacementRegex)) {
            for (int i = 0, n = replacementRegex.size(); i < n - 1; i+=2) {
                targetName = targetName.replaceAll(replacementRegex.get(i), replacementRegex.get(i + 1));
            }
        }
        if (Objects.equals(sourceName, targetName)) {
            System.out.printf("same name between source and target, name: %s, source: %s%n",
                    sourceName, path);
            return;
        }
        Path target = Paths.get(path.toAbsolutePath().getParent().toString(), targetName);
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

        System.out.printf("rename file %s to %s%n", path, target);
        if (effect) {
            Files.move(path, target);
        }
    }

    private boolean isMatchSourceRegex(String sourceName) {
        if (ObjectUtil.isEmpty(sourceRegex)) return true;
        for (String s : sourceRegex) {
            if (sourceName.matches(s)) return true;
        }
        return false;
    }
}
