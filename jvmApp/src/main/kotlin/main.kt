package moe.him188.assembly.interpreter

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val filename = args.getOrNull(0)
        if (filename == null) {
            runInReplMode()
        } else {
            runInAppMode(filename)
        }
    }
}

fun runInReplMode() {
    runBlocking {
        val channel = System.out.asOutputChannel(this)
        runRepl(channel)
        channel.close()
    }
}

fun runInAppMode(filename: String) {
    val callback = RecorderInvocationCallback<InvokeAllAssemblyInterpreter>()
    runBlocking {
        executeAll(
            File(filename).readText(),
            invocationCallback = callback,
        ).also {
            println(it)
        }
    }
}

private val txt = """
            CONST1: #1
            CONST2: #66
            
                    LDM CONST1
                    STO 3
                    
                    LDM #2
                    INC IX
                    INC IX
                    DEC IX
                    
                    LDX 2
                    ADD #65
            LABEL3: CMP CONST2
                    JPN LABEL2
            LABEL1: ADD #1
                    OUT
                    JMP LABEL3
            LABEL2: END
        """.trimIndent()

@OptIn(ExperimentalCoroutinesApi::class)
fun OutputStream.asOutputChannel(coroutineScope: CoroutineScope): SendChannel<Int> {
    val channel = Channel<Int>()

    coroutineScope.launch {
        while (true) {
            val char = channel.receiveOrNull() ?: return@launch
            @Suppress("BlockingMethodInNonBlockingContext")
            write(char)
        }
    }

    return channel
}

fun InputStream.asInputChannel(coroutineScope: CoroutineScope): ReceiveChannel<Int> {
    val channel = Channel<Int>()
    val buffered = this.bufferedReader()

    coroutineScope.launch {
        @Suppress("BlockingMethodInNonBlockingContext")
        val line = withContext(Dispatchers.IO) { buffered.readLine() } ?: return@launch
        channel.sendBlocking(line.singleOrNull()?.toInt() ?: error("Only single character is accepted."))
    }
    return channel
}