@file:Suppress("BlockingMethodInNonBlockingContext")

package moe.him188.assembly.interpreter

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

suspend fun runRepl(
    standardOutput: SendChannel<Int>
) {
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
                @OptIn(FlowPreview::class)
                flow<String> { readLine() }.map { it.takeSingle() }.produceIn(coroutineScope)
            },
            standardOutput
        )
    }
}

private fun String.takeSingle(): Int {
    return singleOrNull()?.toInt() ?: error("Only single character is accepted.")
}

internal expect fun standardReadLine(): String?

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
                print("ASSEMBLY> ")
                temp.send(standardReadLine() ?: continue)
            }
        }
    }

    suspend fun readLine(): String = temp.receive()
}