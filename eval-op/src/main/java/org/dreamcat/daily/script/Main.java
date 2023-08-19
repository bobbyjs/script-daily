package org.dreamcat.daily.script;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.dreamcat.common.TimeDuration;
import org.dreamcat.common.util.ClassPathUtil;
import org.dreamcat.common.util.DateUtil;

/**
 * @author Jerry Will
 * @version 2023-03-25
 */
public class Main {

    public static final String USAGE;

    public static void main(String[] args) {
        int length = args.length;
        if (length < 1) {
            System.err.println(USAGE);
            System.exit(1);
        }

        String formula = args[0];
        if (formula.equals("-h") || formula.equals("--help")) {
            System.err.println(USAGE);
            System.exit(0);
        }

        // simple impl
        switch (formula) {
            case "now":
            case "now()":
                System.out.println(DateUtil.format(new Date(), "yyyy-MM-dd hh:mm:ss.SSS"));
                return;
            case "timestamp":
            case "timestamp()":
                System.out.println(System.currentTimeMillis());
            case "unix_timestamp":
            case "unix_timestamp()":
                System.out.println(System.currentTimeMillis() / 1000);
                return;
        }

        Matcher dateAdd = Pattern.compile("dateAdd\\('([\\d-]+)', '([a-z0-9-]+)'\\)")
                .matcher(formula);
        Matcher dateDiff = Pattern.compile("dateDiff\\('([\\d-]+)', '([\\d-]+)d?'\\)")
                .matcher(formula);
        if (dateAdd.matches()) {
            String date = dateAdd.group(1);
            TimeDuration duration = TimeDuration.parse(dateAdd.group(2));
            System.out.println(DateUtil.format(duration.addTo(DateUtil.parseDate(date))));
        } else if (dateDiff.matches()) {
            String date1 = dateDiff.group(1);
            String date2 = dateDiff.group(2);
            long diff = DateUtil.parseDate(date1).getTime() - DateUtil.parseDate(date2).getTime();
            System.out.println(TimeDuration.ofMillis(diff));
        } else {
            System.err.printf("unsupported formula: %s%n", formula);
            System.err.println(USAGE);
            System.exit(1);
        }
    }

    static {
        try {
            USAGE = ClassPathUtil.getResourceAsString("usage.txt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
