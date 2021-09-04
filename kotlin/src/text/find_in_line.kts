#!/usr/bin/env kotlinc -no-reflect -nowarn -script --

import java.util.*
import java.io.*

/*
find lines which are contained in a bigfile that you specified

chmod +x find_in_line.kts
./find_in_line.kts
 */

fun find_in_line(superFilename: String, sourceFilename: String, targetFilename: String) {
    val superSet: Set<String> = java.io.File(superFilename).readLines()
        .map { it.trim() }
        .toSet();
    File(targetFilename).bufferedWriter().use {
        File(sourceFilename).forEachLine { line ->
            val record = line.trim();
            if (superSet.contains(record)) {
                it.write(record)
                it.newLine()
            }
        }
    }
}

println("executing 'find_in_line ${Arrays.toString(args).trim('[', ']')}'")
if (args.size != 3) {
    println("Usage: find_in_line.kts big-file source-file target-file")
    System.exit(1)
}
find_in_line(args[0], args[1], args[2])
