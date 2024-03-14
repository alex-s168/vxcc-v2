package vxcc.ir

import vxcc.ir.lang.IRInstructionBlock

fun IRInstructionBlock.updateParents() {
    for (child in children) {
        if (child is IRInstructionBlock) {
            child.parent = this
            child.updateParents()
        }
    }
}