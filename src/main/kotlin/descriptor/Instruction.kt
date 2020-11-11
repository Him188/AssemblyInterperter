package moe.him188.assembly.interpreter.descriptor

enum class Instruction {
    LDM,
    LDD,
    LDI,
    LDX,
    LDR,
    STO,
    ADD,
    INC,
    DEC,
    JMP,
    CMP,
    JPE,
    JPN,
    IN,
    OUT,
    END,

    LABEL
}
