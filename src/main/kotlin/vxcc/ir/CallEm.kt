package vxcc.ir

import vxcc.cg.Env
import vxcc.cg.Owner
import vxcc.cg.flatten

// TODO: check if blocks even exist

internal fun <E: Env<E>> callEmitter(ctx: IrLocalScope<E>, env: E, call: IrCall<E>, dest: Owner<E>?) {
    when (call.fn) {
        "add" -> call.args[0].getA().emitAdd(env, call.args[1].getA(), dest!!.storage!!.flatten())
        "mul" -> call.args[0].getA().emitMul(env, call.args[1].getA(), dest!!.storage!!.flatten())
        "imul" -> call.args[0].getA().emitSignedMul(env, call.args[1].getA(), dest!!.storage!!.flatten())
        "imax" -> call.args[0].getA().emitSignedMax(env, call.args[1].getA(), dest!!.storage!!.flatten())
        "index" -> call.args[0].getA().emitArrayIndex(env, call.args[1].getA(), env.backToImm(call.args[2].getA()), dest!!.storage!!.flatten())
        "offset" -> call.args[0].getA().emitArrayOffset(env, call.args[1].getA(), env.backToImm(call.args[2].getA()), dest!!.storage!!.flatten())
        "ret" -> env.emitRet()
        "bra" -> env.emitJump(call.args[0].getB())
        "bre" -> env.emitJumpIfEq(call.args[1].getA(), call.args[2].getA(), call.args[0].getB())
        "brne" -> env.emitJumpIfNotEq(call.args[1].getA(), call.args[2].getA(), call.args[0].getB())
        "brl" -> env.emitJumpIfLess(call.args[1].getA(), call.args[2].getA(), call.args[0].getB())
        "brg" -> env.emitJumpIfGreater(call.args[1].getA(), call.args[2].getA(), call.args[0].getB())
        "bril" -> env.emitJumpIfSignedLess(call.args[1].getA(), call.args[2].getA(), call.args[0].getB())
        "brig" -> env.emitJumpIfSignedGreater(call.args[1].getA(), call.args[2].getA(), call.args[0].getB())
        "call" -> call.args[0].mapA { env.emitCall(it) }.mapB { env.emitCall(it) }
        else -> throw Exception("No builtin function ${call.fn}!")
    }
}