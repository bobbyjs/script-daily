#!/usr/bin/env kotlinc -no-reflect -nowarn -script --

/// Create by tuke on 2020/11/8

import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.system.exitProcess

/*
source
    - kind
        - book
            - book_name.azw3
        - book_name.mobi

 */

val BOOK_EXTS = setOf("pdf", "txt", "epub", "mobi", "azw", "azw3")

fun copyEbook(sourceDir: File, targetDir: File, priorities: List<String>) {
    if (!sourceDir.exists() or !sourceDir.isDirectory) {
        System.err.println("sourceDir `${sourceDir.absolutePath}` doesn't exist")
        exitProcess(1)
    }
    if (!targetDir.exists() or !targetDir.isDirectory) {
        System.err.println("targetDir `${targetDir.absolutePath}` doesn't exist")
        exitProcess(1)
    }
    if (targetDir.absolutePath.startsWith(sourceDir.absolutePath) or
            sourceDir.absolutePath.startsWith(targetDir.absolutePath)) {
        System.err.println("sourceDir `${sourceDir.absolutePath}` and targetDir `${targetDir.absolutePath}` cannot be included by each other")
        exitProcess(1)
    }


    val kinds = sourceDir.listFiles() ?: return
    for (kind in kinds) {
        if (!kind.isDirectory) continue
        val targetKindDir = File(targetDir, kind.name)
        val books = kind.listFiles() ?: continue

        bookLoop@for (book in books) {
            val bookName = book.name;
            // copy to source/kind/book to target/kind/book
            if (book.isFile) {
                if (extName(bookName) !in BOOK_EXTS) continue

                if (!targetKindDir.exists() and !targetKindDir.mkdir()) {
                    System.err.println("failed to mkdir ${targetKindDir.absolutePath}, so skip ${book.absolutePath}")
                    continue
                }
                val targetKindBookFile = File(targetKindDir, book.name)
                println("copying ${book.absolutePath} to ${targetKindBookFile.absolutePath}")
                Files.copy(book.toPath(), targetKindBookFile.toPath())
            } else if (book.isDirectory) {
                val files = book.listFiles() ?: continue
                val fileMap = mutableMapOf<String, File>()
                for (file in files) {
                    if (!file.isFile) continue
                    val ext = extName(file.name)
                    if (ext !in BOOK_EXTS) continue
                    fileMap.put(ext, file)
                }
                if (fileMap.isEmpty()) continue

                for (priority in priorities) {
                    val file = fileMap[priority] ?: continue
                    // copy to source/kind/book/file to target/kind/file
                    if (!targetKindDir.exists() and !targetKindDir.mkdir()) {
                        System.err.println("failed to mkdir ${targetKindDir.absolutePath}, so skip ${book.absolutePath}")
                        continue
                    }
                    val targetKindBookFile = File(targetKindDir, file.name)
                    println("copying ${file.absolutePath} to ${targetKindBookFile.absolutePath}")
                    Files.copy(file.toPath(), targetKindBookFile.toPath())
                    continue@bookLoop
                }
            }
        }
    }
}

fun extName(filename: String): String {
    val i = filename.lastIndexOf('.')
    return filename.substring(i + 1)
}

///

println("executing: copy_ebook ${Arrays.toString(args).trim('[', ']')}")

val size = args.size
if (size < 3) {
    println("Usage: copy_ebook sourceDir targetDir priority1 [priority2 priority3 ...]")
    exitProcess(1);
}

copyEbook(File(args[0]), File(args[1]), args.asList().subList(2, size))
