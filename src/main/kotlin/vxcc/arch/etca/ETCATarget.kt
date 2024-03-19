package vxcc.arch.etca

import vxcc.cg.AbstractTarget

class ETCATarget: AbstractTarget() {
    val stack by flag(false)
}