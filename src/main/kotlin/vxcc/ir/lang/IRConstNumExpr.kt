package vxcc.ir.lang

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
sealed interface IRConstNumExpr: IRElement

@Serializable
@Polymorphic
class IRConstIntExpr(
    val value: Long,
    val typ: String,
): IRConstNumExpr {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRConstIntExpr(
            value,
            transform(typ)
        )

}

@Serializable
@Polymorphic
class IRConstFloatExpr(
    val value: Double,
    val typ: String,
): IRConstNumExpr {
    override fun copyRenamed(transform: (String) -> String): IRElement =
        IRConstFloatExpr(
            value,
            transform(typ)
        )
}