#!/usr/bin/env kotlinc -no-reflect -nowarn -script --

/// Create by tuke on 2020/4/23


import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

/*
replace filename via regexp on dir tree

chmod +x replace_filename.kts
./replace_filename.kts
 */

class NormalFileVisitor(val callback: (File) -> Unit, val excludePatterns: Collection<String>? = null) : SimpleFileVisitor<Path>() {
    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
        if (excludePatterns != null && excludePatterns.isNotEmpty() &&
                excludePatterns.stream().anyMatch({
                    dir.toFile().name.matches(it.toRegex())
                })) {
            return FileVisitResult.SKIP_SUBTREE
        }
        return FileVisitResult.CONTINUE
    }

    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
        callback(file.toFile())
        return FileVisitResult.CONTINUE;
    }
}

fun replace_filename(path: String, oldPattern: String, newValue: String, excludePatterns: Collection<String>? = null) {
    Files.walkFileTree(File(path).toPath(), NormalFileVisitor({
        val oldName = it.name
        val newName = oldName.replace(oldPattern, newValue)
        if (oldName.equals(newName)) return@NormalFileVisitor

        val toFile = File(it.parentFile, newName)
        Files.move(it.toPath(), toFile.toPath())
        println("renaming ${it.name} to ${toFile.name}")
    }, excludePatterns))
}

println("executing 'replace_filename ${Arrays.toString(args).trim('[', ']')}'")
if (args.size < 3) {
    println("Usage: replace_filename.kts dir pattern replacement [exclude-patterns]")
    System.exit(1)
}
val excludePatterns = if (args.size > 3) args.slice(3 until args.size) else null
replace_filename(args[0], args[1], args[2], excludePatterns)
