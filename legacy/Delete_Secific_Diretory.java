import java.io.File;
import java.nio.file.Files;

public class Delete_Secific_Diretory {

    static int _empty = 0;
    static int _count = 0;

    public static void main(String[] args) throws Exception {
        String opt, path;
        if (args.length >= 2) {
            opt = args[0];
            path = args[1];
            choose_secific(opt, new File(path));
        } else print_help();
    }

    public static void choose_secific(String opt, File dir) throws Exception {
        if (opt.contains("-e")) {
            del_empty_dir(dir);
            return;
        } else if (opt.contains("-c")) {
            int count = 0;
            if (opt.startsWith("--count=")) {
                count = Integer.parseInt(opt.replace("--count=", ""));
            } else if (opt.startsWith("-c")) {
                count = Integer.parseInt(opt.replace("-c", ""));
            }
            if (count > 0) {
                del_count_dir(dir, count);
                return;
            }

        }/*else if ( opt.contains("") ){

        }*/

        print_help();
    }

    public static void print_help() {
        System.out.println("java -cp . Delete_Secific_Diretory <opt> path\n");
        System.out.println("opt:");
        System.out.println("    --help  | -h    帮助");
        System.out.println("    --count | -c    子文件少于指定数");
        System.out.println("            --count=<no> or -c<no>");
        System.out.println("    --empty | -e    空条目");
/*        System.out.println("    -- | -    ");
        System.out.println("    -- | -    ");*/
    }

    public static void del_count_dir(File dir, int count) throws Exception {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File d : files) {
                if (d.isDirectory()) {
                    if (has_not_child_dir(d)) {
                        File[] fs = d.listFiles();
                        if (fs != null && fs.length <= count) {
                            Files.delete(d.toPath());
                            _count++;
                            System.out.println(d + " 已删除");
                        }
                    } else {
                        del_count_dir(d, count);
                    }
                }
            }
        }
        System.out.println("共删除 " + _count + " 个目录");
    }

    public static void del_empty_dir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File d : files) {
            if (!d.isDirectory()) continue;
            if (d.delete()) {
                _empty++;
                System.out.println(d + " 已删除");
            } else {
                del_empty_dir(d);
            }
        }
        System.out.println("共删除 " + _empty + " 个空目录");
    }

    private static boolean has_not_child_dir(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return true;
        for (File d : files) {
            if (d.isDirectory()) return false;
        }
        return true;
    }

}
