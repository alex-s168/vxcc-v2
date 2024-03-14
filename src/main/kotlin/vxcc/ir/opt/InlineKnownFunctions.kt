package vxcc.ir.opt

import vxcc.cg.Env
import vxcc.ir.lang.*

// TODO: forgot if you do:  var x = call()
class InlineKnownFunctions(
    /**
     * Functions <= this size will be inlined
     */
    val sizeTreshold: Int = 10
): OptPass {
    override fun <E: Env<E>> runOnGlobal(block: IRInstructionBlock, env: E?) {
        for (function in block.functions()) {
            function.body ?: continue

            val known_syms = function.body.knownSymbols()

            val iter = function.body.children.listIterator()
            while (iter.hasNext()) {
                val element = iter.next()
                if (element !is IRCall)
                    continue

                val target = element.staticFunction(function.body)
                    ?: continue

                if (target.noinline || target.body == null)
                    continue

                if (!(target.inline || target.calculateSizeCost(target.body) <= sizeTreshold))
                    continue

                val body = target.body.copyRenamed {
                    if (it in known_syms)
                        return@copyRenamed it

                    "inlined_${target.hashCode()}_$it"
                }

                iter.remove()
                target.args.forEachIndexed { i, it ->
                    iter.add(IRVariableDefinition(
                        name = it.name,
                        typ = it.typ
                    ))
                    it.name?.let {
                        iter.add(
                            IRAssignStmt(
                                left = IRVarRefExpr(
                                    name = it,
                                    reinterpret = null
                                ),
                                right = element.args.getOrNull(i)
                                    ?: throw IllegalStateException("not enough arguments")
                            )
                        )
                    }
                }
                iter.add(body)
            }
        }
    }
}