package vxcc.ir.lang

fun IRInstructionBlock.withAllParents(): Sequence<IRInstructionBlock> {
    var current = this
    return generateSequence(this) {
        val parent = current.parent
        if (parent != null) {
            current = parent
            current
        } else {
            null
        }
    }
}

fun IRInstructionBlock.functions(): Sequence<IRFunctionDefinition> =
    this.withAllParents()
        .flatMap { it.children }
        .filterIsInstance<IRFunctionDefinition>()

fun IRInstructionBlock.variables(): Sequence<IRVariableDefinition> =
    this.withAllParents()
        .flatMap { it.children + it }
        .filterIsInstance<IRVariableDefinition>() +
    this.functions()
        .flatMap { it.args }

fun IRInstructionBlock.types(): Sequence<IRTypeDefinition> =
    this.withAllParents()
        .flatMap { it.children }
        .filterIsInstance<IRTypeDefinition>()

fun IRCall.staticFunction(parent: IRInstructionBlock): IRFunctionDefinition? =
    (func as? IRVarRefExpr)?.let { f ->
        parent.functions().find { it.name == f.name }
    }

fun IRElement.willStopFunctionFlow(parent: IRInstructionBlock): Boolean =
    when (this) {
        is IRCall -> this.staticFunction(parent)?.noreturn == true
        is IRReturnStmt -> true
        is IRInstructionBlock -> this.children.any { it.willStopFunctionFlow(this) }
        else -> false
    }

// TODO: this is actually target-dependent but for now we'll just assume 5 bytes for any instruction
fun IRElement.calculateSizeCost(parent: IRInstructionBlock): Int =
    when (this) {
        is IRTypeDefinition -> size
        is IRVariableDefinition -> parent.types().find { it.name == typ }?.size ?: 0
        is IRFunctionDefinition -> body?.let { it.calculateSizeCost(it) } ?: 0
        is IRInstructionBlock -> children.sumOf { it.calculateSizeCost(this) }
        else -> 5
    }

fun IRInstructionBlock.knownSymbols(
    dest: MutableList<String> = mutableListOf()
): MutableList<String> {
    dest += this.functions().map { it.name }
    dest += this.variables().mapNotNull { it.name }
    dest += this.types().map { it.name }
    return dest
}