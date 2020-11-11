package moe.him188.assembly.interpreter.descriptor

import moe.him188.assembly.interpreter.Register
import moe.him188.assembly.interpreter.SyntaxErrorException


interface Element

data class Label(
    val name: String
) : Element


@Throws(SyntaxErrorException::class)
fun Operand(raw: String): Operand {
    return when {
        raw.startsWith('#') -> OperandConstInt(raw.substring(1))
        else -> {
            if (Register.values().any { it.name == raw }) return OperandRegisterName(raw)
            val address = operandConstAddressOrNull(raw)
            if (address != null) return address
            OperandLabel(raw)
        }
    }
}

interface Operand : Element

data class OperandRegisterName(
    val name: String
) : Operand

val OperandRegisterName.correspondingRegister: Register get() = Register.valueOf(name)

data class OperandLabel(
    val name: String
) : OperandConst<String> {
    override val value: String
        get() = name
}

interface OperandConst<T : Any> : Operand {
    val value: T
}

fun OperandConstInt(raw: String): OperandConstInt {
    return OperandConstInt(
        when {
            raw.matches(OperandConstInt.REGEX_BASE10) -> raw.toInt(10)
            raw.matches(OperandConstInt.REGEX_BASE16) -> raw.drop(1).toInt(16)
            raw.matches(OperandConstInt.REGEX_BASE2) -> raw.drop(1).toInt(2)
            else -> throw SyntaxErrorException("Unexpected const int value: #$raw")
        }
    )
}

fun operandConstAddressOrNull(raw: String): OperandConstAddress? {
    return OperandConstAddress(
        when {
            raw.matches(OperandConstInt.REGEX_BASE10) -> raw.toInt(10)
            raw.matches(OperandConstInt.REGEX_BASE16) -> raw.drop(1).toInt(16)
            raw.matches(OperandConstInt.REGEX_BASE2) -> raw.drop(1).toInt(2)
            else -> return null
        }
    )
}

/**
 * #0000 // decimal
 * #&0000 // binary
 * #&0000
 */
data class OperandConstInt(override val value: Int) : OperandConst<Int> {
    companion object {
        val REGEX_BASE16 = Regex("&[0-9a-fA-F]+")
        val REGEX_BASE10 = Regex("[0-9]+")
        val REGEX_BASE2 = Regex("B([01]+)")
    }
}

/**
 * #0000 // decimal
 * #&0000 // binary
 * #&0000
 */
data class OperandConstAddress(override val value: Int) : OperandConst<Int>