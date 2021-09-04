#!/usr/bin/env kotlinc -no-reflect -nowarn -script --

/// Create by tuke on 2020/4/23

import java.io.File
import java.util.*

fun replace_fileline(
        inFile: String, outFile: String,
        pattern: String, replacement: String,
        excludePattern: String?, verbose: Boolean) {
    File(inFile).reader().buffered().useLines { lines ->
        File(outFile).writer().buffered().use { w ->
            for (line in lines) {
                if (excludePattern != null &&
                        line.matches(excludePattern.toRegex())) continue

                val newLine = line.replace(pattern.toRegex(), replacement)
                if (verbose) {
                    println(newLine)
                }
                w.write(newLine)
                w.newLine()
            }
        }
    }
}

///

println("executing: replace_fileline ${Arrays.toString(args).trim('[', ']')}")

if (args.size !in 4..6) {
    println("Usage: replace_fileline [-v] infile outfile pattern replacement [excludePattern]")
    System.exit(1);
}

if (args.size == 4) {
    replace_fileline(args[0], args[1], args[2], args[3], null, false);
} else if (args.size == 5) {
    if (args[0] == "-v") {
        replace_fileline(args[1], args[2], args[3], args[4], null, true);
    } else {
        replace_fileline(args[0], args[1], args[2], args[3], args[4], false);
    }
} else {
    replace_fileline(args[1], args[2], args[3], args[4], args[5], true);
}
