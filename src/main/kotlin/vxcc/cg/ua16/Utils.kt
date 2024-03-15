package vxcc.cg.ua16

import vxcc.cg.Value
import vxcc.cg.flatten

fun Value<UA16Env>.useInReg(env: UA16Env, block: (String) -> Unit) {
    if (this is UA16Reg) {
        block(this.name)
        return
    }
    val o = env.forceAllocReg(env.flagsOf(this), env.firstFreeReg())
    block((o.storage!!.flatten() as UA16Reg).name)
    env.dealloc(o)
}