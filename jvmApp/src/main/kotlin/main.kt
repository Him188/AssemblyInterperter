package moe.him188.assembly.interpreter

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream


object ExecuteAllMain {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            executeAll(
                """
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
            ).also {
                println(it)
            }
        }
    }
}

object ReplMain {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            runRepl(System.out.asOutputChannel(this))
        }
    }
}

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