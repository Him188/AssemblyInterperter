package moe.him188.assembly.interpreter

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

data class ExecuteResult(
    val interpreter: AssemblyInterpreter,
    val output: String,
)

@OptIn(FlowPreview::class)
suspend fun executeAll(
    text: String,
    input: String = "",
    invocationCallback: InvocationCallback<InvokeAllAssemblyInterpreter> = InvocationCallback.noop()
): ExecuteResult {
    val output = Channel<Int>(Channel.BUFFERED)
    val interpreter = coroutineScope {
        InvokeAllAssemblyInterpreter(
            calls = text.parseAllCalls(),
            inputFlow = input.asSequence().asFlow().map { it.toInt() }.produceIn(this),
            outputChannel = output,
            invocationCallback
        ).apply {
            execute()
            output.close()
        }
    }
    val out = output.receiveAsFlow().toList().joinToString("") { it.toChar().toString() }
    return ExecuteResult(interpreter, out)
}