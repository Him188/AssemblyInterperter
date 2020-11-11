@file:Suppress("BlockingMethodInNonBlockingContext")

package moe.him188.assembly.interpreter

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.InputStream
import java.io.OutputStream

@OptIn(FlowPreview::class)
suspend fun main() {
    println()

    val callParser = CallParser()
    var lineNumber = 0

    supervisorScope {
        val coroutineScope = this

        lateinit var executor: ReplAssemblyInterpreter

        val executeChannel = Channel<String>()
        launch {
            executeChannel.receiveAsFlow().collect { command ->
                val call = callParser.parse(command, lineNumber++) ?: return@collect
                this@supervisorScope.launch {
                    executor.execute(call)
                }
            }
        }

        executor = ReplAssemblyInterpreter(
            SmartLineReader(coroutineScope, executeChannel).run {
                //flow<String> { readLine() }.map { it.takeSingle() }.produceIn(coroutineScope)
                Channel()
            },
            System.`out`.asOutputChannel(coroutineScope)
        )
    }
}

private fun String.takeSingle(): Int {
    return singleOrNull()?.toInt() ?: error("Only single character is accepted.")
}

@OptIn(ExperimentalCoroutinesApi::class)
class SmartLineReader(
    coroutineScope: CoroutineScope,
    private val fallbackChannel: SendChannel<String>,
) {
    private val temp = Channel<String>()

    init {
        coroutineScope.launch {
            while (isActive) {
                val value = temp.receiveOrNull() ?: return@launch
                if (!temp.offer(value)) {
                    fallbackChannel.send(value)
                }
            }
        }
        coroutineScope.launch {
            while (isActive) {
                withContext(Dispatchers.IO) {
                    print("ASSEMBLY > ")
                    temp.send(kotlin.io.readLine() ?: return@withContext)
                }
            }
        }
    }

    suspend fun readLine(): String = temp.receive()
}

@OptIn(ExperimentalCoroutinesApi::class)
fun OutputStream.asOutputChannel(coroutineScope: CoroutineScope): SendChannel<Int> {
    val channel = Channel<Int>()

    coroutineScope.launch {
        while (true) {
            val char = channel.receiveOrNull() ?: return@launch
            write(char)
        }
    }

    return channel
}

fun InputStream.asInputChannel(coroutineScope: CoroutineScope): ReceiveChannel<Int> {
    val channel = Channel<Int>()
    val buffered = this.bufferedReader()

    coroutineScope.launch {
        val line = withContext(Dispatchers.IO) { buffered.readLine() } ?: return@launch
        channel.sendBlocking(line.singleOrNull()?.toInt() ?: error("Only single character is accepted."))
    }
    return channel
}