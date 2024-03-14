package vxcc.ir.lang

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
class IRFunctionDefinition(
    val name: String,

    val args: List<IRVariableDefinition>,
    val ret: String?,

    /**
     * Always inline this function
     */
    val inline: Boolean,
    val extern: Boolean,
    val noinline: Boolean,
    val noreturn: Boolean,
    val pure: Boolean,
    val export: Boolean,

    val abi: String?,
    val section: String?,
    val align: Int,

    val body: IRInstructionBlock?,
): IRElement {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRFunctionDefinition(
            transform(name),

            args.map { it.copyRenamed(transform) as IRVariableDefinition },
            ret,

            inline,
            extern,
            noinline,
            noreturn,
            pure,
            export,

            abi,
            section,
            align,

            body?.copyRenamed(transform) as IRInstructionBlock?
        )
}