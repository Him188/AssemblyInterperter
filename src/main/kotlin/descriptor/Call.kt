package moe.him188.assembly.interpreter.descriptor

data class Call(
    val label: Label?,
    val instruction: Instruction,
    val operand: Operand,
) : Element {
    companion object {
        val EMPTY_CALL = Call(null, Instruction.LABEL, Operand(""))
    }
}