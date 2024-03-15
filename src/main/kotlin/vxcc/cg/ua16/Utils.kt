package vxcc.cg.ua16

import vxcc.cg.Storage
import vxcc.cg.Value
import vxcc.cg.flatten

fun Value<UA16Env>.useInReg(env: UA16Env, block: (String) -> Unit) {
    if (this is UA16Reg) {
        block(this.name)
        return
    }
    val o = env.forceAllocReg(env.flagsOf(this), env.firstFreeReg())
    val oSto = o.storage!!.flatten() as UA16Reg
    emitMov(env, oSto)
    block(oSto.name)
    env.dealloc(o)
}

fun Storage<UA16Env>.useInRegWriteBack(env: UA16Env, copyInBegin: Boolean = true, block: (String) -> Unit) {
    if (this is UA16Reg) {
        block(this.name)
        return
    }
    val o = env.forceAllocReg(env.flagsOf(this), env.firstFreeReg())
    val oSto = o.storage!!.flatten() as UA16Reg
    if (copyInBegin)
        emitMov(env, oSto)
    block(oSto.name)
    oSto.emitMov(env, this)
    env.dealloc(o)
}