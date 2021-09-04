import java.util.*;
import javax.swing.*;
// import javafx.application.*;
import javax.script.*;

/**
 * Create by tuke on 2019-05-07
 * <p>
 * javac ShowEnv.java
 * java ShowEnv
 */
public class ShowEnv {

    public static void main(String[] args) {
        System.out.println("System.getProperties()");
        System.getProperties().forEach((k, v) -> {
            System.out.printf("\t%s:\t%s\n", k, v);
        });
        System.out.println();

        System.out.println("System.getenv()");
        System.getenv().forEach((k, v) -> {
            System.out.printf("\t%s:\t%s\n", k, v);
        });
        System.out.println();

        System.out.println("javax.script (Script Engines)");
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();
        for (ScriptEngineFactory factory : factories) {
            System.out.printf("%s\t%s\t%s\n",
                    factory.getEngineName(),
                    factory.getEngineVersion(),
                    factory.getNames());
        }
        if (factories.isEmpty()) {
            System.out.println("\tNo Script Engines found");
        }
        System.out.println();
    }
}

