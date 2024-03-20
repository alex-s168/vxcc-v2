package vxcc.arch.etca

import vxcc.arch.AbstractTarget

class ETCATarget: AbstractTarget() {
    val stack by flag(false)

    override val subTargets: Map<String, List<String>> = mapOf(
        "" to listOf()
    )
}