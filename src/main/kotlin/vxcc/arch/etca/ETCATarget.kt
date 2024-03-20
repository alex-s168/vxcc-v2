package vxcc.arch.etca

import vxcc.arch.AbstractTarget

class ETCATarget: AbstractTarget() {
    val stack by flag(false)
}