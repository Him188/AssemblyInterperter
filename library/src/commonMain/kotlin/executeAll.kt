package moe.him188.assembly.interpreter

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

@OptIn(FlowPreview::class)
suspend fun executeAll(
    text: String,
    input: String = "",
    invocationCallback: InvocationCallback<InvokeAllAssemblyInterpreter> = InvocationCallback.noop()
): String {
    val output = Channel<Int>(Channel.BUFFERED)
    coroutineScope {
        InvokeAllAssemblyInterpreter(
            calls = text.parseAllCalls(),
            inputFlow = input.asSequence().asFlow().map { it.toInt() }.produceIn(this),
            outputChannel = output,
            invocationCallback
        ).run {
            execute()
            output.close()
        }
    }
    return output.receiveAsFlow().toList().joinToString("") { it.toChar().toString() }
}