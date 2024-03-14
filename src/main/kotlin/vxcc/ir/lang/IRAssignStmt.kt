package vxcc.ir.lang

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
@Polymorphic
class IRAssignStmt(
    val left: IRElement,
    val right: IRElement,
): IRElement {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRAssignStmt(
            left.copyRenamed(transform),
            right.copyRenamed(transform)
        )
}