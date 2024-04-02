package vxcc.arch

import kotlin.reflect.KProperty

abstract class AbstractTarget {
    var targetFlags = setOf<String>()

    protected class FlagProp(
        private var default: Boolean?,
        private val onSet: () -> Unit,
    ) {
        operator fun getValue(
            thisRef: Any?,
            property: KProperty<*>
        ): Boolean {
            val tg = thisRef as AbstractTarget

            default?.let {
                if (it) {
                    default = null
                    tg.targetFlags += property.name
                }
            }

            return property.name in tg.targetFlags
        }

        operator fun setValue(
            thisRef: Any?,
            property: KProperty<*>,
            value: Boolean
        ) {
            val tg = thisRef as AbstractTarget

            default = null

            if (value) {
                tg.targetFlags += property.name
                onSet()
            } else {
                tg.targetFlags -= property.name
            }
        }
    }

    protected fun flag(default: Boolean = false, onSet: () -> Unit = {}) =
        FlagProp(default, onSet)

    abstract val subTargets: Map<String, List<String>>

    fun loadSub(tg: String) {
        val st = subTargets[tg]
            ?: error("Invalid sub-target \"$tg\"! Available: ${subTargets.keys.joinToString { "\"$it\"" }}")
        targetFlags += st
    }

    override fun toString(): String =
        "${this::class.simpleName}(${targetFlags.joinToString()})"

    open fun compatible(other: AbstractTarget): Boolean {
        if (this::class != other::class)
            return false

        return targetFlags.all { it in other.targetFlags }
    }
}
