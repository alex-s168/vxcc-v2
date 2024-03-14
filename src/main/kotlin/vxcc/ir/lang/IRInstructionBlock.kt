package vxcc.ir.lang

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class IRInstructionBlock(
    /**
     * Is this the root block?
     */
    val global: Boolean,

    val children: MutableList<IRElement>
): IRElement {
    @Transient
    var parent: IRInstructionBlock? = null

    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRInstructionBlock(
            global,
            children.map { it.copyRenamed(transform) }.toMutableList()
        )
}