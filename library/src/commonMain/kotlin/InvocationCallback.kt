package moe.him188.assembly.interpreter

import moe.him188.assembly.interpreter.descriptor.Call
import moe.him188.assembly.interpreter.descriptor.OperandConstAddress
import moe.him188.assembly.interpreter.descriptor.OperandLabel

interface InvocationCallback<out T : AssemblyInterpreter> {
    fun onExecute(interpreter: @UnsafeVariance T, call: Call)
    fun onMemoryWrite(interpreter: @UnsafeVariance T, call: Call, target: OperandConstAddress, value: Int)
    fun onMemoryWrite(interpreter: @UnsafeVariance T, call: Call, target: Register, value: Int)
    fun onInput(interpreter: @UnsafeVariance T, call: Call, value: Int)
    fun onOutput(interpreter: @UnsafeVariance T, call: Call, value: Int)
    fun onJump(interpreter: @UnsafeVariance T, call: Call, target: OperandLabel)

    companion object {
        inline fun <reified T : AssemblyInterpreter> noop(): InvocationCallback<T> {
            return object : InvocationCallback<T> {
                override fun onExecute(interpreter: T, call: Call) {
                    return
                }

                override fun onMemoryWrite(
                    interpreter: T, call: Call, target: OperandConstAddress, value: Int
                ) {
                    return
                }

                override fun onMemoryWrite(interpreter: T, call: Call, target: Register, value: Int) {
                    return
                }

                override fun onInput(interpreter: T, call: Call, value: Int) {
                    return
                }

                override fun onOutput(interpreter: T, call: Call, value: Int) {
                    return
                }

                override fun onJump(interpreter: T, call: Call, target: OperandLabel) {
                    return
                }
            }
        }
    }
}

class RecorderInvocationCallback<T : AssemblyInterpreter> : InvocationCallback<T> {
    sealed class Record {
        abstract val call: Call

        data class WriteAddress(override val call: Call, val target: OperandConstAddress, val value: Int) :
            Record()

        data class WriteRegister(override val call: Call, val target: Register, val value: Int) : Record()
        data class Input(override val call: Call, val value: Int) : Record()
        data class Output(override val call: Call, val value: Int) : Record()
        data class Jump(override val call: Call, val target: OperandLabel) : Record()
    }

    private val list = mutableListOf<Record>()

    fun toList(): List<Record> = list

    override fun onExecute(interpreter: T, call: Call) {
        return
    }

    override fun onMemoryWrite(interpreter: T, call: Call, target: OperandConstAddress, value: Int) {
        list.add(Record.WriteAddress(call, target, value))
    }

    override fun onMemoryWrite(interpreter: T, call: Call, target: Register, value: Int) {
        list.add(Record.WriteRegister(call, target, value))
    }

    override fun onInput(interpreter: T, call: Call, value: Int) {
        list.add(Record.Input(call, value))
    }

    override fun onOutput(interpreter: T, call: Call, value: Int) {
        list.add(Record.Output(call, value))
    }

    override fun onJump(interpreter: T, call: Call, target: OperandLabel) {
        list.add(Record.Jump(call, target))
    }
}

fun RecorderInvocationCallback<*>.dump(): String = buildString {
    appendLine("|\t\t|\t\t|\t\t|\t\t|")
    for (record in this@dump.toList()) {
        append(record)
        appendLine()
        continue
        when (record) {
            is RecorderInvocationCallback.Record.WriteAddress -> {
                // append()
            }
            is RecorderInvocationCallback.Record.WriteRegister -> TODO()
            is RecorderInvocationCallback.Record.Input -> TODO()
            is RecorderInvocationCallback.Record.Output -> TODO()
            is RecorderInvocationCallback.Record.Jump -> TODO()
        }
    }
}