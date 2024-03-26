package vxcc.cg

open class ABI(
    val argRegs: Collection<String>,
    val retRegs: Collection<String>,
    val clobRegs: Collection<String>,
)

class TypedABI(
    val typedArgRegs: Map<String, Owner.Flags>,
    val typedRetRegs: Map<String, Owner.Flags>,
    val typedClobRegs: Map<String, Owner.Flags>,
): ABI(
    typedArgRegs.keys,
    typedRetRegs.keys,
    typedClobRegs.keys
)