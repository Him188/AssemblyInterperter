@file:OptIn(ExperimentalContracts::class)

package moe.him188.assembly.interpreter

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
import moe.him188.assembly.interpreter.Accept.Companion.ACC_ALL
import moe.him188.assembly.interpreter.Accept.Companion.ACC_CONST
import moe.him188.assembly.interpreter.Accept.Companion.ACC_LABEL
import moe.him188.assembly.interpreter.Register.ACC
import moe.him188.assembly.interpreter.Register.IX
import moe.him188.assembly.interpreter.descriptor.*
import moe.him188.assembly.interpreter.descriptor.Instruction.*
import moe.him188.assembly.interpreter.util.EnumMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class Memory {
    private val memory: MutableMap<Int, Int> = mutableMapOf()

    fun getMemoryAt(address: Int): Int? = memory[address]
    fun setMemoryAt(address: Int, value: Int) {
        memory[address] = value
    }
}

enum class Register {
    ACC,
    IX // index
}

class ReplAssemblyInterpreter(
    inputFlow: ReceiveChannel<Int>,
    outputChannel: SendChannel<Int>,
    invocationCallback: InvocationCallback<ReplAssemblyInterpreter> = InvocationCallback.noop(),
) : AssemblyInterpreter(inputFlow, outputChannel, invocationCallback) {
    private val _calls = mutableListOf<Call>()
    override val calls: List<Call> get() = _calls
    public override suspend fun execute(call: Call): Boolean {
        return super.execute(call).also { _calls.add(call) }
    }
}

class InvokeAllAssemblyInterpreter(
    override val calls: List<Call>,
    inputFlow: ReceiveChannel<Int>,
    outputChannel: SendChannel<Int>,
    invocationCallback: InvocationCallback<InvokeAllAssemblyInterpreter> = InvocationCallback.noop(),
) : AssemblyInterpreter(inputFlow, outputChannel, invocationCallback) {
    suspend fun execute() {
        calls.forEachIndexed { index, call ->
            if (!super.executeAtLine(call, index)) {
                return
            }
        }
    }
}

private inline class Accept(
    val value: Int
) {
    operator fun plus(accept: Accept): Accept = Accept(this.value or accept.value)

    companion object {
        val ACC_CONST = Accept(0b1)
        val ACC_REGISTER = Accept(0b10)
        val ACC_LABEL = Accept(0b100)
        val ACC_ADDRESS = Accept(0b1000)

        val ACC_ALL = ACC_CONST + ACC_REGISTER + ACC_LABEL + ACC_ADDRESS
    }
}

