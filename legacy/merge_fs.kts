#!/usr/bin/env kotlinc -no-reflect -nowarn -script --

/// Create by tuke on 2020/4/23

import java.io.File
import java.io.FileWriter
import java.util.*

fun merge_ts_help() {
    println("merge_ts  merge {prefix}{range}{suffix]} to {output}[.mkv]")
    println("\t\t exmaple, merge_ts -r 0..99 -p some -o awesome")
    println("\t\t to merge some000.ts, some001.ts, ..., some099.ts to awesome.mkv")
    println("\t -p --prefix, prefix name")
    println("\t -s --suffix, suffix name, default .ts")
    println("\t -r --range, like 0..100")
    println("\t -o --output, output name")
    println("\t -P --padding --no-padding, padding with 0, default no padding")
    println("\t -v --verbose, verbose debug")
    System.exit(1)
}

fun merge_ts(path: String, range: IntRange, prefix: String, suffix: String,
             output: String, padding: Boolean, verbose: Boolean) {
    val width = "${range.endInclusive}".length

    val filename = UUID.randomUUID().toString()
    val file = File(path, filename)

    val files = mutableListOf<String>()
    FileWriter(file, true).use {
        for (i in range) {
            var name = if (padding)
                String.format("%0${width}d", i)
            else
                i.toString()

            name = "$prefix$name$suffix"
            val line = "file $name\n"
            if (verbose) {
                files.add(name);
                print(line)
            }

            it.write(line)
        }
    }
    if (verbose) {
        println("finish wrote file list to ${file.absolutePath}")

        if (files.size < 100) {
            println("ffmpeg -f concat -i \"${files.joinToString("|")}\" -c copy -bsf:a aac_adtstoasc $output")
        }
    }

    if (true)return

    val cmd = "cd $path && ffmpeg -f concat -i $filename -c copy -bsf:a aac_adtstoasc $output"
    val proc = Runtime.getRuntime().exec(
            arrayOf("sh", "-c", cmd));
    proc.inputStream.use {
        it.bufferedReader().lines().forEach(::println)
    }
    proc.errorStream.use {
        it.bufferedReader().lines().forEach(::println)
    }

    if (!file.delete() && file.exists()) {
        println("failed to delete ${file.absolutePath}")
    }
}

///

println("executing: merge_ts ${Arrays.toString(args).trim('[', ']')}")

val shift = LinkedList(args.toList())

var path = "."
var prefix: String? = ""
var suffix: String? = ".ts"
var range: IntRange? = null
var output: String? = null
var padding = false
var verbose = false

loop@ while (true) {
    val arg = shift.pollFirst()
    if (arg == null) break

    when (arg) {
        "-p", "--prefix" -> {
            prefix = shift.pollFirst()
        }
        "-s", "--suffix" -> {
            suffix = shift.pollFirst()
        }
        "-o", "--output" -> {
            output = shift.pollFirst()
        }
        "-P", "--padding" -> {
            padding = true
        }
        "-r", "--range" -> {
            val v = shift.pollFirst()
            if (v == null) {
                println("the value of -r|--range is required")
                merge_ts_help()
            } else {
                val a = v.split("..")
                range = a[0].toInt()..a[1].toInt()
            }
        }
        "-v", "--verbose" -> {
            verbose = true
        }
        else -> {
            path = arg;
            break@loop
        }
    }
}

if (prefix == null || suffix == null || range == null) {
    merge_ts_help()
}

if (output == null) {
    if (prefix == "") {
        output = "output.mkv"
    } else {
        output = "$prefix.mkv"
    }
} else if (output!!.endsWith(".mkv")) {
    output = "$output.mkv"
}

merge_ts(path, range!!, prefix!!, suffix!!, output!!, padding, verbose)
