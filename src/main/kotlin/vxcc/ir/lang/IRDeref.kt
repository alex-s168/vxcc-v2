package vxcc.ir.lang

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Polymorphic
@Serializable
class IRDeref(
    val expr: IRElement,
    val resultType: String,

    val volatile: Boolean,
): IRElement {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRDeref(
            expr.copyRenamed(transform),
            resultType,

            volatile
        )
}