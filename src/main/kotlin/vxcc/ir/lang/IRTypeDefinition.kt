package vxcc.ir.lang

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
class IRTypeDefinition(
    val name: String,
    val size: Int,
    val align: Int,
    val float: Boolean,
    val signed: Boolean,
    val pointer: Boolean,
): IRElement {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRTypeDefinition(
            transform(name),
            size,
            align,
            float,
            signed,
            pointer
        )
}