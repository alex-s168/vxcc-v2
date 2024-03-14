package vxcc.ir.opt

import vxcc.cg.Env
import vxcc.ir.lang.IRInstructionBlock

interface OptPass {
    fun <E: Env<E>> runOnGlobal(block: IRInstructionBlock, env: E?)
}