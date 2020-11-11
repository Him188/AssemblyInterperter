package moe.him188.assembly.interpreter

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
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

object ReplMain {
    fun main(args: Array<String>) {
        runBlocking {
            val channel = Channel<Int>()
            launch {
                channel.receiveAsFlow().collect { print(it.toChar()) }
            }
            runRepl(channel)
            channel.close()
        }
    }
}