import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create by tuke on 2018/08/19
 */
public class RenameDemo {

    public static void main(String[] args) {
        // windows os style
        rename("/Volumes/HD3/Bum/V", "/Volumes/HD3/Bum/S");
    }

    private static void rename(String videoDir, String subtitleDir) {
        rename(new File(videoDir), new File(subtitleDir));
    }

    private static void rename(File videoDir, File subtitleDir) {
        StringBuilder shell = new StringBuilder();
        System.out.printf("renaming %s to %s\n", videoDir.getAbsolutePath(),
                subtitleDir.getAbsolutePath());

        String[] vs = videoDir.list();
        if (vs == null) return;

        for (String v : vs) {
            Matcher mat = PAT.matcher(v);
            if (mat.find()) {
                String prefix = mat.group(1);

                String name;
                findAndRename:
                {
                    String[] ss = subtitleDir.list();
                    if (ss == null) continue;
                    for (String s : ss) {
                        String pat = String.format(".*?[第]%s話 「(.*?)」.*?", prefix);
                        Matcher m = Pattern.compile(pat).matcher(s);
                        if (m.find()) {
                            String title = m.group(1);
                            name = String.format("%s.%s", prefix, title);
                            System.out.printf("%s rename to %s.ssa\n", prefix, name);
                            shell.append("move 'S\\")
                                    .append(s)
                                    .append("' '")
                                    .append(name)
                                    .append(".ssa'\n");
                            //File(s).renameTo(File( "${name}.ssa" ) )
                            name += ".mkv";
                            break findAndRename;
                        }
                    }
                    name = "";
                }

                if (name.length() != 0) {
                    System.out.printf("%s rename to %s\n", v, name);
                    shell.append("move 'V\\")
                            .append(v)
                            .append("' '")
                            .append(name)
                            .append("'\n");
                    //File(v).renameTo(File( name ) )
                }
            }
        }
        System.out.printf("\n%s\n", shell.toString());
    }

    private static final Pattern PAT = Pattern.compile("([0-9]{3}).*?");

}


