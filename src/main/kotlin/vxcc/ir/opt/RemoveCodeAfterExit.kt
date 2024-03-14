package vxcc.ir.opt

import vxcc.cg.Env
import vxcc.ir.lang.IRInstructionBlock
import vxcc.ir.lang.functions
import vxcc.ir.lang.willStopFunctionFlow

// TODO: forgot if you do:  var x = call()
class RemoveCodeAfterExit: OptPass {
    override fun <E: Env<E>> runOnGlobal(block: IRInstructionBlock, env: E?) {
        for (function in block.functions()) {
            var remove = false
            function.body?.children?.listIterator()?.let { iter ->
                while (iter.hasNext()) {
                    val element = iter.next()
                    if (remove) {
                        iter.remove()
                    }
                    else if (element.willStopFunctionFlow(function.body)) {
                        remove = true
                    }
                }
            }
        }
    }
}