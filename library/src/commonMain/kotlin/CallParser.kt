package moe.him188.assembly.interpreter

import moe.him188.assembly.interpreter.descriptor.Call
import moe.him188.assembly.interpreter.descriptor.Instruction
import moe.him188.assembly.interpreter.descriptor.Label
import moe.him188.assembly.interpreter.descriptor.Operand

fun String.parseAllCalls(): List<Call> {
    val parser = CallParser()
    return lines().mapIndexedNotNull { index, s ->
        parser.parse(s, index + 1)
    }
}

class CallParser {
    companion object {
        private val CALL_REGEX = Regex("""(?:(\w+):)?(?:\s+)?(\w+)?(?:\s+)?((?:#|#&|#B)?\w+)?""")
    }

    private fun throwSyntaxError(line: String, lineNumber: Int?, cause: Throwable? = null): Nothing {
        if (lineNumber == null) throw SyntaxErrorException("Syntax error: $line", cause)
        else throw SyntaxErrorException("Syntax error at line $lineNumber: $line", cause)
    }

    fun parse(line: String, lineNumber: Int?): Call? {
        if (line.isBlank()) return null

        val (labelString, instructionString, operandString) = CALL_REGEX.matchEntire(line)?.destructured
            ?: throwSyntaxError(line, lineNumber)

        return kotlin.runCatching {
            Call(
                labelString.takeIf(String::isNotBlank)?.let(::Label),
                if (instructionString.isBlank()) Instruction.LABEL else Instruction.valueOf(
                    instructionString
                ),
                Operand(operandString)
            )
        }.onFailure {
            throwSyntaxError(line, lineNumber, it)
        }.getOrThrow()
    }

}