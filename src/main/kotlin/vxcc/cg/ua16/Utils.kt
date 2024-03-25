package vxcc.cg.ua16

import vxcc.cg.Storage
import vxcc.cg.Value
import vxcc.utils.flatten

fun Value<UA16Env>.useInReg(env: UA16Env, block: (UA16Reg) -> Unit) {
    if (this is UA16Reg) {
        block(this)
        return
    }
    val o = env.forceAllocReg(env.flagsOf(this), env.firstFreeReg())
    val oSto = o.storage!!.flatten() as UA16Reg
    emitMov(env, oSto)
    block(oSto)
    env.dealloc(o)
}

fun Value<UA16Env>.useInRegOrSpecific(env: UA16Env, name: String, block: (UA16Reg) -> Unit) {
    if (this is UA16Reg) {
        block(this)
        return
    }
    val o = env.forceAllocReg(env.flagsOf(this), name)
    val oSto = o.storage!!.flatten() as UA16Reg
    emitMov(env, oSto)
    block(oSto)
    env.dealloc(o)
}

fun Storage<UA16Env>.useInRegWriteBack(env: UA16Env, copyInBegin: Boolean = true, block: (UA16Reg) -> Unit) {
    if (this is UA16Reg) {
        block(this)
        return
    }
    val o = env.forceAllocReg(env.flagsOf(this), env.firstFreeReg())
    val oSto = o.storage!!.flatten() as UA16Reg
    if (copyInBegin)
        emitMov(env, oSto)
    block(oSto)
    oSto.emitMov(env, this)
    env.dealloc(o)
}
