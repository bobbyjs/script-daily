package org.dreamcat.daily.script;

import java.io.IOException;
import java.util.List;
import org.dreamcat.common.io.IOUtil;
import org.dreamcat.common.text.argparse.ArgParser;
import org.dreamcat.common.util.ObjectUtil;
import org.dreamcat.common.x.mail.MailSender;

/**
 * Create by tuke on 2021/3/5
 */
public class App {

    static final String MAIL_HOST = System.getenv("MAIL_HOST");
    static final String MAIL_USER = System.getenv("MAIL_USER");
    static final String MAIL_PASSWORD = System.getenv("MAIL_PASSWORD");
    static final String USAGE;

    public static void main(String[] args) throws Exception {
        ArgParser argParser = ArgParser.newInstance();
        // basic usage
        argParser.addBool("help", "help");
        // connect
        argParser.add("host", "h", "host");
        argParser.add("user", "u", "user");
        argParser.add("password", "p", "password");
        // content
        argParser.add("from", "f", "from");
        argParser.addList("to", "t", "to");
        argParser.addList("cc", "cc");
        argParser.addList("bcc", "bcc");
        argParser.addList("replyTo", "replyTo");
        argParser.add("subject", "s", "subject");
        argParser.add("content", "c", "content");

        argParser.parse(args);

        boolean help = argParser.getBool("help");
        if (help) {
            System.out.println(USAGE);
            System.exit(0);
        }

        String host = argParser.get("host");
        String user = argParser.get("user");
        String password = argParser.get("password");
        if (ObjectUtil.isBlank(host)) host = MAIL_HOST;
        if (ObjectUtil.isBlank(user)) user = MAIL_USER;
        if (ObjectUtil.isBlank(password)) password = MAIL_PASSWORD;

        // validate connection parameters
        checkParameter(host, "host", "-h|--host", "MAIL_HOST");
        checkParameter(user, "user", "-u|--user", "MAIL_USER");
        checkParameter(password, "password", "-p|--password", "MAIL_PASSWORD");
        MailSender sender = new MailSender(host, user, password);

        String from = argParser.get("from");
        List<String> to = argParser.getList("to");
        List<String> cc = argParser.getList("cc");
        List<String> bcc = argParser.getList("bcc");
        List<String> replyTo = argParser.getList("replyTo");
        String subject = argParser.get("subject");
        String content = argParser.get("content");

        // validate content parameters
        checkParameter(to, "to", "-t|--to");
        if (ObjectUtil.isEmpty(subject)) subject = "";
        checkParameter(content, "content", "-c|--content");

        MailSender.Op op = sender.newOp()
                .from(from)
                .to(to)
                .subject(subject)
                .content(content);
        if (ObjectUtil.isNotEmpty(cc)) op.cc(cc);
        if (ObjectUtil.isNotEmpty(bcc)) op.bcc(bcc);
        if (ObjectUtil.isNotEmpty(replyTo)) op.replyTo(replyTo);
        op.send();
    }

    private static void checkParameter(Object value, String key, String names) {
        checkParameter(value, key, names, null);
    }

    private static void checkParameter(Object value, String key, String names, String env) {
        if (value instanceof List) {
            if (ObjectUtil.isNotEmpty((List<?>) value)) return;
        } else {
            if (ObjectUtil.isNotBlank((String) value)) return;
        }
        String envString = "";
        if (ObjectUtil.isNotBlank(env)) {
            envString = String.format(" or define the environment variable `%s`", env);
        }

        System.err.printf("required parameter `%s` is missing, "
                        + "pass it via %s%s",
                key, names, envString);
        System.exit(1);
    }

    static {
        try {
            USAGE = IOUtil.readAsString(App.class.getResourceAsStream("/usage.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
