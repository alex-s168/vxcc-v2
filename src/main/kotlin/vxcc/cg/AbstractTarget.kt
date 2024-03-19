package vxcc.cg

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

    protected fun flag(default: Boolean, onSet: () -> Unit = {}) =
        FlagProp(default, onSet)
}