abstract class AssemblyInterpreter(
    private val inputChannel: ReceiveChannel<Int>,
    private val outputChannel: SendChannel<Int>,
    private val invocationCallback: InvocationCallback<AssemblyInterpreter> = InvocationCallback.noop(),
) {
    private val memory: Memory = Memory()
    private val register: MutableMap<Register, Int> = EnumMap<Register, Int>(Register::class).apply {
        put(IX, 0)
    }

    private inline fun <reified T : Operand> Operand.assertedCast(): T {
        contract { returns() implies (this@assertedCast is T) }
        if (this !is T) throw InapplicableOperandException("Inapplicable operand: expected ${T::class.simpleName}, given ${this::class.simpleName}")
        return this
    }

    private fun Int.writeMemory(value: Int) {
        memory.setMemoryAt(this, value)
    }

    private var Int.inMemory: Int
        get() = memory.getMemoryAt(this)
            ?: throw EmptyMemoryException("Memory location at $this is currently empty so can't be accessed.")
        set(value) {
            writeMemory(value)
        }

    private var OperandConstInt.inMemory: Int
        get() = this.value.inMemory
        set(value) {
            this.value.inMemory = value
        }

    private var OperandConstAddress.inMemory: Int
        get() = this.value.inMemory
        set(value) {
            this.value.inMemory = value
        }

    private fun Int.getMemory(): Int? = memory.getMemoryAt(this)

    private var Register.value: Int
        get() = register[this]
            ?: throw EmptyRegisterException("Register ${this.name} is currently empty so can't be accessed.")
        set(value) {
            register[this] = value
        }

    private val Call.operandAsConstInt get() = operand.assertedCast<OperandConstInt>()
    private val Call.operandAsConstAddress get() = operand.assertedCast<OperandConstAddress>()
    private val Call.operandAsRegister get() = operand.assertedCast<OperandRegisterName>().correspondingRegister

    private fun Operand.resolveConstInt() = resolveIntValue(ACC_CONST + ACC_LABEL)
    private fun Operand.resolveIntValue(accept: Accept): Int {
        return this.fold(
            onConstInt = { it.value },
            onAddress = { it.inMemory },
            onRegister = { it.value },
            onLabel = { it.resolveConstReference().resolveIntValue(accept) }
        )
    }

    private tailrec fun OperandLabel.resolveConstReference(): OperandConst<*> {
        val labelCall = calls.getByLabel(name)

        if (labelCall.instruction == LABEL) {
            if (labelCall.operand is OperandLabel) {
                return labelCall.operand.resolveConstReference()
            }
            return labelCall.operand.assertedCast()
        } else {
            throw InapplicableLabelReferenceException("Expected const reference but found label $labelCall referencing an ${labelCall.instruction} instruction.")
        }
    }

    private fun Operand.resolveAddress(): OperandConstAddress {
        fun throwInapplicable(): Nothing =
            throw InapplicableOperandException("Inapplicable operand: expected ${OperandConstAddress::class.simpleName} or ${OperandLabel::class.simpleName}, given ${this::class.simpleName}")

        return this.fold(
            onConstInt = { throwInapplicable() },
            onAddress = { it },
            onRegister = { throwInapplicable() },
            onLabel = { it.resolveConstReference().resolveAddress() }
        )
    }

    private fun Operand.resolveTargetInstructionIndex(): Int {
        contract { returns() implies (this@resolveTargetInstructionIndex is OperandLabel) }
        val label = this.assertedCast<OperandLabel>().name
        calls.forEachIndexed { index, call ->
            if (call.label?.name == label) {
                return index
            }
        }
        throw UnresolvedLabelException("Unresolved label: $label")
    }

    private inline fun <R> Operand.fold(
        onConstInt: (const: OperandConstInt) -> R,
        onAddress: (address: OperandConstAddress) -> R,
        onRegister: (register: Register) -> R,
        onLabel: (label: OperandLabel) -> R,
    ): R {
        contract {
            callsInPlace(onConstInt, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onAddress, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onRegister, InvocationKind.AT_MOST_ONCE)
            callsInPlace(onLabel, InvocationKind.AT_MOST_ONCE)
        }
        return when (this) {
            is OperandConstAddress -> onAddress(this)
            is OperandConstInt -> onConstInt(this)
            is OperandRegisterName -> onRegister(this.correspondingRegister)
            is OperandLabel -> onLabel(this)
            else -> null!!
        }
    }

    protected abstract val calls: List<Call>

    protected open suspend fun executeAtLine(call: Call, line: Int): Boolean {
        return kotlin.runCatching { execute(call) }.getOrElse { e ->
            throw ExecutionErrorException("Execution failed at line $line", e)
        }
    }

    private var lastCompareResult: Boolean? = null


    private suspend inline fun gotoInvoke(index: Int) = execute(calls.getCallAtIndex(index))

    /**
     * Boolean: continue invoke next line
     */
    @OptIn(FlowPreview::class)
    protected open suspend fun execute(call: Call): Boolean {
        if (call == Call.EMPTY_CALL) return true
        invocationCallback.onExecute(this, call)
        when (call.instruction) {
            LABEL -> return true // no-op
            LDM -> ACC.value = call.operand.resolveIntValue(ACC_CONST + ACC_LABEL)
            LDD -> ACC.value = call.operand.resolveAddress().inMemory
            LDI -> ACC.value = call.operand.resolveAddress().inMemory.inMemory
            LDX -> ACC.value = (call.operand.resolveAddress().value + IX.value).inMemory
            LDR -> IX.value = call.operand.resolveIntValue(ACC_CONST + ACC_LABEL)
            STO -> {
                call.operandAsConstAddress.value.writeMemory(ACC.value)
                invocationCallback.onMemoryWrite(this, call, call.operandAsConstAddress, ACC.value)
            }
            ADD -> {
                ACC.value += call.operand.resolveIntValue(ACC_ALL)
                invocationCallback.onMemoryWrite(this, call, ACC, ACC.value + call.operand.resolveIntValue(ACC_ALL))
            }
            INC -> {
                call.operandAsRegister.value++
                invocationCallback.onMemoryWrite(this, call, call.operandAsRegister, call.operandAsRegister.value + 1)
            }
            DEC -> {
                call.operandAsRegister.value--
                invocationCallback.onMemoryWrite(this, call, call.operandAsRegister, call.operandAsRegister.value - 1)
            }
            JMP -> {
                gotoInvoke(call.operand.resolveTargetInstructionIndex().also {
                    invocationCallback.onJump(this, call, call.operand.assertedCast())
                })
                return false
            }
            CMP -> lastCompareResult = call.operand.resolveIntValue(ACC_CONST + ACC_LABEL) == ACC.value
            JPE,
            JPN -> {
                val lastCompareResult = lastCompareResult
                    ?: throw StandaloneCompareJumpInstructionException("Found no previous CMP instruction before an ${call.instruction.name}")
                this.lastCompareResult = null
                if (call.instruction == JPE == lastCompareResult) { // (JPE && lastCompareResult) || (JPN && !lastCompareResult)
                    gotoInvoke(call.operand.resolveTargetInstructionIndex().also {
                        invocationCallback.onJump(this, call, call.operand.assertedCast())
                    })
                    return false
                }
            }
            IN -> {
                ACC.value = inputChannel.receive()
                invocationCallback.onInput(this, call, ACC.value)
            }
            OUT -> {
                outputChannel.send(ACC.value)
                invocationCallback.onOutput(this, call, ACC.value)
            }
            END -> return false
        }
        return true
    }
}

private fun List<Call>.getCallAtIndex(index: Int): Call =
    this.asSequence().filterIndexed { i, _ -> i == index }.firstOrNull()
        ?: throw UnresolvedCallException("at index $index")

private fun List<Call>.findByLabel(label: String) = this.find { it.label?.name == label }
private fun List<Call>.getByLabel(label: String) =
    findByLabel(label) ?: throw UnresolvedLabelException("Unresolved label: $label")


class InapplicableOperandException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

class EmptyRegisterException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

class EmptyMemoryException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

class UnresolvedLabelException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

class UnresolvedCallException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

class StandaloneCompareJumpInstructionException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

class InapplicableLabelReferenceException(override val message: String?, override val cause: Throwable? = null) :
    ExecutionErrorException(message, cause)

open class ExecutionErrorException(override val message: String?, override val cause: Throwable? = null) :
    AssemblyInterpreterException(message, cause)

class SyntaxErrorException(override val message: String?, override val cause: Throwable? = null) :
    AssemblyInterpreterException(message, cause)

open class AssemblyInterpreterException(override val message: String?, override val cause: Throwable? = null) :
    Exception()