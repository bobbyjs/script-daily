package org.dreamcat.daily.script.poetry;

import static org.dreamcat.daily.script.App.USAGE;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.dreamcat.common.argparse.ArgParseException;
import org.dreamcat.common.argparse.ArgParser;
import org.dreamcat.common.util.StringUtil;
import org.dreamcat.common.util.SystemUtil;
import org.dreamcat.common.x.json.JsonUtil;

/**
 * @author Jerry Will
 * @version 2022-05-19
 */
@SuppressWarnings("all")
public class ChinesePoetryFinder implements Predicate<String[]> {

    private static final String poetry_home = SystemUtil.getEnv("POETRY_HOME",
            System.getenv("HOME") + "/source/chinese-poetry");

    Set<String> authors;
    Set<String> titles;
    List<String> contents;

    @Override
    public boolean test(String... args) {
        ArgParser argParser;
        try {
            argParser = parseArgs(args);
        } catch (Exception e) {
            System.err.println("arg parser error: " + e.getMessage());
            return false;
        }

        boolean help = argParser.getBool("help");
        if (help) {
            System.out.println(USAGE);
            System.exit(0);
            return true;
        }

        this.authors = new HashSet<>(argParser.getList("author"));
        this.titles = new HashSet<>(argParser.getList("title"));
        this.contents = argParser.getValues();

        ci();
        quan_tang_shi();
        return true;
    }

    private void ci() {
        File dir = new File(poetry_home, "ci");
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.getName().startsWith("ci.song")) continue;
            search(file, Ci.class, this::output);
        }
    }

    private void quan_tang_shi() {
        File dir = new File(poetry_home, "quan_tang_shi/json");
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!file.getName().endsWith(".json")) continue;
            search(file, Shi.class, this::output);
        }
    }

    private <T extends Poetry> void search(File file, Class<T> clazz, Consumer<T> output) {
        search(file, clazz, a -> true, output);
    }

    private <T extends Poetry> void search(File file, Class<T> clazz,
            Predicate<T> filter, Consumer<T> output) {
        List<T> list = JsonUtil.fromJsonArray(file, clazz);
        if (list == null) return;
        for (T poetry : list) {
            String author = poetry.getAuthor();
            String title = poetry.getTitle();
            List<String> paragraphs = poetry.getParagraphs();
            if (author == null || (!authors.isEmpty() && !authors.contains(author))) continue;
            if (title != null && !titles.isEmpty() && !titles.contains(title)) continue;
            if (paragraphs == null || !matchContent(paragraphs)) continue;
            if (filter.test(poetry)) output.accept(poetry);
        }
    }

    private void output(Poetry poetry) {
        System.out.printf("%s\n%s\t%s\n%s\n\n",
                StringUtil.repeat(" ---- ", 6), poetry.getAuthor(), poetry.getTitle(),
                String.join("\n", poetry.getParagraphs()));
    }

    private boolean matchContent(List<String> paragraphs) {
        for (String content : contents) {
            boolean unmatch = true;
            for (String paragraph : paragraphs) {
                if (paragraph.contains(content)) {
                    unmatch = false;
                    break;
                }
            }
            if (unmatch) return false;
        }
        return true;
    }

    private static ArgParser parseArgs(String... args) throws ArgParseException {
        ArgParser argParser = new ArgParser();
        argParser.addBool("help", "help");
        argParser.add("author", "a", "author");
        argParser.add("title", "t", "title");
        argParser.parse(args);
        return argParser;
    }
}
