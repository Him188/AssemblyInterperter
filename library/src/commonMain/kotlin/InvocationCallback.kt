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

fun AssemblyInterpreter.dump(callback: RecorderInvocationCallback<*>): String = buildString {
    val map = LinkedHashMap<String, Int>()
    map["IX"] = 0
    map["ACC"] = 0
    val records = callback.toList()

    // analyze and init map
    for (record in records) {
        when (record) {
            is RecorderInvocationCallback.Record.WriteAddress -> {
                map[record.target.value.toString()] = 0
            }
            is RecorderInvocationCallback.Record.WriteRegister -> {
                map[record.target.name] = 0
            }
            else -> {
            }
        }
    }
    map["OUT"] = 0

    val keysList = map.keys.toList() // indexed

    appendLine("+" + "-".repeat(30) + "+")
    appendLine(keysList.joinToString("|", prefix = "|", postfix = "|") { "\t$it\t" }) // header
    appendLine("+" + "-".repeat(30) + "+")

    fun appendChanged(
        keyName: String,
        newValue: Int
    ) {
        val index = keysList.indexOf(keyName)
        val before = keysList.slice(0 until index)
        val after = keysList.slice((index + 1)..keysList.lastIndex)
        append("|")
        before.forEach { _ -> append("\t\t").append("|") }
        append("\t$newValue\t")
        append("|")
        after.forEach { _ -> append("\t\t").append("|") }
        appendLine()
    }

    for (record in records) {
        when (record) {
            is RecorderInvocationCallback.Record.WriteAddress -> {
                appendChanged(record.target.value.toString(), record.value)
            }
            is RecorderInvocationCallback.Record.WriteRegister -> {
                appendChanged(record.target.name, record.value)
            }
            is RecorderInvocationCallback.Record.Input -> {
                appendChanged("ACC", record.value)
            }
            is RecorderInvocationCallback.Record.Output -> {
                appendChanged("OUT", record.value)
            }
            is RecorderInvocationCallback.Record.Jump -> {
                appendLine("| \t\t JUMP TO ${record.target.name} \t\t |")
            }
        }
    }

    appendLine("+" + "-".repeat(30) + "+")
}