package vxcc.ir.lang

import kotlinx.serialization.Serializable

/**
 * All IR elements are immutable except statements.
 */
@Serializable
sealed interface IRElement {
    fun copyRenamed(transform: (String) -> String): IRElement
}