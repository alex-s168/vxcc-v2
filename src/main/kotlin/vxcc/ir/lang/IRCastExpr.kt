package vxcc.ir.lang

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

/**
 * For reinterpret casts, see [IRVarRefExpr].
 */
@Polymorphic
@Serializable
class IRCastExpr(
    val value: IRElement,
    val typ: String?,
): IRElement {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRCastExpr(
            value.copyRenamed(transform),
            typ
        )
}