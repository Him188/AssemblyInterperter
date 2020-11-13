package moe.him188.assembly.interpreter

import kotlinx.cinterop.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.stat

@OptIn(ExperimentalUnsignedTypes::class)
fun readFile(filename: String): String? {
    val fp = fopen(filename, "r") ?: return null
    memScoped {
        val fileStat = alloc<stat>()
        stat(filename, fileStat.ptr)
        val size = fileStat.st_size + 1
        val bytes = allocArray<ByteVar>(size)
        fread(bytes, 1, size.toULong(), fp)
        val text = bytes.toKString()
        fclose(fp)
        return text
    }
}

fun main(args: Array<String>) {
    val filename = args.getOrNull(0)
    if (filename == null) {
        runInReplMode()
    } else {
        runInAppMode(filename)
    }
}

fun runInReplMode() {
    runBlocking {
        val channel = Channel<Int>()
        launch {
            channel.receiveAsFlow().collect { print(it.toChar()) }
        }
        runRepl(channel)
        channel.close()
    }
}

fun runInAppMode(filename: String) {
    runBlocking { commonRunInAppMode(readFile(filename) ?: run { println("无法读取文件 $filename"); return@runBlocking }) }
}