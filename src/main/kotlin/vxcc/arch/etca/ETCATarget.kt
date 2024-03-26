package vxcc.arch.etca

import vxcc.arch.AbstractTarget

class ETCATarget: AbstractTarget() {
    var stack by flag()
    var int by flag()
    var eop by flag()
    var bm1 by flag {
        eop = true
    }

    override val subTargets: Map<String, List<String>> = mapOf(
        "" to listOf()
    )
}