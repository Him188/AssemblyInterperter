package moe.him188.assembly.interpreter

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

suspend fun main() {
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

@OptIn(FlowPreview::class)
suspend fun executeAll(
    text: String,
    input: String = ""
): String {
    val output = Channel<Int>(Channel.BUFFERED)
    coroutineScope {
        InvokeAllAssemblyInterpreter(
            calls = text.parseAllCalls(),
            inputFlow = input.asSequence().asFlow().map { it.toInt() }.produceIn(this),
            outputChannel = output
        ).run {
            execute()
            output.close()
        }
    }
    return output.receiveAsFlow().toList().joinToString("") { it.toChar().toString() }
}