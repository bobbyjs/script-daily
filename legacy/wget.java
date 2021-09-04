import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * Created by tuke on 2017/3/30.
 * javac wget.java
 * java wget url-1 url-2 ... url-n
 * Download some files for urls
 */

public class wget {

    private final static String DEFAULT_PATH = "./download/"; // download path

    private static boolean okay; // 验证是否下载成功


    private static String filename;
    private static int contentLength;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java wget some_urls");
            return;
        }
        System.out.println("Work in " + Paths.get(DEFAULT_PATH).toAbsolutePath());
        System.out.println("On " + new Date());

        long t;
        URLConnection connection;
        for (String arg : args) {
            System.out.println();
            System.out.println("Downloading from " + arg);

            try {
                okay = false;
                connection = new URL(arg).openConnection();

                String after_quote = arg.substring(arg.lastIndexOf('/') + 1);
                if (Pattern.compile("[?]").matcher(after_quote).find()) {
                    filename = arg.substring(arg.lastIndexOf('/') + 1, arg.lastIndexOf('?'));
                } else {
                    filename = arg.substring(arg.lastIndexOf('/') + 1);
                }

                contentLength = connection.getContentLength();

                if (contentLength == -1) {
                    t = System.currentTimeMillis();
                    handleBinaryWithoutLength(connection);
                    t = System.currentTimeMillis() - t;
                    System.out.println("Time spent " + t / 1000. + "s.");
                } else {
                    t = System.currentTimeMillis();
                    handleBinary(connection);
                    t = System.currentTimeMillis() - t;
                    System.out.println("Time spent " + t / 1000. + "s.");
                }
            } catch (IOException e) {
                System.out.println("Fail to connect it");
                e.printStackTrace();
            }
        }
    }

    public static void handleBinary(URLConnection connection) {
        try (BufferedInputStream in = new BufferedInputStream(
                connection.getInputStream())) {
            byte[] data = new byte[contentLength];

            int offset = 0;
            int bytesRead = -1;
            while (offset < contentLength) {
                // 每次尝试读取全部字节，保存每次真实读取的字节数至bytesRead
                //然后下次再读取剩余的全部字节
                bytesRead = in.read(data, offset, data.length - offset);
                if (bytesRead == -1) break;
                offset += bytesRead;
            }
            //如果没有成功读取全部字节，则放弃保存此文件
            if (offset != contentLength) {
                System.out.println("Only read " + offset + " bytes; Expected "
                        + contentLength + " bytes for " + filename);
                return;
            }

            try (FileOutputStream out = new FileOutputStream(DEFAULT_PATH + filename)) {
                out.write(data);
                out.flush();
                okay = true;
            }

        } catch (IOException e) {
            System.out.println("Fail to get binary file:  " + filename);
            e.printStackTrace();
        }

        if (okay) {
            System.out.println("Succeed in getting binary file:  " + filename);
        }
    }

    public static void handleBinaryWithoutLength(URLConnection connection) {
        try (BufferedInputStream in = new BufferedInputStream(
                connection.getInputStream())) {

            try (FileOutputStream out = new FileOutputStream(DEFAULT_PATH + filename)) {
                byte[] data = new byte[1024];
                int bytesRead = in.read(data);

                while (bytesRead != -1) {
                    out.write(data, 0, bytesRead);
                    bytesRead = in.read(data);
                }
                out.flush();
                okay = true;
            }
        } catch (IOException e) {
            System.out.println("Fail to get binary file:  " + filename);
            e.printStackTrace();
        }

        if (okay) {
            System.out.println("Succeed in getting binary file:  " + filename);
        }
    }

}
